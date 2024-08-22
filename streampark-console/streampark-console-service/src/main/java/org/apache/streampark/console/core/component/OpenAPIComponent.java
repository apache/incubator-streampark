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
import org.apache.streampark.console.base.util.Tuple2;
import org.apache.streampark.console.core.annotation.OpenAPI;
import org.apache.streampark.console.core.bean.OpenAPISchema;
import org.apache.streampark.console.core.controller.OpenAPIController;
import org.apache.streampark.console.core.util.ServiceHelper;
import org.apache.streampark.console.system.service.AccessTokenService;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
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

    private final Map<String, String> types = new HashMap<>();

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

    public String getOpenApiCUrl(String name, String baseUrl, Long appId, Long teamId) {
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
                switch (c.getBindFor()) {
                    case "appId":
                        curlBuilder.addFormData(c.getName(), appId);
                        break;
                    case "teamId":
                        curlBuilder.addFormData(c.getName(), teamId);
                        break;
                    default:
                        break;
                }
            } else {
                curlBuilder.addFormData(c.getName(), c.getDefaultValue());
            }
        });
        return curlBuilder.build();
    }

    private void initOpenAPISchema() {
        initTypes();
        Class<?> clazz = OpenAPIController.class;
        RequestMapping requestMapping = clazz.getDeclaredAnnotation(RequestMapping.class);
        String basePath = requestMapping.value()[0];
        List<Method> methodList = ReflectUtils.getMethodsByAnnotation(clazz, OpenAPI.class);

        for (Method method : methodList) {
            OpenAPISchema detail = new OpenAPISchema();

            List<OpenAPISchema.Schema> headerList = new ArrayList<>();
            OpenAPI openAPI = method.getDeclaredAnnotation(OpenAPI.class);
            for (OpenAPI.Param header : openAPI.header()) {
                headerList.add(paramToSchema(header));
            }

            List<OpenAPISchema.Schema> paramList = new ArrayList<>();
            for (OpenAPI.Param param : openAPI.param()) {
                paramList.add(paramToSchema(param));
            }

            detail.setSchema(paramList);
            detail.setHeader(headerList);

            Tuple2<String, String[]> methodURI = getMethodAndRequestURI(method);
            String[] requestURI = methodURI.t2;
            String uri = (requestURI != null && requestURI.length > 0) ? requestURI[0] : "";
            String restURI = "/" + basePath;
            if (uri != null) {
                restURI += "/" + uri;
            }
            restURI = restURI.replaceAll("/+", "/").replaceAll("/$", "");
            detail.setUrl(restURI);
            detail.setMethod(methodURI.t1);
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
        schema.setRequired(param.required());
        schema.setDescription(param.description());
        schema.setDefaultValue(param.defaultValue());
        String type = types.get(param.type().getSimpleName());
        if (type != null) {
            schema.setType(type);
        } else {
            schema.setType("string(" + param.type().getSimpleName() + ")");
        }
        return schema;
    }

    private Tuple2<String, String[]> getMethodAndRequestURI(Method method) {
        method.setAccessible(true);

        GetMapping getMapping = method.getDeclaredAnnotation(GetMapping.class);
        if (getMapping != null) {
            return Tuple2.of(HttpMethod.GET.name(), getMapping.value());
        }

        PostMapping postMapping = method.getDeclaredAnnotation(PostMapping.class);
        if (postMapping != null) {
            return Tuple2.of(HttpMethod.POST.name(), postMapping.value());
        }

        DeleteMapping deleteMapping = method.getDeclaredAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            return Tuple2.of(HttpMethod.DELETE.name(), deleteMapping.value());
        }

        PatchMapping patchMapping = method.getDeclaredAnnotation(PatchMapping.class);
        if (patchMapping != null) {
            return Tuple2.of(HttpMethod.PATCH.name(), patchMapping.value());
        }

        PutMapping putMapping = method.getDeclaredAnnotation(PutMapping.class);
        if (putMapping != null) {
            return Tuple2.of(HttpMethod.PUT.name(), putMapping.value());
        }

        throw new IllegalArgumentException("get http method and requestURI failed: " + method.getName());
    }

    private void initTypes() {
        types.put("String", "string");

        types.put("int", "integer(int32)");
        types.put("Integer", "integer(int32)");
        types.put("Short", "integer(int32)");

        types.put("long", "integer(int64)");
        types.put("Long", "integer(int64)");

        types.put("double", "number(double)");
        types.put("Double", "number(double)");

        types.put("float", "number(float)");
        types.put("Float", "number(float)");
        types.put("boolean", "boolean");
        types.put("Boolean", "boolean");

        types.put("byte", "string(byte)");
        types.put("Byte", "string(byte)");

        types.put("Date", "string(date)");
        types.put("DateTime", "string(datetime)");
    }
}
