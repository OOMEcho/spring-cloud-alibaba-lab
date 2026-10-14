package cn.oomecho.cloud.user;

import cn.oomecho.cloud.common.core.runtime.SdkRuntimeDirectories;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;

/**
 * 用户服务启动入口
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@SpringBootApplication
@MapperScan("cn.oomecho.cloud.user.mapper")
public class UserApplication {
    /**
     * 启动用户服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        // Nacos 在配置导入阶段就会初始化，必须在 Spring 启动前设置路径。
        SdkRuntimeDirectories.configure("cloud-user", new ApplicationHome(UserApplication.class).getDir().toPath());
        SpringApplication.run(UserApplication.class, args);
    }
}
