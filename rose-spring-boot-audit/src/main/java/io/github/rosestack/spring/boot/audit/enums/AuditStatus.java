package io.github.rosestack.spring.boot.audit.enums;

import lombok.Getter;

/**
 * 审计状态枚举
 *
 * <p>定义了审计日志的操作状态，用于标识操作的执行结果。
 *
 * @author chensoul
 * @since 1.0.0
 */
@Getter
public enum AuditStatus {

    /**
     * 成功
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 失败
     */
    FAILURE("FAILURE", "失败"),

    /**
     * 进行中
     */
    PENDING("PENDING", "进行中"),

    /**
     * 超时
     */
    TIMEOUT("TIMEOUT", "超时"),

    /**
     * 取消
     */
    CANCELLED("CANCELLED", "取消"),

    /**
     * 拒绝
     */
    DENIED("DENIED", "拒绝");

    /**
     * 状态代码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    AuditStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 状态代码
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static AuditStatus fromCode(String code) {
        for (AuditStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为成功状态
     *
     * @return 是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 判断是否为失败状态
     *
     * @return 是否为失败状态
     */
    public boolean isFailure() {
        return this == FAILURE || this == TIMEOUT || this == CANCELLED || this == DENIED;
    }
}
