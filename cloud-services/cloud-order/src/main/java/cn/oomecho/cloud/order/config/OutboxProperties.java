package cn.oomecho.cloud.order.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 发件箱有限重试参数；租约必须覆盖一次连接和确认等待
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Validated
@ConfigurationProperties("lab.outbox")
public record OutboxProperties(
        @DefaultValue("8") @Min(1) @Max(100) int maxAttempts,
        @DefaultValue("20") @Min(1) @Max(100) int batchSize,
        @DefaultValue("5") @Min(1) @Max(30) int confirmTimeoutSeconds,
        @DefaultValue("30") @Min(10) @Max(300) int leaseSeconds,
        @DefaultValue("2") @Min(1) @Max(60) int retryBaseSeconds) {
    /**
     * @return 租约覆盖确认等待和连接等待的安全余量
     */
    @AssertTrue(message = "leaseSeconds 必须至少比 confirmTimeoutSeconds 大 5 秒")
    public boolean isLeaseSafe() {
        return leaseSeconds >= confirmTimeoutSeconds + 5;
    }
}
