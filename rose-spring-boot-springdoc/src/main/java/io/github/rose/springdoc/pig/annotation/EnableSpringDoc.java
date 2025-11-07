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
package io.github.rose.springdoc.pig.annotation;

import io.github.rose.springdoc.pig.config.OpenAPIDefinitionImportSelector;
import io.github.rose.springdoc.pig.config.SwaggerProperties;
import java.lang.annotation.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@EnableConfigurationProperties(SwaggerProperties.class)
@Import(OpenAPIDefinitionImportSelector.class)
public @interface EnableSpringDoc {

    /**
     * 网关路由前缀
     *
     * @return String
     */
    String value();

    /**
     * 是否是微服务架构
     *
     * @return true
     */
    boolean isMicro() default true;
}
