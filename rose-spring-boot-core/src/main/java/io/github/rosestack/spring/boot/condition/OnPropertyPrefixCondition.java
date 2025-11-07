package io.github.rosestack.spring.boot.condition;

import io.github.rosestack.spring.core.annotation.ResolvablePlaceholderAnnotationAttributes;
import io.github.rosestack.spring.core.env.PropertySourcesUtils;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static io.github.rosestack.util.ArrayUtils.arrayToString;
import static org.springframework.boot.autoconfigure.condition.ConditionOutcome.match;
import static org.springframework.boot.autoconfigure.condition.ConditionOutcome.noMatch;
import static org.springframework.core.annotation.AnnotationAttributes.fromMap;

/**
 * {@link Condition} that checks if the prefix of properties are found in environment.
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see SpringBootCondition
 * @see ConditionalOnPropertyPrefix
 * @since 1.0.0
 */
class OnPropertyPrefixCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {

        AnnotationAttributes annotationAttributes = fromMap(metadata.getAnnotationAttributes(ConditionalOnPropertyPrefix.class.getName()));

        ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();

        ResolvablePlaceholderAnnotationAttributes attributes = ResolvablePlaceholderAnnotationAttributes.of(annotationAttributes, environment);

        String[] prefixValues = attributes.getStringArray("value");

        boolean noMatched = PropertySourcesUtils.findPropertyNames(environment, propertyName -> {
            for (String prefix : prefixValues) {
                if (propertyName.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }).isEmpty();

        return noMatched ? noMatch("The prefix values " + arrayToString(prefixValues) + " were not found in Environment!") : match();
    }
}