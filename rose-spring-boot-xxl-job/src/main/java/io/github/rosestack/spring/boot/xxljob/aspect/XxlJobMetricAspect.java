package io.github.rosestack.spring.boot.xxljob.aspect;

import com.xxl.job.core.handler.annotation.XxlJob;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * 统计 @XxlJob 的执行耗时与结果（成功/失败）
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class XxlJobMetricAspect {
    private final MeterRegistry registry;

    @Around("@annotation(job)")
    public Object aroundXxlJob(ProceedingJoinPoint pjp, XxlJob job) throws Throwable {
        String name = job.value();
        String clazz = pjp.getSignature().getDeclaringTypeName();
        String method = pjp.getSignature().getName();
        String metricBase = "rose.xxljob.job";
        long start = System.nanoTime();
        // 添加运行时日志：开始执行
        log.info("xxl-job.start: name={}, class={}, method={}", name, clazz, method);
        try {
            Object ret = pjp.proceed();
            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            Timer.builder(metricBase + ".duration")
                    .description("XXL-Job duration in ms")
                    .tag("name", name)
                    .tag("class", clazz)
                    .tag("method", method)
                    .publishPercentileHistogram()
                    .register(registry)
                    .record(tookMs, TimeUnit.MILLISECONDS);
            registry.counter(metricBase + ".success", "name", name, "class", clazz, "method", method)
                    .increment();
            // 添加运行时日志：成功
            log.info("xxl-job.success: name={}, tookMs={}, class={}, method={}", name, tookMs, clazz, method);
            return ret;
        } catch (Throwable ex) {
            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            Timer.builder(metricBase + ".duration")
                    .description("XXL-Job duration in ms")
                    .tag("name", name)
                    .tag("class", clazz)
                    .tag("method", method)
                    .publishPercentileHistogram()
                    .register(registry)
                    .record(tookMs, TimeUnit.MILLISECONDS);
            registry.counter(metricBase + ".failure", "name", name, "class", clazz, "method", method)
                    .increment();
            // 添加运行时日志：失败（包含堆栈）
            log.warn("xxl-job.failed: name={}, tookMs={}, class={}, method={}", name, tookMs, clazz, method, ex);
            throw ex;
        }
    }
}
