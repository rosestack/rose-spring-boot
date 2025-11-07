package io.github.rosestack.spring.boot.xxljob.client;

import io.github.rosestack.spring.boot.xxljob.config.XxlJobProperties;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 简易认证拦截器：
 * - 若配置了 username/password，拦截首个请求，先尝试登录以获取 Cookie（JSESSIONID），后续请求自动携带
 * - 鉴于不同 Admin 版本的登录端点/参数可能不同，这里采用最通用的 /login POST + form 约定，需按实际端点调整。
 */
@Slf4j
@RequiredArgsConstructor
public class XxlJobClientAuthInterceptor implements ClientHttpRequestInterceptor {

    private final XxlJobProperties props;
    private final AtomicReference<String> cookieRef = new AtomicReference<>(null);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String cookie = cookieRef.get();
        if (cookie != null) {
            request.getHeaders().add(props.getClient().getCookieName(), cookie);
            return execution.execute(request, body);
        }
        // 如果未设置用户名密码，则直接透传
        if (props.getClient().getUsername() == null || props.getClient().getPassword() == null) {
            return execution.execute(request, body);
        }
        // 进行一次登录
        try {
            RestTemplate tmp = new RestTemplate();
            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add(props.getClient().getUsernameParam(), props.getClient().getUsername());
            form.add(props.getClient().getPasswordParam(), props.getClient().getPassword());
            RequestEntity<LinkedMultiValueMap<String, String>> req = RequestEntity.post(java.net.URI.create(
                            extractBaseUrl(request.getURI()) + props.getClient().getLoginPath()))
                    .body(form);
            ResponseEntity<String> resp = tmp.exchange(req, String.class);
            List<String> setCookies = resp.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies != null && !setCookies.isEmpty()) {
                // 简化处理：取第一条 Set-Cookie
                String c = setCookies.get(0);
                cookieRef.set(c);
                request.getHeaders().add(props.getClient().getCookieName(), c);
            }
        } catch (Exception e) {
            // 登录失败则继续按原请求执行，可能由网关/外部代理注入鉴权
            // 但记录警告日志以便排查问题
            log.warn("XXL-Job 自动登录失败，将尝试无认证访问: {}", e.getMessage());
        }
        return execution.execute(request, body);
    }

    private static String extractBaseUrl(java.net.URI uri) {
        String base = uri.getScheme() + "://" + uri.getHost();
        if (uri.getPort() > 0) base += ":" + uri.getPort();
        return base;
    }
}
