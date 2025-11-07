package io.github.rosestack.spring.boot.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.github.rosestack.crypto.enums.EncryptType;
import io.github.rosestack.spring.boot.audit.enums.AuditDetailKey;
import io.github.rosestack.spring.boot.audit.enums.AuditDetailType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计日志详情表实体类
 *
 * <p>存储审计日志的详细信息，采用 JSON 格式存储复杂数据结构。 支持敏感数据标记、加密存储、按类型分类等特性。
 * 详情数据按类型分为：HTTP请求相关、操作对象相关、数据变更相关、系统技术相关、安全相关。
 *
 * @author chensoul
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("audit_log_detail")
public class AuditLogDetail {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 审计日志ID（外键）
     */
    @NotNull(message = "审计日志ID不能为空") @TableField("audit_log_id")
    private Long auditLogId;

    /**
     * 详情类型
     */
    @NotBlank(message = "详情类型不能为空") @Size(max = 50, message = "详情类型长度不能超过50个字符") @TableField("detail_type")
    private String detailType;

    /**
     * 详情键
     */
    @NotBlank(message = "详情键不能为空") @Size(max = 50, message = "详情键长度不能超过50个字符") @TableField("detail_key")
    private String detailKey;

    /**
     * 详情值（JSON格式，可能加密脱敏）
     */
    @TableField(value = "detail_value")
    private String detailValue;

    /**
     * 是否包含敏感数据
     */
    @TableField("is_sensitive")
    private Boolean isSensitive;

    /**
     * 是否加密存储
     */
    @TableField("is_encrypted")
    private Boolean isEncrypted;

    private EncryptType encryptType;

    /**
     * 租户ID（多租户支持）
     */
    @Size(max = 50, message = "租户ID长度不能超过50个字符") @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    // ==================== 业务方法 ====================

    /**
     * 设置详情类型（从枚举）
     *
     * @param detailType 详情类型枚举
     */
    public void setDetailType(AuditDetailType detailType) {
        if (detailType != null) {
            this.detailType = detailType.getCode();
        }
    }

    /**
     * 设置详情键（从枚举）
     *
     * @param detailKey 详情键枚举
     */
    public void setDetailKey(AuditDetailKey detailKey) {
        if (detailKey != null) {
            this.detailKey = detailKey.getCode();
            this.isSensitive = detailKey.isSensitive();
        }
    }
}
