package io.github.rosestack.spring.boot.audit.annotation;

import io.github.rosestack.crypto.enums.EncryptType;
import io.github.rosestack.spring.boot.audit.enums.AuditEventType;
import io.github.rosestack.spring.boot.audit.enums.AuditRiskLevel;
import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {

    String value() default "";

    AuditEventType eventType() default AuditEventType.DATA_OTHER;

    AuditRiskLevel riskLevel() default AuditRiskLevel.LOW;

    String condition() default "";

    boolean recordParams() default true;

    boolean recordException() default true;

    boolean recordReturnValue() default false;

    String[] maskFields() default {};

    EncryptType encryptType() default EncryptType.AES;
}
