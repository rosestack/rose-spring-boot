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
package io.github.rose.syslog.aspect;

import io.github.rose.core.spring.SpringContextHolder;
import io.github.rose.syslog.annotation.SysLog;
import io.github.rose.syslog.event.SysLogEvent;
import io.github.rose.syslog.event.SysLogInfo;
import io.github.rose.syslog.util.SysLogUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 操作日志使用spring event异步入库
 */
@Aspect
public class SysLogAspect {
    private static final Logger log = LoggerFactory.getLogger(SysLogAspect.class);

    @Around("@annotation(sysLog)")
    public Object around(ProceedingJoinPoint joinPoint, SysLog sysLog) {
        String strClassName = joinPoint.getTarget().getClass().getName();
        String strMethodName = joinPoint.getSignature().getName();
        log.debug("[类名]:{},[方法]:{}", strClassName, strMethodName);

        SysLogInfo sysLogInfo = SysLogUtils.getSysLog(joinPoint, sysLog);

        long startTime = System.currentTimeMillis();
        Object result = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            sysLogInfo.setException(e.getMessage());
            sysLogInfo.setSuccess(false);
            throw new RuntimeException(e);
        } finally {
            sysLogInfo.setCostTime(System.currentTimeMillis() - startTime);
            SpringContextHolder.publishEvent(new SysLogEvent(sysLogInfo));
        }
        return result;
    }
}
