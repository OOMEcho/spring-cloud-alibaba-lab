package cn.oomecho.cloud.order.config;

/**
 * Sentinel 业务资源名称；与 Dashboard 规则中的资源名保持一致。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public final class SentinelResources {
    /**
     * 订单预览入口：配置 QPS 或并发限流。
     */
    public static final String ORDER_PREVIEW = "order:preview";
    /**
     * 用户远程调用：配置服务异常熔断，业务不存在不计入异常。
     */
    public static final String USER_QUERY = "user:query";

    /**
     * 常量类不允许创建实例。
     */
    private SentinelResources() {
    }
}
