package io.github.rosestack.spring.boot.audit.support.storage;

import io.github.rosestack.spring.boot.audit.entity.AuditLog;
import io.github.rosestack.spring.boot.audit.entity.AuditLogDetail;
import java.util.List;

public interface AuditStorage {
    AuditLog saveAuditLog(AuditLog auditLog, List<AuditLogDetail> auditLogDetails);
}
