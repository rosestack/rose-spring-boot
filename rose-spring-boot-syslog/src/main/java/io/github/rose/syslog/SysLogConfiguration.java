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
package io.github.rose.syslog;

import io.github.rose.syslog.aspect.SysLogAspect;
import io.github.rose.syslog.event.SysLogListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 日志自动配置
 */
@EnableAsync
@Configuration
@ConditionalOnWebApplication
public class SysLogConfiguration {
    public static final Logger log = LoggerFactory.getLogger(SysLogConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public SysLogListener sysLogListener() {
        return new SysLogListener(sysLogInfo -> {
            log.info("sysLogInfo: {}", sysLogInfo);
        });
    }

    @Bean
    public SysLogAspect sysLogAspect() {
        log.info("Initializing SysLogAspect");
        return new SysLogAspect();
    }
}
