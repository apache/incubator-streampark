/*
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
import org.apache.streampark.console.core.annotation.PermissionAction;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.PermissionTypeEnum;
import org.apache.streampark.console.core.enums.UserTypeEnum;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.console.core.watcher.FlinkAppHttpWatcher;
import org.apache.streampark.console.system.entity.AccessToken;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.MemberService;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@Aspect
public class ConsoleAspect {

  @Autowired private FlinkAppHttpWatcher flinkAppHttpWatcher;
  @Autowired private CommonService commonService;
  @Autowired private MemberService memberService;
  @Autowired private ApplicationManageService applicationManageService;

  @Pointcut(
      "execution(public"
          + " org.apache.streampark.console.base.domain.RestResponse"
          + " org.apache.streampark.console.*.controller.*.*(..))")
  public void apiAccess() {}

  @SuppressWarnings("checkstyle:SimplifyBooleanExpression")
  @Around(value = "apiAccess()")
  public RestResponse apiAccess(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    if (log.isDebugEnabled()) {
      log.debug("restResponse aspect, method:{}", methodSignature.getName());
    }
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
    if (log.isDebugEnabled()) {
      log.debug("appUpdated aspect, method:{}", methodSignature.getName());
    }
    Object target = joinPoint.proceed();
    flinkAppHttpWatcher.init();
    return target;
  }

  @Pointcut("@annotation(org.apache.streampark.console.core.annotation.PermissionAction)")
  public void permissionAction() {}

  @Around("permissionAction()")
  public RestResponse permissionAction(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    PermissionAction permissionAction =
        methodSignature.getMethod().getAnnotation(PermissionAction.class);

    User currentUser = commonService.getCurrentUser();
    ApiAlertException.throwIfNull(currentUser, "Permission denied, please login first.");

    boolean isAdmin = currentUser.getUserType() == UserTypeEnum.ADMIN;

    if (!isAdmin) {
      PermissionTypeEnum permissionTypeEnum = permissionAction.type();
      Long paramId = getParamId(joinPoint, methodSignature, permissionAction.id());

      switch (permissionTypeEnum) {
        case USER:
          ApiAlertException.throwIfTrue(
              !currentUser.getUserId().equals(paramId),
              "Permission denied, only user himself can access this permission");
          break;
        case TEAM:
          ApiAlertException.throwIfTrue(
              memberService.getByTeamIdUserName(paramId, currentUser.getUsername()) == null,
              "Permission denied, only user belongs to this team can access this permission");
          break;
        case APP:
          Application app = applicationManageService.getById(paramId);
          ApiAlertException.throwIfTrue(app == null, "Invalid operation, application is null");
          ApiAlertException.throwIfTrue(
              memberService.getByTeamIdUserName(app.getTeamId(), currentUser.getUsername()) == null,
              "Permission denied, only user belongs to this team can access this permission");
          break;
        default:
          throw new IllegalArgumentException(
              String.format("Permission type %s is not supported.", permissionTypeEnum));
      }
    }

    return (RestResponse) joinPoint.proceed();
  }

  private Long getParamId(
      ProceedingJoinPoint joinPoint, MethodSignature methodSignature, String spELString) {
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression(spELString);
    EvaluationContext context = new StandardEvaluationContext();
    Object[] args = joinPoint.getArgs();
    DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
    String[] parameterNames = discoverer.getParameterNames(methodSignature.getMethod());
    for (int i = 0; i < parameterNames.length; i++) {
      context.setVariable(parameterNames[i], args[i]);
    }
    Object value = expression.getValue(context);

    if (value == null || StringUtils.isBlank(value.toString())) {
      return null;
    }

    try {
      return Long.parseLong(value.toString());
    } catch (NumberFormatException e) {
      throw new ApiAlertException(
          "Wrong use of annotation on method " + methodSignature.getName(), e);
    }
  }
}
