package cn.oomecho.cloud.notification;

import cn.oomecho.cloud.common.core.runtime.SdkRuntimeDirectories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;

/**
 * 订单站内通知服务启动入口；消息消费与订单服务独立部署
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@SpringBootApplication
public class NotificationApplication {
    /**
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SdkRuntimeDirectories.configure("cloud-notification", new ApplicationHome(NotificationApplication.class).getDir().toPath());
        SpringApplication.run(NotificationApplication.class, args);
    }
}
