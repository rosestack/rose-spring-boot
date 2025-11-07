package io.github.rosestack.spring.boot.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
@ConditionalOnClass(RetryOperationsInterceptor.class)
public class SpringRetryConfig {
    private static final Logger log = LoggerFactory.getLogger(SpringRetryConfig.class);

    @Bean
    @ConditionalOnMissingBean(name = "configServerRetryInterceptor")
    public RetryOperationsInterceptor configServerRetryInterceptor() {
        log.info("Changing backOffOptions  to initial: {}, multiplier: {}, maxInterval: {}", 1000, 1.2, 5000);
        return RetryInterceptorBuilder.stateless()
                .backOffOptions(1000, 1.2, 5000)
                .maxAttempts(3)
                .build();
    }
}
