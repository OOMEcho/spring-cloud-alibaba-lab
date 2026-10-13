package cn.oomecho.cloud.order.integration;


import cn.oomecho.cloud.api.user.client.UserClient;
import cn.oomecho.cloud.api.user.dto.UserResponse;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.order.config.SentinelResources;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import feign.FeignException;
import feign.codec.DecodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务调用适配层；隔离远程错误与订单业务
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Component
public class UserGateway {
    private static final Logger log = LoggerFactory.getLogger(UserGateway.class);
    private final UserClient client;

    /**
     * @param client 用户服务远程调用契约
     */
    public UserGateway(UserClient client) {
        this.client = client;
    }

    /**
     * 校验远程响应，区分业务不存在与服务故障；不伪造成功数据。
     *
     * @param userId 用户 ID
     * @return 与请求 ID 匹配的用户信息
     */
    public UserResponse detail(Long userId) {
        Entry entry = null;
        try {
            entry = SphU.entry(SentinelResources.USER_QUERY, EntryType.OUT);
            return queryUser(userId);
        } catch (DegradeException exception) {
            throw new BusinessException(503, 50302, "用户服务已熔断，请稍后重试");
        } catch (BlockException exception) {
            throw new BusinessException(429, 42902, "用户查询请求受限，请稍后重试");
        } catch (BusinessException exception) {
            // 用户不存在等 4xx 是业务结果，只有 5xx 才计入远程服务异常。
            if (exception.getStatus() >= 500) {
                Tracer.traceEntry(exception, entry);
            }
            throw exception;
        } catch (RuntimeException exception) {
            Tracer.traceEntry(exception, entry);
            throw exception;
        } finally {
            // 必须先记录异常再退出，熔断器在 entry.exit 时读取本次调用结果。
            if (entry != null) {
                entry.exit();
            }
        }
    }

    /**
     * 发起 Feign 请求并校验契约，由外层 Sentinel 资源统计实际调用结果。
     *
     * @param userId 用户 ID
     * @return 与请求 ID 一致的用户信息
     */
    private UserResponse queryUser(Long userId) {
        Result<UserResponse> response;
        try {
            response = client.detail(userId);
        } catch (FeignException.NotFound exception) {
            throw new BusinessException(404, 40401, "用户不存在");
        } catch (DecodeException exception) {
            log.warn("用户服务响应解码失败，userId={}", userId);
            throw new BusinessException(502, 50201, "用户服务返回了无效数据");
        } catch (FeignException exception) {
            log.warn("用户服务调用失败，userId={}，remoteStatus={}", userId, exception.status());
            throw new BusinessException(503, 50301, "用户服务暂不可用，请稍后重试");
        }
        if (response == null || response.code() != 200 || response.data() == null
                || !userId.equals(response.data().id())) {
            throw new BusinessException(502, 50201, "用户服务返回了无效数据");
        }
        return response.data();
    }
}
