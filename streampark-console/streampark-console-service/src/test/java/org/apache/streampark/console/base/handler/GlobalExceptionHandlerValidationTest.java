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

package org.apache.streampark.console.base.handler;

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.core.request.alert.AlertConfigRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

class GlobalExceptionHandlerValidationTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleBindException() {
        AlertConfigRequest target = new AlertConfigRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "alertName", "must not be blank"));
        BindException exception = new BindException(bindingResult);

        RestResponseBody<Void> response = handler.validExceptionHandler(exception);

        Assertions.assertEquals(RestResponse.STATUS_FAIL, response.getStatus());
        Assertions.assertTrue(response.getMessage().contains("alertName"));
    }
}
