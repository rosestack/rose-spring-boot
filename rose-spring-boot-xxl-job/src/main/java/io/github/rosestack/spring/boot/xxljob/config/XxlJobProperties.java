package io.github.rosestack.spring.boot.xxljob.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * XXL-Job 配置属性
 */
@Data
@Validated
@ConfigurationProperties(prefix = "rose.xxl-job")
public class XxlJobProperties {

    /**
     * 是否启用 XXL-Job 执行器
     */
    private boolean enabled = true;

    /**
     * 调度中心地址，如：http://127.0.0.1:8080/xxl-job-admin
     */
    @NotBlank(message = "管理端地址不能为空") @Pattern(regexp = "^https?://.*", message = "管理端地址必须以 http:// 或 https:// 开头") private String adminAddresses;

    /**
     * 执行器 AppName，对应调度中心的执行器标识；为空则回退为 spring.application.name
     */
    private String appname;

    /**
     * 执行器端口，默认 9999
     */
    @NotNull(message = "端口不能为空") @Min(value = 1024, message = "端口号必须大于等于 1024") @Max(value = 65535, message = "端口号必须小于等于 65535") private Integer port = 9999;

    /**
     * 执行器通讯 TOKEN，需要与调度中心保持一致
     */
    private String accessToken;

    /**
     * 日志路径，默认 logs/xxl-job/
     */
    private String logPath = "logs/xxl-job/";

    /**
     * 日志保存天数，-1 表示永久
     */
    @Min(value = -1, message = "日志保留天数必须大于等于 -1") private int logRetentionDays = 30;

    private Metrics metrics = new Metrics();

    @Valid private Client client = new Client();

    @Data
    public static class Metrics {
        private boolean enabled = false;
    }

    @Data
    public static class Client {
        private boolean enabled;

        /**
         * 管理登录用户名
         */
        private String username;
        /**
         * 管理登录密码（可选）
         */
        private String password;

        /**
         * 登录路径（默认 /login，不同版本可能不同）
         */
        private String loginPath = "/login";

        /**
         * 用户名参数名（默认 userName）
         */
        private String usernameParam = "userName";
        /**
         * 密码参数名（默认 password）
         */
        private String passwordParam = "password";
        /**
         * 优先匹配的 Cookie 名
         */
        private String cookieName = "Cookie";

        private Defaults defaults = new Defaults();

        /**
         * 验证用户名和密码的一致性
         */
        @AssertTrue(message = "如果配置了用户名，则必须配置密码；如果配置了密码，则必须配置用户名") public boolean isCredentialsValid() {
            boolean hasUsername = username != null && !username.trim().isEmpty();
            boolean hasPassword = password != null && !password.trim().isEmpty();
            // 要么都有，要么都没有
            return hasUsername == hasPassword;
        }

        @Data
        public static class Defaults {
            private String author = "admin";

            private String alarmEmail;

            private String scheduleType = "CRON";

            private String glueType = "BEAN";

            private String executorRouteStrategy = "ROUND";

            private String misfireStrategy = "DO_NOTHING";

            private String executorBlockStrategy = "SERIAL_EXECUTION";

            private int executorTimeout = 0;

            private int executorFailRetryCount = 0;
        }
    }
}
