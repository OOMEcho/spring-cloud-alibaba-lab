package cn.oomecho.cloud.order;

import cn.oomecho.cloud.api.product.client.ProductClient;
import cn.oomecho.cloud.api.stock.client.StockClient;
import cn.oomecho.cloud.api.user.client.UserClient;
import cn.oomecho.cloud.common.core.runtime.SdkRuntimeDirectories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务启动入口
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@SpringBootApplication
@EnableFeignClients(clients = {UserClient.class, ProductClient.class, StockClient.class})
public class OrderApplication {
    /**
     * 启动订单服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        // SDK 路径在配置导入前生效，IDEA 与 java -jar 启动遵循同一约定。
        SdkRuntimeDirectories.configure("cloud-order", new ApplicationHome(OrderApplication.class).getDir().toPath());
        SpringApplication.run(OrderApplication.class, args);
    }
}
