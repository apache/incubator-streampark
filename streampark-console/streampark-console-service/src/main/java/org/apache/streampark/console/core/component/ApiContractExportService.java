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

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.domain.RestResponseBody;
import org.apache.streampark.console.core.bean.ApiContractDocument;
import org.apache.streampark.console.core.bean.ApiContractDocument.ApiEndpointDescriptor;
import org.apache.streampark.console.core.bean.OpenAPISchema;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Scans registered MVC handlers and exports request/response DTO metadata. */
@Component
public class ApiContractExportService {

    private static final String CONSOLE_PACKAGE = "org.apache.streampark.console";

    private final RequestMappingHandlerMapping handlerMapping;

    public ApiContractExportService(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    public ApiContractDocument exportContracts() {
        Map<String, String> typeNames = RequestDtoSchemaBuilder.defaultTypeNames();
        ApiContractDocument document = new ApiContractDocument();
        Map<String, List<OpenAPISchema.Schema>> dtoSchemas = new LinkedHashMap<>();
        Set<Class<?>> dtoClasses = new LinkedHashSet<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            if (!isConsoleHandler(handlerMethod)) {
                continue;
            }
            ApiEndpointDescriptor endpoint = toDescriptor(entry.getKey(), handlerMethod, typeNames, dtoClasses);
            document.getEndpoints().add(endpoint);
        }

        for (Class<?> dtoClass : dtoClasses) {
            String key = dtoClass.getSimpleName();
            if (key.endsWith("Request")) {
                dtoSchemas.put(key, RequestDtoSchemaBuilder.build(dtoClass, null, typeNames));
            } else if (key.endsWith("Response")) {
                dtoSchemas.put(key, ResponseDtoSchemaBuilder.build(dtoClass, typeNames));
            }
        }
        document.setDtoSchemas(dtoSchemas);
        return document;
    }

    private ApiEndpointDescriptor toDescriptor(
                                               RequestMappingInfo mappingInfo,
                                               HandlerMethod handlerMethod,
                                               Map<String, String> typeNames,
                                               Set<Class<?>> dtoClasses) {
        ApiEndpointDescriptor descriptor = new ApiEndpointDescriptor();
        Method method = handlerMethod.getMethod();
        descriptor.setController(handlerMethod.getBeanType().getSimpleName());
        descriptor.setHandler(method.getName());
        descriptor.setHttpMethod(resolveHttpMethod(mappingInfo));
        descriptor.setPath(resolvePath(mappingInfo));

        Class<?> requestType = resolveRequestType(method);
        if (requestType != null) {
            descriptor.setRequestType(requestType.getSimpleName());
            descriptor.setRequestSchema(RequestDtoSchemaBuilder.build(requestType, null, typeNames));
            dtoClasses.add(requestType);
        }

        Class<?> responseDataType = resolveResponseDataClass(method);
        descriptor.setResponseDataType(
            responseDataType != null ? responseDataType.getSimpleName() : "Object");
        if (responseDataType != null && responseDataType.getSimpleName().endsWith("Response")) {
            dtoClasses.add(responseDataType);
        }
        return descriptor;
    }

    private static boolean isConsoleHandler(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getName().startsWith(CONSOLE_PACKAGE);
    }

    private static String resolveHttpMethod(RequestMappingInfo mappingInfo) {
        if (mappingInfo.getMethodsCondition().getMethods().isEmpty()) {
            return "POST";
        }
        return mappingInfo.getMethodsCondition().getMethods().iterator().next().name();
    }

    private static String resolvePath(RequestMappingInfo mappingInfo) {
        Set<String> patterns = new LinkedHashSet<>();
        if (mappingInfo.getPatternsCondition() != null) {
            patterns.addAll(mappingInfo.getPatternsCondition().getPatterns());
        }
        if (patterns.isEmpty()) {
            return "/";
        }
        return patterns.iterator().next();
    }

    private static Class<?> resolveRequestType(Method method) {
        for (Parameter parameter : method.getParameters()) {
            Class<?> type = parameter.getType();
            if (isSkippableParameter(type)) {
                continue;
            }
            return type;
        }
        return null;
    }

    private static boolean isSkippableParameter(Class<?> type) {
        return type.isPrimitive()
            || type == String.class
            || RestRequest.class.isAssignableFrom(type)
            || type.getName().startsWith("javax.servlet")
            || type.getName().startsWith("org.springframework");
    }

    private static Class<?> resolveResponseDataClass(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            Type rawType = parameterizedType.getRawType();
            if (rawType == RestResponseBody.class || rawType == RestResponse.class) {
                return extractClassType(parameterizedType.getActualTypeArguments()[0]);
            }
        }
        return null;
    }

    private static Class<?> extractClassType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return extractClassType(((ParameterizedType) type).getRawType());
        }
        return null;
    }
}
