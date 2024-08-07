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

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.enums.UserMessageStatus;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.annotation.Permission;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.UserTypeEnum;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.console.core.util.ServiceHelper;
import org.apache.streampark.console.system.entity.Member;
import org.apache.streampark.console.system.entity.User;
import org.apache.streampark.console.system.service.MemberService;

import org.apache.commons.lang3.StringUtils;

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

import static org.apache.streampark.console.base.enums.ApplicationMessageStatus.API_NOT_SUPPORT;
import static org.apache.streampark.console.base.enums.CommonStatus.UNKNOWN_ERROR;
import static org.apache.streampark.console.base.enums.FlinkMessageStatus.FLINk_APP_IS_NULL;
import static org.apache.streampark.console.base.enums.UserMessageStatus.SYSTEM_PERMISSION_JOB_OWNER_MISMATCH;
import static org.apache.streampark.console.base.enums.UserMessageStatus.SYSTEM_PERMISSION_LOGIN_USER_PERMISSION_MISMATCH;
import static org.apache.streampark.console.base.enums.UserMessageStatus.SYSTEM_PERMISSION_TEAM_NO_PERMISSION;

@Slf4j
@Component
@Aspect
public class PermissionAspect {

    @Autowired
    private MemberService memberService;

    @Autowired
    private ApplicationManageService applicationManageService;

    @Pointcut("@annotation(org.apache.streampark.console.core.annotation.Permission)")
    public void permissionPointcut() {
    }

    @Around("permissionPointcut()")
    public RestResponse permissionAction(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Permission permission = methodSignature.getMethod().getAnnotation(Permission.class);

        User currentUser = ServiceHelper.getLoginUser();
        ApiAlertException.throwIfNull(currentUser, UserMessageStatus.SYSTEM_USER_NOT_LOGIN);

        boolean isAdmin = currentUser.getUserType() == UserTypeEnum.ADMIN;

        if (!isAdmin) {
            // 1) check userId
            Long userId = getId(joinPoint, methodSignature, permission.user());
            ApiAlertException.throwIfTrue(
                userId != null && !currentUser.getUserId().equals(userId),
                SYSTEM_PERMISSION_LOGIN_USER_PERMISSION_MISMATCH);

            // 2) check team
            Long teamId = getId(joinPoint, methodSignature, permission.team());
            if (teamId != null) {
                Member member = memberService.getByTeamIdUserName(teamId, currentUser.getUsername());
                ApiAlertException.throwIfTrue(member == null, SYSTEM_PERMISSION_TEAM_NO_PERMISSION);
            }

            // 3) check app
            Long appId = getId(joinPoint, methodSignature, permission.app());
            if (appId != null) {
                Application app = applicationManageService.getById(appId);
                ApiAlertException.throwIfTrue(app == null, FLINk_APP_IS_NULL);
                if (!currentUser.getUserId().equals(app.getUserId())) {
                    Member member = memberService.getByTeamIdUserName(app.getTeamId(), currentUser.getUsername());
                    ApiAlertException.throwIfTrue(member == null, SYSTEM_PERMISSION_JOB_OWNER_MISMATCH);
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
            return ApiAlertException.throwException(UNKNOWN_ERROR,
                "Wrong use of annotation on method " + methodSignature.getName(), e);
        }
    }
}
