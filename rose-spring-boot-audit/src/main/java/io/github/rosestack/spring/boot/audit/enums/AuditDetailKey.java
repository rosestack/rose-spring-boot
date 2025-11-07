package io.github.rosestack.spring.boot.audit.enums;

import lombok.Getter;

/**
 * 审计详情键枚举
 *
 * <p>定义了审计日志详情表中的具体详情键，每个键对应一种特定的详情数据类型。 详情值以 JSON 格式存储，支持复杂的数据结构。
 *
 * @author chensoul
 * @since 1.0.0
 */
@Getter
public enum AuditDetailKey {

    // ==================== HTTP请求相关 ====================
    REQUEST_PARAMS("REQUEST_PARAMS", "HTTP请求参数", AuditDetailType.HTTP_REQUEST, false, true),
    REQUEST_HEADERS("REQUEST_HEADERS", "HTTP请求头", AuditDetailType.HTTP_REQUEST, false, false),
    RESPONSE_RESULT("RESPONSE_RESULT", "HTTP响应结果", AuditDetailType.HTTP_REQUEST, false, false),
    RESPONSE_HEADERS("RESPONSE_HEADERS", "HTTP响应头", AuditDetailType.HTTP_REQUEST, true, false),

    // ==================== 操作对象相关 ====================
    TARGET_INFO("TARGET_INFO", "操作目标对象信息", AuditDetailType.OPERATION_TARGET, false, false),
    OPERATION_CONTEXT("OPERATION_CONTEXT", "操作上下文", AuditDetailType.OPERATION_TARGET, false, false),
    BUSINESS_DATA("BUSINESS_DATA", "业务数据快照", AuditDetailType.OPERATION_TARGET, false, true),

    // ==================== 数据变更相关 ====================
    DATA_CHANGE_BEFORE("DATA_CHANGE_BEFORE", "变更前数据", AuditDetailType.DATA_CHANGE, false, true),
    DATA_CHANGE_AFTER("DATA_CHANGE_AFTER", "变更后数据", AuditDetailType.DATA_CHANGE, false, true),
    DATA_CHANGE_DIFF("DATA_CHANGE_DIFF", "变更差异对比", AuditDetailType.DATA_CHANGE, false, false),
    SQL_STATEMENT("SQL_STATEMENT", "执行的SQL语句", AuditDetailType.DATA_CHANGE, false, false),
    SQL_PARAMETERS("SQL_PARAMETERS", "SQL参数", AuditDetailType.DATA_CHANGE, false, true),

    // ==================== 系统技术相关 ====================
    SYSTEM_ENV("SYSTEM_ENV", "系统环境信息", AuditDetailType.SYSTEM_TECH, false, false),
    PERFORMANCE_METRICS("PERFORMANCE_METRICS", "性能指标", AuditDetailType.SYSTEM_TECH, false, false),
    ERROR_DETAIL("ERROR_DETAIL", "错误详情", AuditDetailType.SYSTEM_TECH, false, false),
    EXCEPTION_STACK("EXCEPTION_STACK", "异常堆栈", AuditDetailType.SYSTEM_TECH, false, false),
    DEBUG_INFO("DEBUG_INFO", "调试信息", AuditDetailType.SYSTEM_TECH, false, false),

    // ==================== 安全相关 ====================
    SECURITY_CONTEXT("SECURITY_CONTEXT", "安全上下文", AuditDetailType.SECURITY, false, true),
    PERMISSION_CHECK("PERMISSION_CHECK", "权限检查详情", AuditDetailType.SECURITY, false, false),
    RISK_ASSESSMENT("RISK_ASSESSMENT", "风险评估结果", AuditDetailType.SECURITY, false, false),
    THREAT_INDICATORS("THREAT_INDICATORS", "威胁指标", AuditDetailType.SECURITY, false, true);

    /**
     * 详情键代码
     */
    private final String code;

    /**
     * 详情键描述
     */
    private final String description;

    /**
     * 所属详情类型
     */
    private final AuditDetailType detailType;

    private final boolean encrypted;

    /**
     * 是否包含敏感数据
     */
    private final boolean sensitive;

    AuditDetailKey(String code, String description, AuditDetailType detailType, boolean encrypted, boolean sensitive) {
        this.code = code;
        this.description = description;
        this.detailType = detailType;
        this.encrypted = encrypted;
        this.sensitive = sensitive;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 详情键代码
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static AuditDetailKey fromCode(String code) {
        for (AuditDetailKey key : values()) {
            if (key.getCode().equals(code)) {
                return key;
            }
        }
        return null;
    }

    /**
     * 获取指定详情类型下的所有详情键
     *
     * @param detailType 详情类型
     * @return 该类型下的所有详情键
     */
    public static AuditDetailKey[] getKeysByType(AuditDetailType detailType) {
        return java.util.Arrays.stream(values())
                .filter(key -> key.getDetailType() == detailType)
                .toArray(AuditDetailKey[]::new);
    }

    /**
     * 判断是否需要加密存储 敏感数据需要加密存储
     *
     * @return 是否需要加密
     */
    public boolean needsEncrypt() {
        return this.encrypted;
    }

    /**
     * 判断是否需要脱敏处理 敏感数据在记录时需要脱敏
     *
     * @return 是否需要脱敏
     */
    public boolean needsMasking() {
        return this.sensitive;
    }
}
