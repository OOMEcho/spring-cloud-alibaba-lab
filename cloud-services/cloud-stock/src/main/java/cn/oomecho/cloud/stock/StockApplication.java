package cn.oomecho.cloud.stock;


import cn.oomecho.cloud.common.core.runtime.SdkRuntimeDirectories;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;

/**
 * 库存服务启动入口
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@SpringBootApplication
@MapperScan("cn.oomecho.cloud.stock.mapper")
public class StockApplication {
    /**
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SdkRuntimeDirectories.configure("cloud-stock", new ApplicationHome(StockApplication.class).getDir().toPath());
        SpringApplication.run(StockApplication.class, args);
    }
}
