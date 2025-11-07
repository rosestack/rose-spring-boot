package io.github.rosestack.spring.boot.audit.listener;

import io.github.rosestack.spring.boot.audit.entity.AuditLog;
import io.github.rosestack.spring.boot.audit.entity.AuditLogDetail;
import io.github.rosestack.spring.boot.audit.support.storage.AuditStorage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@RequiredArgsConstructor
public class AuditEventListener {
    private final AuditStorage auditStorage;

    @Async
    @EventListener
    public void handleAuditEvent(AuditEvent auditEvent) {
        try {
            AuditLog auditLog = auditEvent.getAuditLog();
            List<AuditLogDetail> auditLogDetails = auditEvent.getAuditLogDetails();
            auditStorage.saveAuditLog(auditLog, auditLogDetails);
        } catch (Exception e) {
            log.error("处理审计事件失败: {}", e.getMessage(), e);
            // 根据配置决定是否重试或记录到备用存储
            handleEventProcessingFailure(auditEvent, e);
        }
    }

    /**
     * 处理事件处理失败的情况
     */
    private void handleEventProcessingFailure(AuditEvent auditEvent, Exception e) {
        // 可以在这里实现重试机制、备用存储等容错策略
        // 例如：将失败的事件写入文件或发送到死信队列

        // 记录失败统计
        recordFailureStats(auditEvent, e);
    }

    /**
     * 记录失败统计
     */
    private void recordFailureStats(AuditEvent auditEvent, Exception e) {
        // 实现失败统计逻辑
        log.warn("记录审计事件处理失败统计: 错误类型={}", e.getClass().getSimpleName());
    }
}
