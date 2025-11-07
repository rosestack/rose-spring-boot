package io.github.rosestack.spring.boot.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Web 配置属性
 *
 * <p>提供 Web 相关的配置选项
 *
 * @author rosestack
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "rose.web")
public class WebProperties {

    /**
     * 是否启用 Web 自动配置
     */
    private boolean enabled = true;

    /**
     * 过滤器配置
     */
    private Filter filter = new Filter();

    /**
     * Swagger 配置
     */
    private Springdoc springdoc = new Springdoc();

    /**
     * 过滤器配置
     */
    @Data
    public static class Filter {
        private String[] excludePaths;

        /**
         * 请求ID过滤器
         */
        private Trace trace = new Trace();

        /**
         * XSS 过滤器
         */
        private Xss xss = new Xss();

        private CachingRequest caching = new CachingRequest();

        private LoggingRequest logging = new LoggingRequest();

        @Data
        public static class LoggingRequest {
            private boolean enabled = true;

            private int maxResponseTimeToLogInMs = 1500;
        }

        @Data
        public static class CachingRequest {
            /**
             * 是否启用
             */
            private boolean enabled = true;
        }

        /**
         * 请求ID过滤器配置
         */
        @Data
        public static class Trace {
            /**
             * 是否启用
             */
            private boolean enabled = true;
        }

        /**
         * XSS 过滤器配置
         */
        @Data
        public static class Xss {
            /**
             * 是否启用
             */
            private boolean enabled = true;
        }
    }

    /**
     * Swagger 配置
     */
    @Data
    public static class Springdoc {
        /**
         * API 标题
         */
        private String title = "Rose API Documentation";

        /**
         * API 描述
         */
        private String description = "Rose 框架 API 文档";

        /**
         * API 版本
         */
        private String version = "1.0.0";

        /**
         * 联系人信息
         */
        private Contact contact = new Contact();

        /**
         * 许可证信息
         */
        private License license = new License();

        /**
         * 服务器列表
         */
        private java.util.List<Server> servers = new java.util.ArrayList<>();

        /**
         * 安全配置
         */
        private Security security = new Security();

        /**
         * API 分组配置
         */
        private Groups groups = new Groups();

        /**
         * 联系人信息
         */
        @Data
        public static class Contact {
            private String name = "Rose Team";
            private String email = "support@rosestack.io";
            private String url = "https://rosestack.io";
        }

        /**
         * 许可证信息
         */
        @Data
        public static class License {
            private String name = "Apache 2.0";
            private String url = "https://www.apache.org/licenses/LICENSE-2.0";
        }

        /**
         * 服务器信息
         */
        @Data
        public static class Server {
            private String url = "http://localhost:8080";
            private String description = "开发环境";
        }

        /**
         * 安全配置
         */
        @Data
        public static class Security {
            /**
             * 是否启用安全认证
             */
            private boolean enabled = false;

            /**
             * JWT Token 配置
             */
            private Jwt jwt = new Jwt();

            /**
             * OAuth2 配置
             */
            private Oauth2 oauth2 = new Oauth2();

            /**
             * API Key 配置
             */
            private ApiKey apiKey = new ApiKey();

            /**
             * JWT Token 配置
             */
            @Data
            public static class Jwt {
                private boolean enabled = true;
            }

            /**
             * OAuth2 配置
             */
            @Data
            public static class Oauth2 {
                private boolean enabled = false;
                private AuthorizationCode authorizationCode = new AuthorizationCode();
                private ClientCredentials clientCredentials = new ClientCredentials();

                @Data
                public static class AuthorizationCode {
                    private boolean enabled = true;
                    private String authorizationUrl = "http://localhost:8080/oauth2/authorize";
                    private String tokenUrl = "http://localhost:8080/oauth2/token";
                    private String refreshUrl = "http://localhost:8080/oauth2/token";
                }

                @Data
                public static class ClientCredentials {
                    private boolean enabled = false;
                    private String tokenUrl = "http://localhost:8080/oauth2/token";
                    private String refreshUrl = "http://localhost:8080/oauth2/token";
                }
            }

            /**
             * API Key 配置
             */
            @Data
            public static class ApiKey {
                private boolean enabled = false;
                private String name = "X-API-Key";
                private String in = "header"; // header, query, cookie
            }
        }

        /**
         * API 分组配置
         */
        @Data
        public static class Groups {
            private Group system = new Group();
            private Group business = new Group();
            private Group publicApi = new Group();
            private Group internal = new Group();
            private Group actuator = new Group();

            @Data
            public static class Group {
                private boolean enabled = true;
            }
        }
    }
}
