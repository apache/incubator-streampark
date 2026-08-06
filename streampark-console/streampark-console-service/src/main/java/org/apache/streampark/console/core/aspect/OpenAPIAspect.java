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

    @Pointcut("@annotation(org.apache.streampark.console.core.annotation.OpenAPI)")
    public void openAPIPointcut() {
    }

    @SuppressWarnings("checkstyle:SimplifyBooleanExpression")
    @Around(value = "openAPIPointcut()")
    public Object openAPI(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        log.debug("restResponse aspect, method:{}", methodSignature.getName());
        Boolean isApi = (Boolean) SecurityUtils.getSubject().getSession().getAttribute(AccessToken.IS_API_TOKEN);
        if (isApi != null && isApi) {
            HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            OpenAPI openAPI = methodSignature.getMethod().getAnnotation(OpenAPI.class);
            if (openAPI == null) {
                throw new ApiAlertException("openapi unsupported: " + request.getRequestURI());
            }
            bindOpenApiParameters(request, openAPI, joinPoint.getArgs());
        }
        return joinPoint.proceed();
    }

    private void bindOpenApiParameters(HttpServletRequest request, OpenAPI openAPI,
                                       Object[] args) throws Exception {
        for (OpenAPI.Param param : openAPI.param()) {
            bindOpenApiParameter(request, param, args);
        }
    }

    private void bindOpenApiParameter(HttpServletRequest request, OpenAPI.Param param,
                                      Object[] args) throws Exception {
        String bindFor = param.bindFor();
        if (StringUtils.isBlank(bindFor)) {
            return;
        }
        String name = param.name();
        for (Object arg : args) {
            Field bindForField = ReflectUtils.getField(arg.getClass(), bindFor);
            if (bindForField == null) {
                continue;
            }
            String value = request.getParameter(name);
            if (value == null) {
                continue;
            }
            bindForField.setAccessible(true);
            bindForField.set(arg, convertParameterValue(param, value));
        }
    }

    private Object convertParameterValue(OpenAPI.Param param, String value) throws Exception {
        Class<?> type = param.type();
        if (type.equals(String.class)) {
            return value;
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        if (type.equals(Integer.class) || type.equals(int.class)) {
            return Integer.parseInt(value);
        }
        if (type.equals(Long.class) || type.equals(long.class)) {
            return Long.parseLong(value);
        }
        if (type.equals(Date.class)) {
            return DateUtils.parse(value, DateUtils.fullFormat(), TimeZone.getDefault());
        }
        return value;
    }
}
