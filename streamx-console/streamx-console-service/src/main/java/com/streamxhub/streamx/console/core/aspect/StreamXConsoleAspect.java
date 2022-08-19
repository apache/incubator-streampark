/*
 * Copyright 2019 The StreamX Project
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

package com.streamxhub.streamx.console.core.aspect;

import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.task.FlinkTrackingTask;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author benjobs
 */
@Slf4j
@Component
@Aspect
public class StreamXConsoleAspect {

    @Pointcut("@annotation(com.streamxhub.streamx.console.core.annotation.RefreshCache)")
    public void refreshCache() {
    }

    @Around("refreshCache()")
    public Object refreshCache(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        log.debug("refreshCache aspect, method:{}", methodSignature.getName());
        Object[] args = joinPoint.getArgs();
        Object param = args[0];
        Long appId;
        if (param instanceof Application) {
            appId = ((Application) param).getId();
        } else {
            Method method = param.getClass().getDeclaredMethod("getAppId");
            method.setAccessible(true);
            appId = (Long) method.invoke(param, null);
        }
        return FlinkTrackingTask.refreshTracking(appId, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            return null;
        });
    }

}
