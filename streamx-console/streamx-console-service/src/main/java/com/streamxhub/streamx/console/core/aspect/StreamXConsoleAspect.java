/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.console.core.aspect;

import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.streamxhub.streamx.common.util.ExceptionUtils;
import com.streamxhub.streamx.console.base.domain.RestResponse;

/**
 * @author benjobs
 */
@Slf4j
@Component
@Aspect
public class StreamXConsoleAspect {

    @Pointcut("execution(public" +
            " com.streamxhub.streamx.console.base.domain.RestResponse" +
            " com.streamxhub.streamx.console.core.controller.*.*(..))"
    )
    public void response() {
    }

    @Around(value = "response()")
    public RestResponse response(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        log.info("restResponse aspect, method:{}", methodSignature.getName());
        RestResponse response;
        try {
            response = (RestResponse) joinPoint.proceed();
            response.put("status", "success");
        } catch (Throwable e) {
            e.printStackTrace();
            response = Objects.requireNonNull(RestResponse.create()
                    .put("status", "error"))
                    .put("exception", ExceptionUtils.stringifyException(e));
        }
        return response;
    }
}
