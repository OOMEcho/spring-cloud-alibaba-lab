package cn.oomecho.cloud.gateway;

import cn.oomecho.cloud.common.core.runtime.SdkRuntimeDirectories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;

/**
 * 响应式网关启动入口；通过服务发现统一访问业务服务。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@SpringBootApplication
public class GatewayApplication {
    /**
     * 在 SDK 初始化前设置运行目录，再启动响应式网关。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SdkRuntimeDirectories.configure("cloud-gateway", new ApplicationHome(GatewayApplication.class).getDir().toPath());
        SpringApplication.run(GatewayApplication.class, args);
    }
}
