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

package org.apache.streampark.console.base.web;

import org.apache.streampark.console.system.request.team.TeamCreateRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import javax.validation.Validation;
import javax.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormOrJsonArgumentResolverTest {

    private FormOrJsonArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        resolver = new FormOrJsonArgumentResolver(new ObjectMapper(), validator);
    }

    @Test
    void shouldBindFromJsonBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"teamName\":\"demo\",\"description\":\"test\"}".getBytes());

        org.springframework.core.MethodParameter parameter =
            new org.springframework.core.MethodParameter(FormOrJsonArgumentResolverTest.class.getDeclaredMethod(
                "sample", TeamCreateRequest.class), 0);

        Object target = resolver.resolveArgument(
            parameter, null, new ServletWebRequest(request), null);
        TeamCreateRequest dto = (TeamCreateRequest) target;
        assertEquals("demo", dto.getTeamName());
    }

    @Test
    void shouldBindFromFormFields() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/x-www-form-urlencoded");
        request.addParameter("teamName", "demo");
        request.addParameter("description", "test");

        org.springframework.core.MethodParameter parameter =
            new org.springframework.core.MethodParameter(FormOrJsonArgumentResolverTest.class.getDeclaredMethod(
                "sample", TeamCreateRequest.class), 0);

        Object target = resolver.resolveArgument(
            parameter, null, new ServletWebRequest(request), null);
        TeamCreateRequest dto = (TeamCreateRequest) target;
        assertEquals("demo", dto.getTeamName());
    }

    @SuppressWarnings({"java:S1144", "java:S1172"})
    private void sample(@FormOrJson TeamCreateRequest request) {
        // referenced reflectively by resolveArgument tests
    }
}
