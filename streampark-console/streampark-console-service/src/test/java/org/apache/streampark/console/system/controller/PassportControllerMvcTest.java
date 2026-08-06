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

package org.apache.streampark.console.system.controller;

import org.apache.streampark.console.base.handler.GlobalExceptionHandler;
import org.apache.streampark.console.base.web.FormOrJsonArgumentResolver;
import org.apache.streampark.console.system.security.Authenticator;
import org.apache.streampark.console.system.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PassportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, FormOrJsonArgumentResolver.class, PassportControllerMvcTest.MvcTestConfig.class})
class PassportControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private Authenticator authenticator;

    @org.springframework.boot.test.context.TestConfiguration
    static class MvcTestConfig implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {

        @Autowired
        private FormOrJsonArgumentResolver formOrJsonArgumentResolver;

        @Override
        public void addArgumentResolvers(
                                         java.util.List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(formOrJsonArgumentResolver);
        }
    }

    @Test
    void signinShouldReturnLegacyFailureCode() throws Exception {
        mockMvc.perform(
            post("/passport/signin")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }
}
