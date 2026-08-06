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
import org.apache.streampark.console.core.controller.FlinkApplicationController;
import org.apache.streampark.console.core.request.flink.FlinkAppIdRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/** Verifies {@link PermissionAspect} signature accepts {@link RestResponseBody} returns. */
class PermissionAspectTest {

    @Test
    void permissionAspectShouldDeclareObjectReturnType() throws NoSuchMethodException {
        Method aspectMethod = PermissionAspect.class.getDeclaredMethod(
            "permissionAction", org.aspectj.lang.ProceedingJoinPoint.class);
        Assertions.assertEquals(Object.class, aspectMethod.getReturnType());
    }

    @Test
    void permissionProtectedControllerShouldReturnRestResponseBody() throws NoSuchMethodException {
        Method getMethod = FlinkApplicationController.class.getMethod("get", FlinkAppIdRequest.class);
        Assertions.assertTrue(
            RestResponseBody.class.isAssignableFrom(getMethod.getReturnType()));
    }
}
