package io.github.rosestack.spring.boot.audit.enums;

import lombok.Getter;

/**
 * 风险等级枚举
 *
 * <p>定义了审计事件的风险等级，用于安全分析和告警。
 *
 * @author chensoul
 * @since 1.0.0
 */
@Getter
public enum AuditRiskLevel implements Comparable<AuditRiskLevel> {

    /**
     * 低风险
     */
    LOW("LOW", "低风险", 1),

    /**
     * 中等风险
     */
    MEDIUM("MEDIUM", "中等风险", 2),

    /**
     * 高风险
     */
    HIGH("HIGH", "高风险", 3),

    /**
     * 严重风险
     */
    CRITICAL("CRITICAL", "严重风险", 4);

    /**
     * 风险等级代码
     */
    private final String code;

    /**
     * 风险等级描述
     */
    private final String description;

    /**
     * 风险等级数值（用于比较）
     */
    private final int level;

    AuditRiskLevel(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 风险等级代码
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static AuditRiskLevel fromCode(String code) {
        for (AuditRiskLevel auditRiskLevel : values()) {
            if (auditRiskLevel.getCode().equals(code)) {
                return auditRiskLevel;
            }
        }
        return null;
    }

    /**
     * 根据事件类型自动判断风险等级
     *
     * @param eventType 事件类型
     * @return 对应的风险等级
     */
    public static AuditRiskLevel fromEventType(AuditEventType eventType) {
        if (eventType.isHighRiskEvent()) {
            return HIGH;
        } else if (eventType.isSecurityEvent()) {
            return CRITICAL;
        } else if (eventType.getEventType().equals("数据")) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }

    /**
     * 判断是否为高风险等级
     *
     * @return 是否为高风险等级
     */
    public boolean isHighRisk() {
        return this.level >= HIGH.level;
    }

    /**
     * 判断是否需要告警
     *
     * @return 是否需要告警
     */
    public boolean needsAlert() {
        return this.level >= MEDIUM.level;
    }

    //    /**
    //     * 比较风险等级
    //     *
    //     * @param other 另一个风险等级
    //     * @return 比较结果：负数表示当前等级较低，0表示相等，正数表示当前等级较高
    //     */
    //    public int compareTo(AuditRiskLevel other) {
    //        return Integer.compare(this.level, other.level);
    //    }

}
