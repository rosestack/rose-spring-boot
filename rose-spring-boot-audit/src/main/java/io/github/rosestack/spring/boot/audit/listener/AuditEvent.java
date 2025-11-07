package io.github.rosestack.spring.boot.audit.listener;

import io.github.rosestack.spring.boot.audit.entity.AuditLog;
import io.github.rosestack.spring.boot.audit.entity.AuditLogDetail;
import java.util.List;
import lombok.Getter;

/**
 * 审计事件
 *
 * <p>当审计日志需要保存时发布此事件，监听器可以根据配置选择不同的存储方式。
 *
 * @author chensoul
 * @since 1.0.0
 */
@Getter
public class AuditEvent {

    /**
     * 审计日志主记录
     */
    private final AuditLog auditLog;

    /**
     * 审计日志详细记录列表
     */
    private final List<AuditLogDetail> auditLogDetails;

    /**
     * 构造函数
     *
     * @param auditLog        审计日志主记录
     * @param auditLogDetails 审计日志详细记录列表
     */
    public AuditEvent(AuditLog auditLog, List<AuditLogDetail> auditLogDetails) {
        this.auditLog = auditLog;
        this.auditLogDetails = auditLogDetails;
    }
}
