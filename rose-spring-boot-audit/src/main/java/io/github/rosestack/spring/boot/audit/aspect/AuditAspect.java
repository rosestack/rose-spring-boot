package io.github.rosestack.spring.boot.audit.aspect;

import io.github.rosestack.crypto.FieldEncryptor;
import io.github.rosestack.spring.boot.audit.annotation.Audit;
import io.github.rosestack.spring.boot.audit.enums.AuditStatus;
import io.github.rosestack.spring.boot.audit.listener.AuditEvent;
import io.github.rosestack.spring.boot.audit.support.AuditEventBuilder;
import io.github.rosestack.spring.boot.audit.support.AuditEventConditionEvaluator;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;

@Slf4j
@Aspect
@Order(100) // 确保在事务切面之后执行
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rose.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditAspect {
    private final ApplicationEventPublisher eventPublisher;
    private final FieldEncryptor fieldEncryptor;

    /**
     * 环绕通知：拦截@Audit注解的方法
     */
    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        LocalDateTime startTime = LocalDateTime.now();
        long executionStartTime = System.currentTimeMillis();

        Object result = null;
        Throwable exception = null;
        AuditStatus status = AuditStatus.SUCCESS;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            status = AuditStatus.FAILURE;
            throw e;
        } finally {
            try {
                if (AuditEventConditionEvaluator.evaluate(joinPoint, audit.condition(), result)) {
                    long executionTime = System.currentTimeMillis() - executionStartTime;

                    AuditEventBuilder auditEventBuilder = new AuditEventBuilder(audit, fieldEncryptor);
                    AuditEvent auditEvent = auditEventBuilder.buildAuditEvent(
                            joinPoint, audit, startTime, executionTime, result, exception, status);

                    eventPublisher.publishEvent(auditEvent);
                    log.debug("发布审计事件成功，审计日志ID: {}", auditEvent.getAuditLog().getId());
                }
            } catch (Exception e) {
                log.error("记录审计日志失败: {}", e.getMessage(), e);
            }
        }
    }
}
