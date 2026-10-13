package cn.oomecho.cloud.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 从 Nacos 加载业务路由；校验整份配置后替换不可变快照，并刷新 Gateway 路由缓存。
 * 首次配置无效时拒绝启动；运行中无效更新保留旧路由，不向请求线程暴露半份配置。
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Component
public class NacosBusinessRouteLocator implements RouteLocator, InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(NacosBusinessRouteLocator.class);
    private static final String DATA_ID = "cloud-gateway-routes.json";
    /** 共享 Nacos 配置客户端管理器。 */
    private final NacosConfigManager manager;
    /** 内存路由构建器，用于先构建候选快照。 */
    private final RouteLocatorBuilder builder;
    /** 通知 Gateway 刷新实际匹配缓存。 */
    private final ApplicationEventPublisher events;
    /** 拒绝未知字段和重复 JSON 属性的独立映射器。 */
    private final ObjectMapper json;
    /** 与业务配置一致的 Nacos Group。 */
    private final String group;
    /** 将配置回调串行化，且不占用响应式请求线程。 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "nacos-business-routes");
        thread.setDaemon(true);
        return thread;
    });
    /** 当前请求可读取的完整快照；发布后不再修改其中的路由列表。 */
    private volatile List<Route> snapshot = List.of();
    /** 由管理器提供，不由本类关闭的共享配置服务。 */
    private ConfigService service;
    /** 仅监听本类路由 Data ID 的回调。 */
    private Listener listener;
    /** 初始快照是否已经安装。 */
    private boolean initialized;
    /** 关闭后忽略已经排队的配置回调。 */
    private boolean closed;
    /** 初始化期间到达的最新回调，在初始配置之后处理，避免旧配置覆盖新推送。 */
    private String pendingUpdate;

    /**
     * 复用 Nacos 配置客户端，其地址、命名空间和鉴权沿用 application-nacos.yml。
     *
     * @param manager Nacos 配置客户端管理器
     * @param builder Gateway 路由构建器
     * @param events 路由缓存刷新事件发布器
     * @param json JSON 映射器
     * @param group 业务配置 Group
     */
    public NacosBusinessRouteLocator(NacosConfigManager manager, RouteLocatorBuilder builder,
                                     ApplicationEventPublisher events, ObjectMapper json,
                                     @Value("${NACOS_GROUP:LAB_GROUP}") String group) {
        this.manager = manager;
        this.builder = builder;
        this.events = events;
        this.group = group;
        this.json = json.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    /**
     * 先取得配置并注册监听，再安装初始快照；连接失败、缺失或无效配置均阻止启动。
     *
     * @throws Exception Nacos 读取或初始配置校验失败
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        service = manager.getConfigService();
        listener = new Listener() {
            @Override
            public Executor getExecutor() {
                return executor;
            }

            @Override
            public void receiveConfigInfo(String content) {
                onUpdate(content);
            }
        };
        try {
            String content = service.getConfigAndSignListener(DATA_ID, group, 5000, listener);
            synchronized (this) {
                snapshot = buildSnapshot(content);
                initialized = true;
                log.info("业务路由初始加载成功，dataId={}，routeCount={}", DATA_ID, snapshot.size());
                if (pendingUpdate != null) {
                    onUpdate(pendingUpdate);
                    pendingUpdate = null;
                }
            }
        } catch (Exception failure) {
            destroy();
            // 不把远程配置内容或底层连接信息作为公开异常消息。
            throw new IllegalStateException("Nacos 业务路由初始化失败，请检查 Data ID、Group 和配置格式");
        }
    }

    /**
     * 在订阅时读取完整快照，不在请求线程访问 Nacos 或修改路由。
     *
     * @return 当前业务路由
     */
    @Override
    public Flux<Route> getRoutes() {
        return Flux.defer(() -> Flux.fromIterable(snapshot));
    }

    /**
     * 单线程监听配合互斥锁串行安装配置；异常不影响当前生效快照。
     *
     * @param content Nacos 推送的完整 JSON 数组
     */
    private synchronized void onUpdate(String content) {
        if (closed) return;
        if (!initialized) {
            pendingUpdate = content;
            return;
        }
        List<Route> previous = snapshot;
        try {
            List<Route> next = buildSnapshot(content);
            snapshot = next;
            events.publishEvent(new RefreshRoutesEvent(this));
            log.info("业务路由更新成功，dataId={}，routeCount={}", DATA_ID, next.size());
        } catch (Exception failure) {
            snapshot = previous;
            // 不记录配置全文；配置可能包含误填的凭据或其他敏感内容。
            log.warn("业务路由更新被拒绝，保留旧路由，dataId={}，errorType={}", DATA_ID,
                    failure.getClass().getSimpleName());
        }
    }

    /**
     * 校验全部定义并构建不可变列表；数组顺序即匹配优先级，空数组表示关闭所有业务路由。
     *
     * @param content 仅允许 id、path、uri 三个字段的 JSON 数组
     * @return 已完成构建的路由快照
     * @throws Exception JSON、路径、服务名或路由构建无效
     */
    private List<Route> buildSnapshot(String content) throws Exception {
        if (content == null || content.isBlank() || content.length() > 65536)
            throw new IllegalArgumentException("路由配置缺失或过大");
        List<BusinessRoute> definitions = json.readValue(content, new TypeReference<>() { });
        if (definitions == null || definitions.size() > 50)
            throw new IllegalArgumentException("路由数组无效或超过上限");
        HashSet<String> ids = new HashSet<>();
        RouteLocatorBuilder.Builder routes = builder.routes();
        for (int index = 0; index < definitions.size(); index++) {
            BusinessRoute route = definitions.get(index);
            validate(route, ids);
            int order = index;
            routes.route(route.id(), spec -> spec.order(order).path(route.path())
                    .filters(filters -> filters.stripPrefix(1)).uri(route.uri()));
        }
        // 构建源仅包含内存定义，无网络调用；完成全部构建后才允许替换当前快照。
        List<Route> built = routes.build().getRoutes().collectList().block(Duration.ofSeconds(2));
        if (built == null) throw new IllegalStateException("路由构建未完成");
        return List.copyOf(built);
    }

    /**
     * 限制为现有 /api 业务路径和 lb 服务名，避免配置管理接口或任意外部 URL。
     *
     * @param route 一条路由定义
     * @param ids 本次配置已出现的 ID
     */
    private void validate(BusinessRoute route, HashSet<String> ids) {
        if (route == null || route.id() == null || !route.id().matches("[a-z][a-z0-9-]{0,63}")
                || !ids.add(route.id())) throw new IllegalArgumentException("路由 ID 无效或重复");
        if (route.path() == null || !route.path().matches(
                "/api/(users|orders|products|stocks|notifications)(/[A-Za-z0-9_{}*?/-]*)?"))
            throw new IllegalArgumentException("路由只能匹配公开业务路径");
        new PathPatternParser().parse(route.path());
        if (route.uri() == null || !route.uri().matches("lb://cloud-[a-z0-9]+(?:-[a-z0-9]+)*"))
            throw new IllegalArgumentException("路由上游必须是 cloud 服务名");
        URI.create(route.uri());
    }

    /** 关闭本类的监听和线程，不关闭其他配置监听共享的 Nacos 客户端。 */
    @Override
    public synchronized void destroy() {
        closed = true;
        if (service != null && listener != null) service.removeListener(DATA_ID, group, listener);
        executor.shutdownNow();
    }

    /**
     * 业务路由配置；所有路由统一移除首段 /api，不接受任意过滤器或管理路径。
     *
     * @param id 路由唯一 ID
     * @param path 公开业务路径模式
     * @param uri lb 服务名地址
     * @author xuesong.lei
     * @since 2026-10-13
     */
    private record BusinessRoute(String id, String path, String uri) { }
}
