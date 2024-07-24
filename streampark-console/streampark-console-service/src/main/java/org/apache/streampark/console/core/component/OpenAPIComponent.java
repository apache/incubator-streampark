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

package org.apache.streampark.console.core.component;

import org.apache.streampark.common.util.CURLBuilder;
import org.apache.streampark.common.util.ReflectUtils;
import org.apache.streampark.console.core.annotation.OpenAPI;
import org.apache.streampark.console.core.bean.OpenAPISchema;
import org.apache.streampark.console.core.controller.OpenAPIController;
import org.apache.streampark.console.core.util.ServiceHelper;
import org.apache.streampark.console.system.service.AccessTokenService;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAPIComponent {

    @Autowired
    private AccessTokenService accessTokenService;

    private final Map<String, OpenAPISchema> schemas = new HashMap<>();

    public synchronized OpenAPISchema getOpenAPISchema(String name) {
        if (schemas.isEmpty()) {
            try {
                initOpenAPISchema();
            } catch (Exception e) {
                log.error("InitOpenAPISchema failed", e);
            }
        }
        return schemas.get(name);
    }

    private void initOpenAPISchema() {
        Class<?> clazz = OpenAPIController.class;
        RequestMapping requestMapping = clazz.getDeclaredAnnotation(RequestMapping.class);
        String basePath = requestMapping.value()[0];
        List<Method> methodList = ReflectUtils.getMethodsByAnnotation(clazz, OpenAPI.class);

        for (Method method : methodList) {
            String[] subUriPath = getMethodUriPath(method);
            String subPath = (subUriPath != null && subUriPath.length > 0) ? subUriPath[0] : "";
            String restUrl = "/" + basePath;
            if (subPath != null) {
                restUrl += "/" + subPath;
            }
            restUrl = restUrl.replaceAll("/+", "/").replaceAll("/$", "");

            List<OpenAPISchema.Schema> headerList = new ArrayList<>();
            OpenAPI openAPI = method.getDeclaredAnnotation(OpenAPI.class);
            for (OpenAPI.Param header : openAPI.header()) {
                headerList.add(paramToSchema(header));
            }

            List<OpenAPISchema.Schema> paramList = new ArrayList<>();
            for (OpenAPI.Param param : openAPI.param()) {
                paramList.add(paramToSchema(param));
            }

            OpenAPISchema detail = new OpenAPISchema();
            detail.setUrl(restUrl);
            detail.setSchema(paramList);
            detail.setHeader(headerList);

            schemas.put(openAPI.name(), detail);
        }
    }

    private OpenAPISchema.Schema paramToSchema(OpenAPI.Param param) {
        OpenAPISchema.Schema schema = new OpenAPISchema.Schema();
        schema.setName(param.name());
        if (StringUtils.isBlank(param.bindFor())) {
            schema.setBindFor(param.name());
        } else {
            schema.setBindFor(param.bindFor());
        }
        schema.setType(param.type().getName());
        schema.setRequired(param.required());
        schema.setDescription(param.description());
        schema.setDefaultValue(param.defaultValue());
        return schema;
    }

    private String[] getMethodUriPath(Method method) {
        method.setAccessible(true);

        GetMapping getMapping = method.getDeclaredAnnotation(GetMapping.class);
        if (getMapping != null) {
            return getMapping.value();
        }

        PostMapping postMapping = method.getDeclaredAnnotation(PostMapping.class);
        if (postMapping != null) {
            return postMapping.value();
        }

        DeleteMapping deleteMapping = method.getDeclaredAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            return deleteMapping.value();
        }

        PatchMapping patchMapping = method.getDeclaredAnnotation(PatchMapping.class);
        if (patchMapping != null) {
            return patchMapping.value();
        }

        PutMapping putMapping = method.getDeclaredAnnotation(PutMapping.class);
        if (putMapping != null) {
            return putMapping.value();
        }
        return null;
    }

    public String getOpenApiCUrl(String baseUrl, Long appId, Long teamId, String name) {
        OpenAPISchema schema = this.getOpenAPISchema(name);
        if (schema == null) {
            throw new UnsupportedOperationException("Unsupported OpenAPI: " + name);
        }

        String url = schema.getUrl();
        if (StringUtils.isNoneBlank(baseUrl)) {
            url = baseUrl + url;
        }
        CURLBuilder curlBuilder = new CURLBuilder(url);
        curlBuilder
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader(
                "Authorization",
                accessTokenService.getByUserId(ServiceHelper.getUserId()).getToken());

        schema.getSchema().forEach(c -> {
            if (c.isRequired()) {
                if ("appId".equals(c.getBindFor())) {
                    curlBuilder.addFormData(c.getName(), appId);
                } else if ("teamId".equals(c.getBindFor())) {
                    curlBuilder.addFormData(c.getName(), teamId);
                }
            } else {
                curlBuilder.addFormData(c.getName(), c.getDefaultValue());
            }
        });
        return curlBuilder.build();
    }
}
