package io.github.rosestack.spring.boot.audit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.rosestack.spring.boot.audit.entity.AuditLog;
import io.github.rosestack.spring.boot.audit.support.storage.AuditStorage;

public interface AuditLogService extends IService<AuditLog>, AuditStorage {}
