package io.github.rosestack.spring.boot.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link Conditional} that checks if the prefix of properties are found in environment..
 *
 * <h3>Example Usage</h3>
 * <h4>Single prefix</h4>
 * <pre>{@code
 * @ConditionalOnPropertyPrefix("myapp.config")
 * public class MyConfig {
 *     // This bean will only be loaded if any property with prefix "myapp.config." exists
 * }
 * }</pre>
 *
 * <h4>Multiple prefixes</h4>
 * <pre>{@code
 * @ConditionalOnPropertyPrefix( {"feature.alpha", "feature.beta"} )
 * public class FeatureConfig {
 *     // Loaded if any property with prefix "feature.alpha." or "feature.beta." exists
 * }
 * }</pre>
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see OnPropertyPrefixCondition
 * @since 1.0.0
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Conditional(OnPropertyPrefixCondition.class)
public @interface ConditionalOnPropertyPrefix {

    /**
     * The prefix values of properties.
     * <p>
     * The prefix automatically ends
     * with a dot if not specified.
     *
     * @return prefix values of properties.
     */
    String[] value();

}