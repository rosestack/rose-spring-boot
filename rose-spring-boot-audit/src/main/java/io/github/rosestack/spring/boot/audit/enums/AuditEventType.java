package io.github.rosestack.spring.boot.audit.enums;

import lombok.Getter;

/**
 * 审计事件类型枚举
 *
 * <p>定义了审计日志的事件类型和子类型，采用混合模式设计： - eventType: 技术分类（认证、授权、数据、系统、网络、安全） - eventSubType:
 * 技术子类（用户登录、数据更新等） - operation_name: 具体业务操作（在主表中单独字段存储）
 *
 * @author chensoul
 * @since 1.0.0
 */
@Getter
public enum AuditEventType {

    // ==================== 认证类 ====================
    AUTH_LOGIN("认证", "用户登录"),
    AUTH_LOGOUT("认证", "用户登出"),
    AUTH_PASSWORD_CHANGE("认证", "密码修改"),
    AUTH_PASSWORD_RESET("认证", "密码重置"),
    AUTH_SESSION_TIMEOUT("认证", "会话超时"),

    // ==================== 授权类 ====================
    AUTHZ_PERMISSION_DENIED("授权", "权限拒绝"),
    AUTHZ_CHANGE("授权", "授权变更"),

    // ==================== 数据类 ====================
    DATA_CREATE("数据", "数据创建"),
    DATA_READ("数据", "数据读取"),
    DATA_UPDATE("数据", "数据更新"),
    DATA_DELETE("数据", "数据删除"),
    DATA_BATCH_OPERATION("数据", "批量操作"),
    DATA_EXPORT("数据", "数据导出"),
    DATA_IMPORT("数据", "数据导入"),
    DATA_SENSITIVE_ACCESS("数据", "敏感数据访问"),
    DATA_OTHER("数据", "其他"),

    // ==================== 系统类 ====================
    SYS_CONFIG_CHANGE("系统", "配置变更"),
    SYS_SERVICE_CONTROL("系统", "服务控制"),
    SYS_FILE_OPERATION("系统", "文件操作"),
    SYS_MAINTENANCE("系统", "系统维护"),
    SYS_EXTERNAL_REQUEST("系统", "外部请求"),

    // ==================== 安全类 ====================
    SEC_ATTACK_DETECTION("安全", "攻击检测"),
    SEC_ABNORMAL_BEHAVIOR("安全", "异常行为");

    /**
     * 事件类型
     */
    private final String eventType;

    /**
     * 事件子类型
     */
    private final String eventSubType;

    AuditEventType(String eventType, String eventSubType) {
        this.eventType = eventType;
        this.eventSubType = eventSubType;
    }

    /**
     * 判断是否为安全事件
     *
     * @return 是否为安全事件
     */
    public boolean isSecurityEvent() {
        return "安全".equals(this.eventType);
    }

    /**
     * 判断是否为高风险事件
     *
     * @return 是否为高风险事件
     */
    public boolean isHighRiskEvent() {
        return this.isSecurityEvent()
                || this == DATA_SENSITIVE_ACCESS
                || this == DATA_DELETE
                || this == SYS_CONFIG_CHANGE
                || this == AUTHZ_CHANGE;
    }

    /**
     * 获取事件代码（用于日志记录）
     *
     * @return 事件代码
     */
    public String getEventCode() {
        return this.name();
    }

    /**
     * 获取完整描述
     *
     * @return 完整描述
     */
    public String getFullDescription() {
        return eventType + " - " + eventSubType;
    }
}
