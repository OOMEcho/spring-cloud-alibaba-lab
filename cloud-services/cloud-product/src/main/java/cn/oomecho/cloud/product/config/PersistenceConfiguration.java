package cn.oomecho.cloud.product.config;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 本服务 Mapper 扫描配置；数据库组件不在离线或 P2 基础实验中加载
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Configuration(proxyBeanMethods = false)
@MapperScan("cn.oomecho.cloud.product.mapper")
public class PersistenceConfiguration {
}
