package io.github.rosestack.spring.boot.xxljob.client.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface XxlJobRegister {

    String cron();

    String jobDesc() default "";

    String author() default "";

    String alarmEmail() default "";

    String executorRouteStrategy() default "ROUND";

    boolean autoStart() default false;

    String executorParam() default "";
}
