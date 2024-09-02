/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.core.aspect;

import org.apache.streampark.console.core.controller.SparkApplicationController;
import org.apache.streampark.console.core.watcher.FlinkAppHttpWatcher;
import org.apache.streampark.console.core.watcher.SparkAppHttpWatcher;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Aspect
public class AppChangeEventAspect {

    @Autowired
    private FlinkAppHttpWatcher flinkAppHttpWatcher;

    @Autowired
    private SparkAppHttpWatcher sparkAppHttpWatcher;

    @Pointcut("@annotation(org.apache.streampark.console.core.annotation.AppChangeEvent)")
    public void appChangeEventPointcut() {
    }

    @Around("appChangeEventPointcut()")
    public Object appChangeEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        log.debug("appUpdated aspect, method:{}", methodSignature.getName());
        Object target = joinPoint.proceed();
        if (joinPoint.getTarget() instanceof SparkApplicationController) {
            sparkAppHttpWatcher.init();
        } else {
            flinkAppHttpWatcher.init();
        }
        return target;
    }

}
