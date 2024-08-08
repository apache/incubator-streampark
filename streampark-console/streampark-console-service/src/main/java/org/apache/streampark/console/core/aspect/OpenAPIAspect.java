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

import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.common.util.ReflectUtils;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.annotation.OpenAPI;
import org.apache.streampark.console.system.entity.AccessToken;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
@Component
@Aspect
public class OpenAPIAspect {

    @Pointcut("execution(public"
        + " org.apache.streampark.console.base.domain.RestResponse"
        + " org.apache.streampark.console.core.controller.*.*(..))")
    public void openAPIPointcut() {
    }

    @SuppressWarnings("checkstyle:SimplifyBooleanExpression")
    @Around(value = "openAPIPointcut()")
    public RestResponse openAPI(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        log.debug("restResponse aspect, method:{}", methodSignature.getName());
        Boolean isApi = (Boolean) SecurityUtils.getSubject().getSession().getAttribute(AccessToken.IS_API_TOKEN);
        if (isApi != null && isApi) {
            HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            OpenAPI openAPI = methodSignature.getMethod().getAnnotation(OpenAPI.class);
            if (openAPI == null) {
                String url = request.getRequestURI();
                throw new ApiAlertException("openapi unsupported: " + url);
            } else {
                Object[] objects = joinPoint.getArgs();
                for (OpenAPI.Param param : openAPI.param()) {
                    String bingFor = param.bindFor();
                    if (StringUtils.isNotBlank(bingFor)) {
                        String name = param.name();
                        for (Object args : objects) {
                            Field bindForField = ReflectUtils.getField(args.getClass(), bingFor);
                            if (bindForField != null) {
                                Object value = request.getParameter(name);
                                bindForField.setAccessible(true);
                                if (value != null) {
                                    if (param.type().equals(String.class)) {
                                        bindForField.set(args, value.toString());
                                    } else if (param.type().equals(Boolean.class)
                                        || param.type().equals(boolean.class)) {
                                        bindForField.set(args, Boolean.parseBoolean(value.toString()));
                                    } else if (param.type().equals(Integer.class) || param.type().equals(int.class)) {
                                        bindForField.set(args, Integer.parseInt(value.toString()));
                                    } else if (param.type().equals(Long.class) || param.type().equals(long.class)) {
                                        bindForField.set(args, Long.parseLong(value.toString()));
                                    } else if (param.type().equals(Date.class)) {
                                        bindForField.set(args, DateUtils.parse(value.toString(), DateUtils.fullFormat(),
                                            TimeZone.getDefault()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return (RestResponse) joinPoint.proceed();
    }
}
