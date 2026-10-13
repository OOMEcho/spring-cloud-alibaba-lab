package cn.oomecho.cloud.notification.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 通知服务仅扫描自己的 Mapper，拥有独立数据库
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Configuration(proxyBeanMethods = false)
@MapperScan("cn.oomecho.cloud.notification.mapper")
public class PersistenceConfiguration {
}
