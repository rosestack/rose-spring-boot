package io.github.rosestack.spring.boot.audit.config;

import io.github.rosestack.encrypt.FieldEncryptor;
import io.github.rosestack.spring.boot.audit.aspect.AuditAspect;
import io.github.rosestack.spring.boot.audit.listener.AuditEventListener;
import io.github.rosestack.spring.boot.audit.mapper.AuditLogDetailMapper;
import io.github.rosestack.spring.boot.audit.service.impl.AuditLogServiceImpl;
import io.github.rosestack.spring.boot.audit.support.storage.AuditStorage;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 审计日志自动配置类
 *
 * <p>提供审计日志功能的自动配置，包括： - 审计日志服务 - 审计切面 - 存储实现 - 加密脱敏工具
 *
 * @author chensoul
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(AuditProperties.class)
@MapperScan(basePackages = "io.github.rosestack.spring.boot.audit.mapper")
@ConditionalOnProperty(prefix = "rose.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditAutoConfig {
    private final AuditProperties auditProperties;

    @PostConstruct
    public void init() {
        log.info("审计配置: 存储类型={}", auditProperties.getStorage().getType());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rose.audit.aspect", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuditAspect auditAspect(ApplicationEventPublisher eventPublisher, FieldEncryptor fieldEncryptor) {
        log.debug("注册 AuditAspect Bean");
        return new AuditAspect(eventPublisher, fieldEncryptor);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "rose.audit.storage",
            name = "type",
            havingValue = "database",
            matchIfMissing = true)
    public AuditStorage jdbcAuditStorage(Validator validator, AuditLogDetailMapper auditLogDetailMapper) {
        return new AuditLogServiceImpl(validator, auditLogDetailMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditStorage noopAuditLogService() {
        return (auditLog, auditLogDetails) -> null;
    }

    @Bean
    public AuditEventListener auditEventListener(AuditStorage auditStorage) {
        return new AuditEventListener(auditStorage);
    }
}
