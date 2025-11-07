package io.github.rosestack.spring.boot.audit.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@Slf4j
public class AuditEventConditionEvaluator {
    private static final ExpressionParser expressionParser = new SpelExpressionParser();

    public static boolean evaluate(ProceedingJoinPoint joinPoint, String condition, Object result) {
        if (StringUtils.isBlank(condition)) {
            return true;
        }

        try {
            Expression expression = expressionParser.parseExpression(condition);
            EvaluationContext context = new StandardEvaluationContext();

            // 设置方法参数
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            // 设置返回值
            if (result != null) {
                context.setVariable("result", result);
            }

            return Boolean.TRUE.equals(expression.getValue(context, Boolean.class));
        } catch (Exception e) {
            log.warn("评估条件表达式失败: {}, 条件: {}", e.getMessage(), condition);
            return true; // 默认记录
        }
    }
}
