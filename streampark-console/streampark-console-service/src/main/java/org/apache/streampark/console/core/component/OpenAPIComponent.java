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

import org.apache.streampark.common.util.ReflectUtils;
import org.apache.streampark.console.base.util.SpringContextUtils;
import org.apache.streampark.console.core.annotation.OpenAPI;
import org.apache.streampark.console.core.bean.OpenAPISchema;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.PostConstruct;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAPIComponent {

    private final Map<String, OpenAPISchema> schemas = new HashMap<>();

    @PostConstruct
    public void initialize() throws Exception {
        Map<String, Object> beans = SpringContextUtils.getBeansWithAnnotation(OpenAPI.class);
        for (Object value : beans.values()) {
            Class<?> clazz = Class.forName(value.getClass().getName().replaceAll("\\$.*$", ""));
            RequestMapping requestMapping = clazz.getDeclaredAnnotation(RequestMapping.class);
            String basePath = requestMapping.value()[0];
            List<Method> methodList = ReflectUtils.getMethodByAnnotation(clazz, OpenAPI.class);

            for (Method method : methodList) {
                String[] subUriPath = getMethodUriPath(method);
                String subPath = (subUriPath != null && subUriPath.length > 0) ? subUriPath[0] : "";
                String restUrl = "/" + basePath;
                if (subPath != null) {
                    restUrl += "/" + subPath;
                }
                restUrl = restUrl.replaceAll("/+", "/").replaceAll("/$", "");

                List<OpenAPISchema.Param> paramList = new ArrayList<>();
                OpenAPI openAPI = method.getDeclaredAnnotation(OpenAPI.class);
                OpenAPI.Param[] params = openAPI.param();
                for (OpenAPI.Param param : params) {
                    OpenAPISchema.Param paramDetail = new OpenAPISchema.Param();
                    paramDetail.setName(param.name());
                    paramDetail.setType(param.type().getName());
                    paramDetail.setRequired(param.required());
                    paramDetail.setDescription(param.description());
                    paramList.add(paramDetail);
                }
                OpenAPISchema detail = new OpenAPISchema();
                detail.setUrl(restUrl);
                detail.setParam(paramList);
                schemas.put(restUrl, detail);
            }
        }
    }

    private String[] getMethodUriPath(Method method) {
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

    public OpenAPISchema getOpenAPISchema(String url) {
        url = ("/" + url).replaceAll("/+", "/").replaceAll("/$", "");
        return schemas.get(url);
    }
}
