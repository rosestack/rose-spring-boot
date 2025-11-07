# rose-xxl-job-spring-boot-starter

基于 XXL-Job 的 Spring Boot Starter，提供自动配置、属性绑定、任务自动注册（可选）与运行时日志/指标增强。

## 依赖坐标

Maven：
```xml
<dependency>
  <groupId>io.github.rosestack</groupId>
  <artifactId>rose-xxl-job-spring-boot-starter</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 快速开始

1) 配置 application.yaml（最小化配置）
```yaml
rose:
  xxl-job:
    enabled: true
    admin-addresses: "http://127.0.0.1:8080/xxl-job-admin"
    appname: demo-app
    # 可选：
    # port: 9999
    # access-token: ""
    # log-path: logs/xxl-job/
    # log-retention-days: 30

    # 运行时指标（可选）
    metrics:
      enabled: false

    # 管理端客户端（可选，用于自动注册/更新任务元数据）
    client:
      enabled: false
      # username: admin
      # password: 123456
      # login-path: /login
      # username-param: userName
      # password-param: password
      # cookie-name: Cookie
      # defaults:
      #   author: admin
      #   alarm-email: ""
      #   schedule-type: CRON
      #   glue-type: BEAN
      #   executor-route-strategy: ROUND
      #   misfire-strategy: DO_NOTHING
      #   executor-block-strategy: SERIAL_EXECUTION
      #   executor-timeout: 0
      #   executor-fail-retry-count: 0
```

2) 在同一方法上同时标注 @XxlJob 与 @XxlJobRegister（推荐写法）
```java
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.rosestack.spring.boot.xxljob.client.annotation.XxlJobRegister;
import org.springframework.stereotype.Component;

@Component
public class DemoJob {

    @XxlJob("demoJobHandler")
    @XxlJobRegister(
        cron = "0 */1 * * * ?",
        jobDesc = "demo job",
        author = "your-name"
    )
    public void demoJob() {
        // TODO 业务逻辑
    }
}
```
- @XxlJob 用于让执行器识别任务入口；
- @XxlJobRegister 用于将任务元数据（cron、描述、作者等）注册/更新到调度中心；
- 两个注解必须同时标在同一个方法上，缺一会导致发现或注册不完整。

## 运行时日志约定

Starter 统一了关键日志的事件名与键值顺序（示例）：
- xxl-job.executor.config: appName=..., adminAddresses=...
- xxl-job.registered / xxl-job.updated / xxl-job.deleted
- xxl-job.started / xxl-job.succeeded / xxl-job.failed（失败附带异常堆栈）

你可以据此在日志平台中建立检索与告警规则。

## 可选能力与依赖提示

- 指标采集（metrics.enabled=true）：
  - 需要在应用中引入 `spring-boot-starter-aop` 以启用切面。
- 管理端客户端（client.enabled=true）：
  - 用于通过 HTTP 调用 admin 做任务的注册/更新/启停等；若你的应用未显式引入 Web 组件，请确保存在 `spring-web` 依赖。

> 以上可选依赖在本 Starter 中声明为 optional，按需在你的应用引入即可。

## 常见问题

- 只标注了 @XxlJobRegister 没有 @XxlJob：执行器无法发现该方法为任务入口。
- 只标注了 @XxlJob 没有 @XxlJobRegister：任务不会自动在 admin 侧创建/更新，需要手工维护。
- 登录失败（client.enabled=true）：请核对 admin 版本的登录路径及参数名（login-path/username-param/password-param）。

## 许可证

Apache-2.0