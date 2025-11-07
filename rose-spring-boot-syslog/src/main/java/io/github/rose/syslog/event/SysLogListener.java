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
package io.github.rose.syslog.event;

import io.github.rose.core.function.CheckedConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;

/**
 * 异步监听日志事件
 */
public class SysLogListener {
    private static final Logger log = LoggerFactory.getLogger(SysLogListener.class);

    private final CheckedConsumer<SysLogInfo> consumer;

    public SysLogListener(CheckedConsumer<SysLogInfo> consumer) {
        this.consumer = consumer;
    }

    @Async
    @Order
    @EventListener(SysLogEvent.class)
    public void saveLog(SysLogEvent sysLogEvent) {
        try {
            consumer.accept(sysLogEvent.getSource());
        } catch (Throwable e) {
            log.error("保存日志失败", e);
        }
    }
}
