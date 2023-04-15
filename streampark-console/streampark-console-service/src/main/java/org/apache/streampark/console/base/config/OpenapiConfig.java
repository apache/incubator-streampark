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

package org.apache.streampark.console.base.config;

import org.apache.http.HttpHeaders;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;

@EnableKnife4j
@Configuration
@ConditionalOnWebApplication
public class OpenapiConfig implements WebMvcConfigurer {

  @Bean
  public OpenAPI apiV1Info() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Apache StreamPark Api Docs")
                .description("Apache StreamPark Api Docs")
                .contact(
                    new Contact()
                        .name("Apache StreamPark")
                        .url("https://streampark.apache.org/")
                        .email("dev@streampark.apache.org"))
                .version("2.1.0-SNAPSHOT")
                .license(new License().name("Apache-2.0 license")))
        .components(
            new Components()
                .addSecuritySchemes(
                    HttpHeaders.AUTHORIZATION,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .name(HttpHeaders.AUTHORIZATION)
                        .in(SecurityScheme.In.HEADER)))
        .security(
            Collections.singletonList(
                new SecurityRequirement().addList(HttpHeaders.AUTHORIZATION)));
  }

  @Bean
  public GroupedOpenApi publicApiV1() {
    return GroupedOpenApi.builder().group("v1").pathsToExclude("/v2/**").build();
  }

  @Bean
  public GroupedOpenApi publicApiV2() {
    return GroupedOpenApi.builder().group("v2").pathsToMatch("/v2/**").build();
  }
}
