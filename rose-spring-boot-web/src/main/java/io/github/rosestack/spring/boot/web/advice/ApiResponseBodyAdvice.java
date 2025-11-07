package io.github.rosestack.spring.boot.web.advice;

import io.github.rosestack.core.util.ApiResponse;
import io.github.rosestack.spring.annotation.ResponseIgnore;
import io.github.rosestack.spring.filter.AbstractRequestFilter;
import io.github.rosestack.spring.util.ServletUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体包装器
 *
 * <p>自动将控制器返回的数据包装为统一的 ApiResponse 格式，支持基于 URL 的排除路径
 *
 * @author rosestack
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !returnType.hasMethodAnnotation(ResponseIgnore.class)
                && !returnType.getContainingClass().isAnnotationPresent(ResponseIgnore.class)
                && !ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        // 检查请求路径是否应该被排除
        String requestPath = ServletUtils.extractPathFromUri(request.getURI().getPath());
        if (AbstractRequestFilter.shouldExcludePath(requestPath)) {
            log.debug("ApiResponseBodyAdvice 跳过包装路径: {}", requestPath);
            return body;
        }

        // 如果返回值为 null，包装为成功响应
        if (body == null) {
            return ApiResponse.ok();
        }

        // 如果返回值已经是 ApiResponse 类型，直接返回
        if (body instanceof ApiResponse) {
            return body;
        }

        // 包装为成功响应
        ApiResponse<Object> apiResponse = ApiResponse.ok(body);

        log.debug("响应体包装完成，路径: {}, 数据类型: {}", requestPath, body.getClass().getSimpleName());

        return apiResponse;
    }
}
