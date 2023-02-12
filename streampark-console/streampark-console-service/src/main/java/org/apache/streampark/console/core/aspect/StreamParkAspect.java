/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.core.aspect;

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.annotation.ApiAccess;
import org.apache.streampark.console.core.task.FlinkRESTAPIWatcher;
import org.apache.streampark.console.system.entity.AccessToken;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@Aspect
public class StreamParkAspect {

  @Autowired private FlinkRESTAPIWatcher flinkRESTAPIWatcher;

  @Pointcut(
      "execution(public"
          + " org.apache.streampark.console.base.domain.RestResponse"
          + " org.apache.streampark.console.*.controller.*.*(..))")
  public void apiAccess() {}

  @SuppressWarnings("checkstyle:SimplifyBooleanExpression")
  @Around(value = "apiAccess()")
  public RestResponse apiAccess(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    log.debug("restResponse aspect, method:{}", methodSignature.getName());
    Boolean isApi =
        (Boolean) SecurityUtils.getSubject().getSession().getAttribute(AccessToken.IS_API_TOKEN);
    if (Objects.nonNull(isApi) && isApi) {
      ApiAccess apiAccess = methodSignature.getMethod().getAnnotation(ApiAccess.class);
      if (Objects.isNull(apiAccess) || !apiAccess.value()) {
        throw new ApiAlertException("api accessToken authentication failed!");
      }
    }
    return (RestResponse) joinPoint.proceed();
  }

  @Pointcut("@annotation(org.apache.streampark.console.core.annotation.AppUpdated)")
  public void appUpdated() {}

  @Around("appUpdated()")
  public Object appUpdated(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    log.debug("appUpdated aspect, method:{}", methodSignature.getName());
    Object target = joinPoint.proceed();
    flinkRESTAPIWatcher.init();
    return target;
  }
}
