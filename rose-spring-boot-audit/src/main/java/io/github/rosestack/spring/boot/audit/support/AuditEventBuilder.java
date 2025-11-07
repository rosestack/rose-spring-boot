package io.github.rosestack.spring.boot.audit.support;

import io.github.rosestack.core.util.JsonUtils;
import io.github.rosestack.core.util.SensitiveUtils;
import io.github.rosestack.crypto.FieldEncryptor;
import io.github.rosestack.spring.boot.audit.annotation.Audit;
import io.github.rosestack.spring.boot.audit.entity.AuditLog;
import io.github.rosestack.spring.boot.audit.entity.AuditLogDetail;
import io.github.rosestack.spring.boot.audit.enums.AuditDetailKey;
import io.github.rosestack.spring.boot.audit.enums.AuditEventType;
import io.github.rosestack.spring.boot.audit.enums.AuditRiskLevel;
import io.github.rosestack.spring.boot.audit.enums.AuditStatus;
import io.github.rosestack.spring.boot.audit.listener.AuditEvent;
import io.github.rosestack.spring.util.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * 审计日志构建器
 *
 * <p>负责构建 {@link AuditLog} 对象，设置基本的审计信息。 包括操作名称、事件类型、风险等级、执行时间等。
 *
 * @author chensoul
 * @since 1.0.0
 */
@Slf4j
public class AuditEventBuilder {
    private static final List<String> DEFAULT_MASK_FIELDS = Arrays.asList(
            "password",
            "oldPassword",
            "newPassword",
            "newPasswordAgain",
            "token",
            "access_token",
            "refresh_token",
            "secret",
            "key");

    private final Audit audit;
    private final FieldEncryptor fieldEncryptor;
    private final Set<String> maskFields = new LinkedHashSet<>();

    public AuditEventBuilder(Audit audit, FieldEncryptor fieldEncryptor) {
        this.audit = audit;
        this.maskFields.addAll(DEFAULT_MASK_FIELDS);
        this.maskFields.addAll(Arrays.asList(audit.maskFields()));
        this.fieldEncryptor = fieldEncryptor;
    }

    /**
     * 记录审计日志
     */
    public AuditEvent buildAuditEvent(
            ProceedingJoinPoint joinPoint,
            Audit audit,
            LocalDateTime startTime,
            long executionTime,
            Object result,
            Throwable exception,
            AuditStatus status) {
        AuditLog auditLog = buildAuditLog(joinPoint, audit, startTime, executionTime, status);
        List<AuditLogDetail> auditLogDetails = buildAuditDetails(joinPoint, audit, auditLog.getId(), result, exception);
        return new AuditEvent(auditLog, auditLogDetails);
    }

    /**
     * 构建审计日志对象
     */
    private AuditLog buildAuditLog(
            ProceedingJoinPoint joinPoint,
            Audit audit,
            LocalDateTime startTime,
            long executionTime,
            AuditStatus status) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 构建审计日志
        AuditLog auditLog = AuditLog.builder()
                .eventTime(startTime)
                .operationName(getOperationName(audit, method))
                .status(status.getCode())
                .executionTime(executionTime)
                .build();

        AuditEventType eventType = getEventType(audit, method);
        auditLog.setEventType(eventType);
        auditLog.setEventSubtype(eventType.getEventSubType());
        auditLog.setRiskLevel(getRiskLevel(audit, eventType));

        setHttpInfo(auditLog);

