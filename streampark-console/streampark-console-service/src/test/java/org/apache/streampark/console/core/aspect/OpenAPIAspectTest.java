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

import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.core.controller.OpenAPIController;
import org.apache.streampark.console.core.request.flink.FlinkAppStartRequest;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenAPIAspectTest {

    private final OpenAPIAspect openAPIAspect = new OpenAPIAspect();

    @BeforeEach
    void bindSubject() {
        Subject subject = new Subject.Builder(new DefaultSecurityManager()).buildSubject();
        ThreadContext.bind(subject);
    }

    @AfterEach
    void unbindSubject() {
        ThreadContext.unbindSubject();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldReturnRestResponseBodyWhenApiTokenNotUsed() throws Throwable {
        RestResponseBody<Boolean> expected = RestResponseBody.success(true);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("flinkStart");
        when(joinPoint.proceed()).thenReturn(expected);

        Object result = openAPIAspect.openAPI(joinPoint);
        assertSame(expected, result);
    }

    @Test
    void shouldBindOpenApiAliasFieldWhenApiTokenPresent() throws Throwable {
        FlinkAppStartRequest request = new FlinkAppStartRequest();
        RestResponseBody<Boolean> expected = RestResponseBody.success(true);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = OpenAPIController.class.getMethod("flinkStart", FlinkAppStartRequest.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{request});
        when(joinPoint.proceed()).thenReturn(expected);

        Subject subject = ThreadContext.getSubject();
        subject.getSession().setAttribute(org.apache.streampark.console.system.entity.AccessToken.IS_API_TOKEN, true);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setParameter("restoreFromSavepoint", "true");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        Object result = openAPIAspect.openAPI(joinPoint);
        assertSame(expected, result);
        assertTrue(request.getRestoreOrTriggerSavepoint());
    }
}
