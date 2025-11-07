package io.github.rosestack.spring.boot.web.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 将当前 traceId 写入响应头，便于客户端与日志关联，同时也便于手工校验 MDC 透传效果。
 */
public class TracingConfig {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String MDC_TRACE_ID = "traceId";

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    @ConditionalOnBean(Tracer.class)
    @ConditionalOnProperty(
            prefix = "rose.web.filter.trace",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    FilterRegistrationBean<TraceIdFilter> requestIdFilter(Tracer tracer) {
        TraceIdFilter filter = new TraceIdFilter(tracer);
        FilterRegistrationBean<TraceIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName(filter.getClass().getSimpleName());
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registrationBean;
    }

    @RequiredArgsConstructor
    public static class TraceIdFilter extends OncePerRequestFilter {
        private final Tracer tracer;

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            // 在链路追踪过滤器之后执行：此时已存在当前 Span，可直接读取 traceId
            String traceId = null;
            if (tracer != null
                    && tracer.currentSpan() != null
                    && tracer.currentSpan().context() != null) {
                traceId = tracer.currentSpan().context().traceId();
            }
            if (traceId == null || traceId.isEmpty()) {
                traceId = MDC.get(MDC_TRACE_ID);
            }
            if (traceId != null && !traceId.isEmpty()) {
                response.setHeader(HEADER_TRACE_ID, traceId);
            }
            filterChain.doFilter(request, response);
        }
    }
}
