package io.github.rosestack.spring.boot.web.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;

public class RestTemplateConfig {
    @Bean
    public RestOperations restTemplate(RestTemplateBuilder builder) {
        // 使用 RestTemplateBuilder 配置请求工厂并设置超时，避免使用已弃用的超时方法；
        // 保持 Micrometer/Tracing 自动拦截器注入。
        return builder.requestFactory(() -> {
                    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
                    f.setConnectTimeout(3_000);
                    f.setReadTimeout(7_000);
                    return f;
                })
                .build();
    }
}
