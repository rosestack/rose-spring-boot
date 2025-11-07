package io.github.rosestack.spring.boot.xxljob.config;

import io.github.rosestack.spring.boot.xxljob.client.XxlJobClient;
import io.github.rosestack.spring.boot.xxljob.client.XxlJobClientAuthInterceptor;
import io.github.rosestack.spring.boot.xxljob.client.registry.XxlJobRegistrar;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * 仅在 rose.xxl-job.client.enabled=true 时，提供最小的 RestTemplate 与 Admin 客户端
 */
@ConditionalOnProperty(prefix = "rose.xxl-job.client", name = "enabled", havingValue = "true")
public class XxlJobClientConfig {
    @Bean
    @ConditionalOnMissingBean
    XxlJobClientAuthInterceptor xxlJobClientAuthInterceptor(XxlJobProperties properties) {
        return new XxlJobClientAuthInterceptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(XxlJobClientAuthInterceptor.class)
    XxlJobClient xxlJobClient(
            XxlJobClientAuthInterceptor xxlJobClientAuthInterceptor,
            ObjectProvider<RestTemplate> restTemplateObjectProvider,
            XxlJobProperties properties) {
        RestTemplate restTemplate = restTemplateObjectProvider.getIfAvailable(RestTemplate::new);

        // 添加认证拦截器
        restTemplate.getInterceptors().add(xxlJobClientAuthInterceptor);

        return new XxlJobClient(restTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(XxlJobClient.class)
    XxlJobRegistrar xxlJobRegistrar(XxlJobClient xxlJobClient, XxlJobProperties properties) {
        return new XxlJobRegistrar(xxlJobClient, properties);
    }
}
