/*
 * Copyright © 2025 rosestack.github.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.rose.syslog.util;

import io.github.rose.core.json.JsonUtils;
import io.github.rose.core.spring.WebUtils;
import io.github.rose.core.util.NetUtils;
import io.github.rose.syslog.annotation.SysLog;
import io.github.rose.syslog.annotation.SysLogIgnore;
import io.github.rose.syslog.event.SysLogInfo;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

public class SysLogUtils {
    private static final Logger log = LoggerFactory.getLogger(SysLogUtils.class);

    public static SysLogInfo getSysLog(ProceedingJoinPoint joinPoint, SysLog sysLog) {
        SysLogInfo sysLogInfo = new SysLogInfo();
        sysLogInfo.setName(getSysLogValue(joinPoint, sysLog));
        sysLogInfo.setSuccess(true);
        sysLogInfo.setServerIp(NetUtils.getLocalAddress());
        sysLogInfo.setCreatedBy(WebUtils.getUsername());
        sysLogInfo.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()));

        Optional<HttpServletRequest> requestOptional = WebUtils.ofRequest();
        if (requestOptional.isPresent()) {
            HttpServletRequest request = requestOptional.get();
            sysLogInfo.setRequestUrl(WebUtils.constructUrl(request));
            sysLogInfo.setRequestMethod(request.getMethod());
            sysLogInfo.setUserAgent(WebUtils.getUserAgent(request));
            sysLogInfo.setClientIp(WebUtils.getClientIp(request));

            if (HttpMethod.PUT.name().equals(sysLogInfo.getRequestMethod())
                    || HttpMethod.POST.name().equals(sysLogInfo.getRequestMethod())) {
                sysLogInfo.setRequestParams(JsonUtils.toJson(dealArgs(joinPoint.getArgs())));
            } else {
                sysLogInfo.setRequestParams(JsonUtils.toJson(request.getParameterMap()));
            }
        }
        return sysLogInfo;
    }

    private static String getSysLogValue(ProceedingJoinPoint joinPoint, SysLog sysLog) {
        String value = sysLog.value();
        String expression = sysLog.expression();

        if (StringUtils.isNotBlank(expression)) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            EvaluationContext context = getContext(joinPoint.getArgs(), signature.getMethod());
            try {
                value = getValue(context, expression, String.class);
            } catch (Exception e) {
                log.error("@SysLog 解析 spel {} 异常", expression);
            }
        }
        return value;
    }

    private static List<Object> dealArgs(Object[] args) {
        if (args == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(args).filter(t -> !isFilterObject(t)).collect(Collectors.toList());
    }

    @SuppressWarnings("rawtypes")
    private static boolean isFilterObject(Object o) {
        if (Objects.isNull(o)
                || o.getClass().isAnnotationPresent(SysLogIgnore.class)
                || o.getClass().isAnnotationPresent(PathVariable.class)) {
            return true;
        }

        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection = (Collection) o;
            for (Object value : collection) {
                return value instanceof MultipartFile;
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map map = (Map) o;
            for (Object value : map.entrySet()) {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile
                || o instanceof HttpServletRequest
                || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }

    /**
     * 获取spel 定义的参数值
     *
     * @param context 参数容器
     * @param key     key
     * @param clazz   需要返回的类型
     * @param <T>     返回泛型
     * @return 参数值
     */
    private static <T> T getValue(EvaluationContext context, String key, Class<T> clazz) {
        SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
        Expression expression = spelExpressionParser.parseExpression(key);
        return expression.getValue(context, clazz);
    }

    /**
     * 获取参数容器
     *
     * @param arguments       方法的参数列表
     * @param signatureMethod 被执行的方法体
     * @return 装载参数的容器
     */
    private static EvaluationContext getContext(Object[] arguments, Method signatureMethod) {
        String[] parameterNames = new StandardReflectionParameterNameDiscoverer().getParameterNames(signatureMethod);
        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames == null) {
            return context;
        }
        for (int i = 0; i < arguments.length; i++) {
            context.setVariable(parameterNames[i], arguments[i]);
        }
        return context;
    }
}
