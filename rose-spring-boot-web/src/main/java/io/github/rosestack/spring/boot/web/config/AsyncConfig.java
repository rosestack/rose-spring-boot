package io.github.rosestack.spring.boot.web.config;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    /**
     * 系统 CPU 核心数，用于计算默认线程池大小
     */
    private final int core = Runtime.getRuntime().availableProcessors();

    private final TaskExecutionProperties properties;

    public AsyncConfig(TaskExecutionProperties properties) {
        this.properties = properties;
    }

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        log.info("Creating Async Task Executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getPool().getCoreSize());
        executor.setMaxPoolSize(properties.getPool().getMaxSize());
        executor.setQueueCapacity(properties.getPool().getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setAllowCoreThreadTimeOut(properties.getPool().isAllowCoreThreadTimeout());
        executor.setKeepAliveSeconds((int) properties.getPool().getKeepAlive().toSeconds());

        // 关闭配置
        TaskExecutionProperties.Shutdown shutdown = properties.getShutdown();
        executor.setWaitForTasksToCompleteOnShutdown(shutdown.isAwaitTermination());
        if (shutdown.getAwaitTerminationPeriod() != null) {
            executor.setAwaitTerminationSeconds(
                    (int) shutdown.getAwaitTerminationPeriod().getSeconds());
        }

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();

        log.info(
                "创建异步线程池，核心线程数：{}，最大线程数：{}，队列容量：{}，线程前缀：{}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity(),
                executor.getThreadNamePrefix());

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("Async void method error: {}({})", method.getName(), Arrays.toString(params), ex);
    }

    @Bean
    BeanPostProcessor threadPoolTaskExecutorBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (!(bean instanceof ThreadPoolTaskExecutor)) {
                    return bean;
                }
                ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) bean;
                executor.setTaskDecorator(new CopyContextTaskDecorator());
                return executor;
            }
        };
    }

    public static class CopyContextTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            final var contextMap = MDC.getCopyOfContextMap();
            return () -> {
                // 将父线程的 MDC 上下文透传到子线程，确保 traceId/spanId 在异步日志中可见
                Map<String, String> previous = null;
                try {
                    previous = MDC.getCopyOfContextMap();
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    if (previous == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previous);
                    }
                }
            };
        }
    }
}
