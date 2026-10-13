package cn.oomecho.cloud.order.config;

import cn.oomecho.cloud.order.service.OutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 发件箱定时扫描；后台故障记录后继续下一周期，不影响正式下单入口
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = {"lab.messaging.enabled", "lab.outbox.scheduler-enabled"}, havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class OutboxScheduler {
    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);
    private final OutboxPublisher publisher;

    /**
     * @param publisher 发布批次服务
     */
    public OutboxScheduler(OutboxPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * 每个实例运行；租约 SQL 保护并发，单次异常不会停止后续调度。
     */
    @Scheduled(fixedDelayString = "${lab.outbox.poll-delay-ms:1000}", initialDelayString = "${lab.outbox.initial-delay-ms:3000}")
    public void tick() {
        try {
            publisher.publishBatch();
        } catch (RuntimeException e) {
            log.error("事件扫描失败，下一周期继续，errorType={}", e.getClass().getSimpleName());
        }
    }
}