        return auditLog;
    }

    /**
     * 构建审计详情列表
     */
    private List<AuditLogDetail> buildAuditDetails(
            ProceedingJoinPoint joinPoint, Audit audit, Long auditLogId, Object result, Throwable exception) {
        List<AuditLogDetail> details = new ArrayList<>();

        try {
            // 记录方法参数
            if (audit.recordParams()) {
                details.add(buildParameterDetail(auditLogId, joinPoint));
            }

            // 记录方法返回值
            if (audit.recordReturnValue() && result != null) {
                details.add(createDetail(auditLogId, AuditDetailKey.RESPONSE_RESULT, result));
            }

            // 记录HTTP请求信息
            details.addAll(buildHttpDetails(auditLogId));

            // 记录异常信息
            if (exception != null && audit.recordException()) {
                details.addAll(buildExceptionDetails(auditLogId, exception));
            }
        } catch (Exception e) {
            log.error("构建审计详情失败: {}", e.getMessage(), e);
        }

        return details;
    }

    /**
     * 构建参数详情
     */
    private AuditLogDetail buildParameterDetail(Long auditLogId, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();

        List<Object> newArgs = new ArrayList<>();
        if (joinPoint.getArgs() != null) {
            for (int i = 0; i < parameters.length && i < args.length; i++) {
                Parameter parameter = parameters[i];
                Object arg = args[i];

                // 跳过特殊类型的参数
                if (isSpecialType(parameter.getType())) {
                    continue;
                }
                newArgs.add(arg);
            }
        }

        return createDetail(auditLogId, AuditDetailKey.REQUEST_PARAMS, newArgs);
    }

    /**
     * 判断是否为特殊类型（不需要序列化的类型）
     */
    private boolean isSpecialType(Class<?> type) {
        return HttpServletRequest.class.isAssignableFrom(type)
                || type.getName().startsWith("org.springframework.")
                || type.getName().startsWith("javax.servlet.")
                || type.getName().startsWith("jakarta.servlet.");
    }

    /**
     * 构建HTTP详情
     */
    private List<AuditLogDetail> buildHttpDetails(Long auditLogId) {
        List<AuditLogDetail> details = new ArrayList<>();

        try {
            // 获取 request 请求头
            Map<String, String> headers = ServletUtils.getRequestHeaders();
            if (!headers.isEmpty()) {
                details.add(createDetail(auditLogId, AuditDetailKey.REQUEST_HEADERS, headers));
            }

            // 获取 response 请求头
            headers = ServletUtils.getResponseHeaders();
            if (!headers.isEmpty()) {
                details.add(createDetail(auditLogId, AuditDetailKey.RESPONSE_HEADERS, headers));
            }
        } catch (Exception e) {
            log.warn("构建HTTP详情失败: {}", e.getMessage());
        }

        return details;
    }

    /**
     * 构建异常详情
     */
    private List<AuditLogDetail> buildExceptionDetails(Long auditLogId, Throwable exception) {
        List<AuditLogDetail> details = new ArrayList<>();

        try {
            Map<String, Object> exceptionInfo = new HashMap<>();
            exceptionInfo.put("type", exception.getClass().getName());
            exceptionInfo.put("message", exception.getMessage());
            exceptionInfo.put("stackTrace", ExceptionUtils.getStackTrace(exception));

            details.add(createDetail(auditLogId, AuditDetailKey.EXCEPTION_STACK, exceptionInfo));
        } catch (Exception e) {
            log.warn("构建异常详情失败: {}", e.getMessage());
        }

        return details;
    }

    /**
     * 获取操作名称
     */
    private String getOperationName(Audit audit, Method method) {
        if (StringUtils.isNoneBlank(audit.value())) {
            return audit.value();
        }
        if (StringUtils.isNoneBlank(audit.value())) {
            return audit.value();
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    /**
     * 获取事件类型
     */
    private AuditEventType getEventType(Audit audit, Method method) {
        if (audit.eventType() != AuditEventType.DATA_OTHER) {
            return audit.eventType();
        }

        // 根据方法名推断事件类型
        String methodName = method.getName().toLowerCase();
        if (methodName.contains("login") || methodName.contains("logout") || methodName.contains("auth")) {
            return AuditEventType.AUTH_LOGIN;
        } else if (methodName.contains("create") || methodName.contains("add") || methodName.contains("insert")) {
            return AuditEventType.DATA_CREATE;
        } else if (methodName.contains("update") || methodName.contains("modify") || methodName.contains("edit")) {
            return AuditEventType.DATA_UPDATE;
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            return AuditEventType.DATA_DELETE;
        } else if (methodName.contains("query")
                || methodName.contains("find")
                || methodName.contains("get")
                || methodName.contains("list")) {
            return AuditEventType.DATA_READ;
        }

        return AuditEventType.DATA_OTHER;
    }

    /**
     * 获取风险等级
     */
    private AuditRiskLevel getRiskLevel(Audit audit, AuditEventType eventType) {
        if (audit.riskLevel() != AuditRiskLevel.LOW) {
            return audit.riskLevel();
        }
        return AuditRiskLevel.fromEventType(eventType);
    }

    /**
     * 设置HTTP信息
     */
    private void setHttpInfo(AuditLog auditLog) {
        HttpServletRequest request = ServletUtils.getCurrentRequest();
        if (request != null) {
            auditLog.setRequestUri(request.getRequestURI());
            auditLog.setHttpMethod(request.getMethod());
            auditLog.setClientIp(ServletUtils.getClientIp());
            auditLog.setUserAgent(ServletUtils.getUserAgent());
            auditLog.setSessionId(ServletUtils.getCurrentRequest().getSession().getId());
        }
    }

    public AuditLogDetail createDetail(Long auditLogId, AuditDetailKey detailKey, Object detailValue) {
        AuditLogDetail auditLogDetail = AuditLogDetail.builder()
                .auditLogId(auditLogId)
                .detailType(detailKey.getDetailType().getCode())
                .detailKey(detailKey.getCode())
                .isEncrypted(detailKey.isEncrypted())
                .build();

        if (detailKey.isSensitive()) {
            auditLogDetail.setDetailValue(JsonUtils.toString(
                    SensitiveUtils.maskSensitiveFields(detailValue, maskFields.toArray(new String[] {}))));
            auditLogDetail.setIsSensitive(auditLogDetail.getDetailValue().contains(SensitiveUtils.MASKED));
        } else {
            auditLogDetail.setIsSensitive(false);
        }

        if (detailKey.isEncrypted()) {
            auditLogDetail.setDetailValue(fieldEncryptor.decrypt(auditLogDetail.getDetailValue(), audit.encryptType()));
            auditLogDetail.setIsEncrypted(true);
        } else {
            auditLogDetail.setIsEncrypted(false);
        }

        return auditLogDetail;
    }
}
