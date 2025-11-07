package io.github.rosestack.spring.boot.xxljob.config;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.rosestack.spring.boot.xxljob.aspect.XxlJobMetricAspect;
import io.github.rosestack.spring.boot.xxljob.exception.XxlJobException;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * XXL-Job 自动配置
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(XxlJobProperties.class)
@ConditionalOnClass({XxlJobExecutor.class, XxlJobSpringExecutor.class})
@Import(XxlJobClientConfig.class)
@ConditionalOnProperty(prefix = "rose.xxl-job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobAutoConfig {

    private static final String XXL_JOB_ADMIN = "rose-xxl-job";
    private final ObjectProvider<DiscoveryClient> discoveryClientObjectProvider;

    @Bean
    @ConditionalOnMissingBean
    public XxlJobSpringExecutor xxlJobExecutor(XxlJobProperties props, Environment env) {
        // 解析 appName：优先使用 rose.xxl-job.appname，其次 spring.application.name
        String appname = getAppName(props, env);
        if (StringUtils.isBlank(appname)) {
            throw new XxlJobException("应用名称不能为空");
        }

        log.info("xxl-job.executor.config: appname={}, adminAddresses={}", appname, props.getAdminAddresses());
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAppname(appname);
        executor.setPort(props.getPort());
        executor.setAccessToken(props.getAccessToken());
        executor.setLogPath(props.getLogPath());
        executor.setLogRetentionDays(props.getLogRetentionDays());

        if (discoveryClientObjectProvider.getIfAvailable() != null) {
            DiscoveryClient discoveryClient = discoveryClientObjectProvider.getObject();
            String serverList = discoveryClient.getServices().stream()
                    .filter(s -> s.contains(XXL_JOB_ADMIN))
                    .flatMap(s -> discoveryClient.getInstances(s).stream())
                    .map(XxlJobAutoConfig::getServiceUrl)
                    .collect(Collectors.joining(","));
            executor.setAdminAddresses(serverList);
        } else {
            if (StringUtils.isBlank(props.getAdminAddresses())) {
                throw new XxlJobException("调度器地址不能为空");
            }
            executor.setAdminAddresses(props.getAdminAddresses());
        }
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean(name = "xxlJobHealthIndicator")
    public HealthIndicator xxlJobHealthIndicator(XxlJobProperties props, Environment env) {
        return () -> {
            // 基础：无主动探测，仅报告 UP（装配成功）及静态信息
            Health.Builder builder = Health.up()
                    .withDetail("appName", getAppName(props, env))
                    .withDetail("port", props.getPort())
                    .withDetail("adminAddresses", props.getAdminAddresses());
            return builder.build();
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rose.xxl-job.metrics", name = "enabled", havingValue = "true")
    @ConditionalOnClass({MeterRegistry.class, XxlJob.class})
    XxlJobMetricAspect xxlJobMetricsAspect(MeterRegistry registry) {
        return new XxlJobMetricAspect(registry);
    }

    private static String getAppName(XxlJobProperties props, Environment env) {
        return StringUtils.defaultIfBlank(props.getAppname(), env.getProperty("spring.application.name"));
    }

    private static String getServiceUrl(ServiceInstance instance) {
        return String.format(
                Locale.getDefault(),
                "%s://%s:%s",
                instance.isSecure() ? "https" : "http",
                instance.getHost(),
                instance.getPort());
    }
}
