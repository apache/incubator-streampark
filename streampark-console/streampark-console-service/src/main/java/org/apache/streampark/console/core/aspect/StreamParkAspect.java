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
import org.apache.streampark.console.core.annotation.PermissionScope;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.UserType;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.ServiceHelper;
import org.apache.streampark.console.core.task.FlinkAppHttpWatcher;
import org.apache.streampark.console.system.entity.AccessToken;
import org.apache.streampark.console.system.entity.Member;
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
import org.springframework.core.SpringProperties;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

@Slf4j
@Component
@Aspect
public class StreamParkAspect {

  @Autowired private FlinkAppHttpWatcher flinkAppHttpWatcher;
  @Autowired private ServiceHelper serviceHelper;
  @Autowired private MemberService memberService;
  @Autowired private ApplicationService applicationService;

  private final Set<String> openapiWhitelist = new HashSet<>();

  @PostConstruct
  public void initOpenapiWhitelist() {
    String whiteLists = SpringProperties.getProperty("streampark.openapi.white-list");
    if (StringUtils.isNotBlank(whiteLists)) {
      String[] whiteList = whiteLists.trim().split("\\s|,");
      for (String order : whiteList) {
        if (StringUtils.isNotBlank(order)) {
          if (!order.startsWith("/")) {
            order = "/" + order;
          }
          openapiWhitelist.add(order);
        }
      }
    }
  }

  @Pointcut(
      "execution(public"
          + " org.apache.streampark.console.base.domain.RestResponse"
          + " org.apache.streampark.console.core.controller.*.*(..))")
  public void openAPIPointcut() {}

  @SuppressWarnings("checkstyle:SimplifyBooleanExpression")
  @Around(value = "openAPIPointcut()")
  public RestResponse openAPI(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    log.debug("restResponse aspect, method:{}", methodSignature.getName());
    Boolean isApi =
        (Boolean) SecurityUtils.getSubject().getSession().getAttribute(AccessToken.IS_API_TOKEN);
    if (isApi != null && isApi) {
      HttpServletRequest request =
          ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
      OpenAPI openAPI = methodSignature.getMethod().getAnnotation(OpenAPI.class);
      if (openAPI == null) {
        String url = request.getRequestURI();
        if (openapiWhitelist.contains(url)) {
          log.info("request by openapi white-list: {} ", url);
        } else {
          throw new ApiAlertException("current api unsupported: " + url);
        }
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
                    bindForField.set(
                        args,
                        DateUtils.parse(
                            value.toString(), DateUtils.fullFormat(), TimeZone.getDefault()));
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

  @Pointcut("@annotation(org.apache.streampark.console.core.annotation.AppUpdated)")
  public void appUpdated() {}

  @Around("appUpdated()")
  public Object appUpdated(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    log.debug("appUpdated aspect, method:{}", methodSignature.getName());
    Object target = joinPoint.proceed();
    flinkAppHttpWatcher.initialize();
    return target;
  }

  @Pointcut("@annotation(org.apache.streampark.console.core.annotation.PermissionScope)")
  public void permissionAction() {}

  @Around("permissionAction()")
  public RestResponse permissionAction(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    PermissionScope permissionScope =
        methodSignature.getMethod().getAnnotation(PermissionScope.class);

    User currentUser = serviceHelper.getLoginUser();
    ApiAlertException.throwIfNull(currentUser, "Permission denied, please login first.");

    boolean isAdmin = currentUser.getUserType() == UserType.ADMIN;

    if (!isAdmin) {
      // 1) check userId
      Long userId = getId(joinPoint, methodSignature, permissionScope.user());
      ApiAlertException.throwIfTrue(
          userId != null && !currentUser.getUserId().equals(userId),
          "Permission denied, operations can only be performed with the permissions of the currently logged-in user.");

      // 2) check team
      Long teamId = getId(joinPoint, methodSignature, permissionScope.team());
      if (teamId != null) {
        Member member = memberService.findByUserName(teamId, currentUser.getUsername());
        ApiAlertException.throwIfTrue(
            member == null,
            "Permission denied, only members of this team can access this permission");
      }

      // 3) check app
      Long appId = getId(joinPoint, methodSignature, permissionScope.app());
      if (appId != null) {
        Application app = applicationService.getById(appId);
        ApiAlertException.throwIfTrue(app == null, "Invalid operation, application is null");
        if (!currentUser.getUserId().equals(app.getUserId())) {
          Member member = memberService.findByUserName(app.getTeamId(), currentUser.getUsername());
          ApiAlertException.throwIfTrue(
              member == null,
              "Permission denied, this job not created by the current user, And the job cannot be found in the current user's team.");
        }
      }
    }

    return (RestResponse) joinPoint.proceed();
  }

  private Long getId(ProceedingJoinPoint joinPoint, MethodSignature methodSignature, String expr) {
    if (StringUtils.isEmpty(expr)) {
      return null;
    }
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression(expr);
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
