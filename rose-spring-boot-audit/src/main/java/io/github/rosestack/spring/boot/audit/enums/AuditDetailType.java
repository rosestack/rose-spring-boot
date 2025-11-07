package io.github.rosestack.spring.boot.audit.enums;

import lombok.Getter;

/**
 * 审计详情类型枚举
 *
 * <p>定义了审计日志详情表中的详情类型分类，用于对详情数据进行分类管理。 详情表采用 JSON 格式存储，支持复杂数据结构。
 *
 * @author chensoul
 * @since 1.0.0
 */
@Getter
public enum AuditDetailType {

    /**
     * HTTP请求相关 包含：请求参数、请求体、请求头、响应结果、响应头等
     */
    HTTP_REQUEST("HTTP请求相关", "HTTP请求和响应的详细信息"),

    /**
     * 操作对象相关 包含：操作目标对象信息、操作上下文、业务数据快照等
     */
    OPERATION_TARGET("操作对象相关", "操作目标对象和上下文信息"),

    /**
     * 数据变更相关 包含：变更前数据、变更后数据、变更差异、SQL语句等
     */
    DATA_CHANGE("数据变更相关", "数据变更的详细记录"),

    /**
     * 系统技术相关 包含：系统环境信息、性能指标、异常堆栈、调试信息等
     */
    SYSTEM_TECH("系统技术相关", "系统技术和性能相关信息"),

    /**
     * 安全相关 包含：安全上下文、权限检查、风险评估、威胁指标等
     */
    SECURITY("安全相关", "安全和风险相关信息");

    /**
     * 类型代码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String description;

    AuditDetailType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 类型代码
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static AuditDetailType fromCode(String code) {
        for (AuditDetailType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否为敏感类型 安全相关的详情类型通常包含敏感信息
     *
     * @return 是否为敏感类型
     */
    public boolean isSensitive() {
        return this == SECURITY || this == DATA_CHANGE;
    }
}
