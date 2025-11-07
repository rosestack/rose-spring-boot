package io.github.rosestack.spring.boot.web.config;

import io.github.rosestack.core.util.StringPool;
import io.github.rosestack.spring.boot.web.advice.ApiResponseBodyAdvice;
import io.github.rosestack.spring.boot.web.exception.GlobalExceptionHandler;
import io.github.rosestack.spring.factory.YmlPropertySourceFactory;
import io.github.rosestack.spring.filter.CachingRequestFilter;
import io.github.rosestack.spring.filter.LoggingRequestFilter;
import io.github.rosestack.spring.filter.XssRequestFilter;
import io.github.rosestack.spring.util.SpringContextUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Web 自动配置
 *
 * <p>提供 Web 相关的自动配置功能
 *
 * @author rosestack
 * @since 1.0.0
 */
@Import({
    AsyncConfig.class,
    SchedulingConfig.class,
    WebMvcConfig.class,
    OpenApiConfig.class,
    JacksonConfig.class,
    RestTemplateConfig.class,
    TracingConfig.class,
    LoggingConfig.class,
    // 精准引入组件（替代包扫描）
    ApiResponseBodyAdvice.class,
    GlobalExceptionHandler.class
})
@Slf4j
@RequiredArgsConstructor
@AutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(WebProperties.class)
@ConditionalOnProperty(prefix = "rose.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@PropertySource(value = "classpath:application-rose-web.yaml", factory = YmlPropertySourceFactory.class)
public class WebAutoConfig {
    private final WebProperties webProperties;

    @Order(value = Ordered.HIGHEST_PRECEDENCE)
    @EventListener(WebServerInitializedEvent.class)
    public void afterStart(WebServerInitializedEvent event) {
        String appName = SpringContextUtils.getApplicationName();
        int localPort = event.getWebServer().getPort();
        String profiles = String.join(StringPool.COMMA, SpringContextUtils.getActiveProfiles());
        log.info("Application {} finish to start with port {} and {} profile", appName, localPort, profiles);
    }

    @PostConstruct
    public void init() {
        log.info("Rose Web 自动配置已启用");
    }

    @Bean
    @ConditionalOnMissingBean(RequestContextListener.class)
    RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> enableDefaultServlet() {
        return (factory) -> factory.setRegisterDefaultServlet(true);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "rose.web.filter.caching",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    FilterRegistrationBean<CachingRequestFilter> cachingRequestFilter() {
        CachingRequestFilter filter =
                new CachingRequestFilter(webProperties.getFilter().getExcludePaths());
        FilterRegistrationBean<CachingRequestFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        registrationBean.addUrlPatterns(StringPool.ALL_PATH);
        registrationBean.setName(filter.getClass().getSimpleName());
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registrationBean;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "rose.web.filter.xss",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    FilterRegistrationBean<XssRequestFilter> xxsFilter() {
        XssRequestFilter filter = new XssRequestFilter(webProperties.getFilter().getExcludePaths());
        FilterRegistrationBean<XssRequestFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        registrationBean.addUrlPatterns(StringPool.ALL_PATH);
        registrationBean.setName(filter.getClass().getSimpleName());
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
        return registrationBean;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "rose.web.filter.logging",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public CommonsRequestLoggingFilter commonsRequestLoggingFilter() {
        final CommonsRequestLoggingFilter filter =
                new LoggingRequestFilter(webProperties.getFilter().getLogging().getMaxResponseTimeToLogInMs());
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(1000);
        filter.setIncludeHeaders(true);
        filter.setIncludeClientInfo(true);
        return filter;
    }
}
