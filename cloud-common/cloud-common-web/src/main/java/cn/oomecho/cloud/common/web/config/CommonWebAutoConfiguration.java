package cn.oomecho.cloud.common.web.config;


import cn.oomecho.cloud.common.web.exception.GlobalExceptionHandler;
import cn.oomecho.cloud.common.web.trace.RequestIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 公共 Web 自动配置；通过自动配置注册，业务服务无需扫描公共包
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@AutoConfiguration
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
public class CommonWebAutoConfiguration {
}
