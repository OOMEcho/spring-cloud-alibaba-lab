package cn.oomecho.cloud.order.integration.feign;

import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.order.domain.vo.ProductDisplayVO.DegradeReason;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import feign.FeignException;
import feign.codec.DecodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * 商品展示的消费者回退策略：故障可以省略展示信息，业务拒绝和无效响应不能伪装成成功。
 * FallbackFactory 能取得原始异常；普通 fallback 不能直接获知触发原因。
 *
 * @author xuesong.lei
 * @since 2026-10-14
 */
@Component
public class ProductDisplayFallbackFactory implements FallbackFactory<ProductDisplayClient> {
    private static final Logger log = LoggerFactory.getLogger(ProductDisplayFallbackFactory.class);

    /**
     * 按本次失败创建回退实例，不保存请求间共享的故障状态。
     *
     * @param cause 本次 Feign 调用或 Sentinel 拦截的异常
     * @return 只处理当前失败的展示客户端
     */
    @Override
    public ProductDisplayClient create(Throwable cause) {
        return id -> {
            if (cause instanceof DecodeException) {
                throw new BusinessException(502, 50210, "商品服务返回了无效展示数据");
            }
            if (cause instanceof FeignException feign && feign.status() >= 400
                    && feign.status() < 500 && feign.status() != 429) {
                // 商品不存在、参数错误、无权限等仍保留业务错误，不能返回“暂无信息”。
                throw new BusinessException(feign.status(), feign.status() == 404 ? 40402 : feign.status() * 100 + 10,
                        feign.status() == 404 ? "商品不存在" : "商品服务拒绝了展示请求");
            }
            DegradeReason reason;
            if (cause instanceof DegradeException) {
                reason = DegradeReason.CIRCUIT_OPEN;
            } else if (cause instanceof BlockException
                    || cause instanceof FeignException feign && feign.status() == 429) {
                reason = DegradeReason.RATE_LIMITED;
            } else if (isTimeout(cause)) {
                reason = DegradeReason.TIMEOUT;
            } else if (cause instanceof FeignException feign && (feign.status() < 0 || feign.status() >= 500)) {
                reason = DegradeReason.UNAVAILABLE;
            } else {
                // 未预期的代码异常不采用展示降级，保留错误供排查。
                throw new BusinessException(500, 50010, "商品展示调用发生未预期错误");
            }
            log.warn("商品展示触发 Feign 回退，productId={}，reason={}，type={}",
                    id, reason, cause.getClass().getSimpleName());
            throw new ProductDisplayUnavailableException(reason, cause);
        };
    }

    /**
     * Feign 将网络异常包装在 RetryableException 中，沿原因链识别超时。
     *
     * @param cause 原始调用异常
     * @return 是否包含超时原因
     */
    private boolean isTimeout(Throwable cause) {
        Throwable current = cause;
        for (int depth = 0; current != null && depth < 10; depth++, current = current.getCause()) {
            if (current instanceof SocketTimeoutException || current instanceof TimeoutException) {
                return true;
            }
        }
        return false;
    }
}
