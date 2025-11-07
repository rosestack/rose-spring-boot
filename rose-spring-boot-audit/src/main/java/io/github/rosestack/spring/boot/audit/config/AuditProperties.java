package io.github.rosestack.spring.boot.audit.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Rose Audit 配置属性
 *
 * <p>提供审计日志功能的核心配置选项，简化配置复杂度，专注于常用功能。 支持数据库存储、基本过滤和数据保留策略。
 *
 * @author chensoul
 * @since 1.0.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "rose.audit")
public class AuditProperties {

    /**
     * 是否启用审计日志功能
     */
    private boolean enabled = true;

    /**
     * 敏感字段脱敏配置
     */
    private List<String> maskFields;

    /**
     * 存储配置
     */
    @Valid @NotNull private Storage storage = new Storage();

    /**
     * 数据保留配置
     */
    @Valid @NotNull private Retention retention = new Retention();

    /**
     * 事件过滤配置
     */
    @Valid @NotNull private Filter filter = new Filter();

    /**
     * 存储配置
     */
    @Data
    public static class Storage {
        /**
         * 存储类型：目前主要支持 database
         */
        @NotBlank(message = "存储类型不能为空") private String type = "database";
    }

    /**
     * 数据保留配置
     */
    @Data
    public static class Retention {
        /**
         * 数据保留天数
         */
        @Min(value = 1, message = "数据保留天数不能小于1天") @Max(value = 3650, message = "数据保留天数不能大于10年") private int days = 365;

        /**
         * 是否启用自动清理
         */
        private boolean autoCleanup = true;

        /**
         * 清理任务执行时间（cron表达式）
         */
        @NotBlank(message = "清理任务执行时间不能为空") private String cleanupCron = "0 0 2 * * ?";
    }

    /**
     * 事件过滤配置
     */
    @Data
    public static class Filter {
        /**
         * 忽略的用户
         */
        private List<String> ignoreUsers = Arrays.asList("system", "admin");

        /**
         * 忽略的IP地址
         */
        private List<String> ignoreIps = Arrays.asList("127.0.0.1", "::1");

        /**
         * 忽略的URI模式（支持通配符）
         */
        private List<String> ignoreUriPatterns = Arrays.asList("/health/**", "/actuator/**", "/favicon.ico");

        /**
         * 最小风险等级（低于此等级的事件将被忽略）
         */
        private String minRiskLevel = "LOW";
    }
}
