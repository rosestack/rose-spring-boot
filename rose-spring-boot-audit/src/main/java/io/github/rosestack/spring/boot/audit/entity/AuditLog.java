package io.github.rosestack.spring.boot.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.github.rosestack.spring.boot.audit.enums.AuditEventType;
import io.github.rosestack.spring.boot.audit.enums.AuditRiskLevel;
import io.github.rosestack.spring.boot.audit.enums.AuditStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计日志主表实体类
 *
 * <p>存储审计日志的核心信息，包括事件基本信息、用户信息、HTTP信息、执行结果等。 采用混合模式设计：技术分类使用枚举，具体业务操作使用字符串。 支持多租户、加密存储、完整性保护等特性。
 *
 * @author chensoul
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("audit_log")
public class AuditLog {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 事件时间
     */
    @NotNull(message = "事件时间不能为空") @TableField("event_time")
    private LocalDateTime eventTime;

    /**
     * 事件类型（技术分类）
     */
    @NotBlank(message = "事件类型不能为空") @Size(max = 50, message = "事件类型长度不能超过50个字符") @TableField("event_type")
    private String eventType;

    /**
     * 事件子类型（技术子分类）
     */
    @Size(max = 50, message = "事件子类型长度不能超过50个字符") @TableField("event_subtype")
    private String eventSubtype;

    /**
     * 具体业务操作名称
     */
    @Size(max = 200, message = "操作名称长度不能超过200个字符") @TableField("operation_name")
    private String operationName;

    /**
     * 操作状态
     */
    @NotBlank(message = "操作状态不能为空") @Size(max = 20, message = "操作状态长度不能超过20个字符") @TableField("status")
    private String status;

    /**
     * 风险等级
     */
    @NotBlank(message = "风险等级不能为空") @Size(max = 20, message = "风险等级长度不能超过20个字符") @TableField("risk_level")
    private String riskLevel;

    /**
     * 用户ID
     */
    @Size(max = 64, message = "用户ID长度不能超过64个字符") @TableField("user_id")
    private String userId;

    /**
     * 用户名
     */
    @Size(max = 100, message = "用户名长度不能超过100个字符") @TableField("user_name")
    private String userName;

    /**
     * 请求URI
     */
    @Size(max = 500, message = "请求URI长度不能超过500个字符") @TableField("request_uri")
    private String requestUri;

    /**
     * HTTP方法
     */
    @Size(max = 10, message = "HTTP方法长度不能超过10个字符") @TableField("http_method")
    private String httpMethod;

    /**
     * HTTP状态码
     */
    @TableField("http_status")
    private Integer httpStatus;

    /**
     * 会话ID
     */
    @Size(max = 128, message = "会话ID长度不能超过128个字符") @TableField("session_id")
    private String sessionId;

    /**
     * 客户端IP地址
     */
    @Size(max = 45, message = "客户端IP长度不能超过45个字符") @TableField("client_ip")
    private String clientIp;

    /**
     * 服务器IP地址
     */
    @Size(max = 45, message = "服务器IP长度不能超过45个字符") @TableField("server_ip")
    private String serverIp;

    /**
     * 地理位置信息
     */
    @Size(max = 200, message = "地理位置信息长度不能超过200个字符") @TableField("geo_location")
    private String geoLocation;

    /**
     * 用户代理简要信息
     */
    @Size(max = 100, message = "用户代理信息长度不能超过100个字符") @TableField("user_agent")
    private String userAgent;

    /**
     * 应用名称
     */
    @Size(max = 100, message = "应用名称长度不能超过100个字符") @TableField("app_name")
    private String appName;

    /**
     * 租户ID（多租户支持）
     */
    @Size(max = 50, message = "租户ID长度不能超过50个字符") @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    /**
     * 追踪ID（链路追踪）
     */
    @Size(max = 100, message = "追踪ID长度不能超过100个字符") @TableField("trace_id")
    private String traceId;

    /**
     * 执行耗时（毫秒）
     */
    @TableField("execution_time")
    private Long executionTime;

    /**
     * 数字签名（完整性保护）
     */
    @Size(max = 512, message = "数字签名长度不能超过512个字符") @TableField("digital_signature")
    private String digitalSignature;

    /**
     * 哈希值（完整性保护）
     */
    @Size(max = 128, message = "哈希值长度不能超过128个字符") @TableField("hash_value")
    private String hashValue;

    /**
     * 前一条记录哈希值（链式完整性保护）
     */
    @Size(max = 128, message = "前一条记录哈希值长度不能超过128个字符") @TableField("prev_hash")
    private String prevHash;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 逻辑删除标识
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;

    // ==================== 业务方法 ====================

    /**
     * 设置事件类型（从枚举）
     *
     * @param eventType 事件类型枚举
     */
    public void setEventType(AuditEventType eventType) {
        if (eventType != null) {
            this.eventType = eventType.getEventType();
            this.eventSubtype = eventType.getEventSubType();
        }
    }

    /**
     * 设置操作状态（从枚举）
     *
     * @param status 状态枚举
     */
    public void setStatus(AuditStatus status) {
        if (status != null) {
            this.status = status.getCode();
        }
    }

    /**
     * 设置风险等级（从枚举）
     *
     * @param auditRiskLevel 风险等级枚举
     */
    public void setRiskLevel(AuditRiskLevel auditRiskLevel) {
        if (auditRiskLevel != null) {
            this.riskLevel = auditRiskLevel.getCode();
        }
    }

    /**
     * 获取事件类型枚举
     *
     * @return 事件类型枚举
     */
    public AuditEventType getEventTypeEnum() {
        for (AuditEventType type : AuditEventType.values()) {
            if (type.getEventType().equals(this.eventType)
                    && type.getEventSubType().equals(this.eventSubtype)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取状态枚举
     *
     * @return 状态枚举
     */
    public AuditStatus getStatusEnum() {
        return AuditStatus.fromCode(this.status);
    }

    /**
     * 获取风险等级枚举
     *
     * @return 风险等级枚举
     */
    public AuditRiskLevel getRiskLevelEnum() {
        return AuditRiskLevel.fromCode(this.riskLevel);
    }

    /**
     * 判断是否为成功操作
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        AuditStatus statusEnum = getStatusEnum();
        return statusEnum != null && statusEnum.isSuccess();
    }

    /**
     * 判断是否为失败操作
     *
     * @return 是否失败
     */
    public boolean isFailure() {
        AuditStatus statusEnum = getStatusEnum();
        return statusEnum != null && statusEnum.isFailure();
    }

    /**
     * 判断是否为高风险操作
     *
     * @return 是否高风险
     */
    public boolean isHighRisk() {
        AuditRiskLevel auditRiskLevelEnum = getRiskLevelEnum();
        return auditRiskLevelEnum != null && auditRiskLevelEnum.isHighRisk();
    }

    /**
     * 判断是否需要告警
     *
     * @return 是否需要告警
     */
    public boolean needsAlert() {
        AuditRiskLevel auditRiskLevelEnum = getRiskLevelEnum();
        return auditRiskLevelEnum != null && auditRiskLevelEnum.needsAlert();
    }
}
