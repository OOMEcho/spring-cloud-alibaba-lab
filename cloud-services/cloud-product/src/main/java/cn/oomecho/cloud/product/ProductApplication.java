package cn.oomecho.cloud.product;


import cn.oomecho.cloud.common.core.runtime.SdkRuntimeDirectories;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;

/**
 * 商品服务启动入口
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@SpringBootApplication
@MapperScan("cn.oomecho.cloud.product.mapper")
public class ProductApplication {
    /**
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SdkRuntimeDirectories.configure("cloud-product", new ApplicationHome(ProductApplication.class).getDir().toPath());
        SpringApplication.run(ProductApplication.class, args);
    }
}
