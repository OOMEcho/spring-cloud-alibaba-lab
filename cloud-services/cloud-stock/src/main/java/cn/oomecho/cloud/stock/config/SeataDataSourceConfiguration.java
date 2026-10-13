package cn.oomecho.cloud.stock.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

/**
 * AT 数据源初始化顺序：原始连接池执行 Flyway，迁移完成后才创建唯一 Seata 代理
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "seata.enabled", havingValue = "true")
@EnableConfigurationProperties({DataSourceProperties.class, FlywayProperties.class})
public class SeataDataSourceConfiguration {
    /**
     * @param properties 数据库连接参数 @return Flyway 原始连接池，不执行 AT SQL 拦截
     */
    @Bean
    @FlywayDataSource
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource rawDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /**
     * @param raw 未代理的迁移连接池 @param properties 当前服务迁移路径 @return 只从原始池执行的 Flyway
     */
    @Bean
    public Flyway flyway(@Qualifier("rawDataSource") HikariDataSource raw, FlywayProperties properties) {
        return Flyway.configure().dataSource(raw).locations(properties.getLocations().toArray(String[]::new)).load();
    }

    /**
     * @param flyway 原始连接池的迁移器 @return 显式初始化，避免 Boot 选择主代理数据源造成循环依赖
     */
    @Bean
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    /**
     * @param raw 已完成迁移的连接池
     * @return 业务 JDBC、MyBatis 和事务管理器统一使用的唯一代理；连接池由 rawDataSource 关闭
     */
    @Bean(destroyMethod = "")
    @Primary
    @DependsOn("flywayInitializer")
    public DataSourceProxy dataSource(@Qualifier("rawDataSource") HikariDataSource raw) {
        return new DataSourceProxy(raw);
    }
}
