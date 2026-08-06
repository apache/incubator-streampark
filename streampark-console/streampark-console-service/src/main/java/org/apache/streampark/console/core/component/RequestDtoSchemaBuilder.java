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

import org.apache.streampark.console.core.annotation.ApiParam;
import org.apache.streampark.console.core.annotation.OpenAPI;
import org.apache.streampark.console.core.bean.OpenAPISchema;

import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds OpenAPI parameter schemas from request DTO fields and {@link OpenAPI.Param} overrides. */
public final class RequestDtoSchemaBuilder {

    private static final String TYPE_INTEGER_INT32 = "integer(int32)";
    private static final String TYPE_BOOLEAN = "boolean";

    private RequestDtoSchemaBuilder() {
    }

    public static List<OpenAPISchema.Schema> build(Class<?> requestType, OpenAPI.Param[] overrides,
                                                   Map<String, String> typeNames) {
        Map<String, OpenAPISchema.Schema> merged = new LinkedHashMap<>();
        mergeOverrides(merged, overrides, typeNames);
        mergeRequestFields(merged, requestType, typeNames);
        return new ArrayList<>(merged.values());
    }

    private static void mergeOverrides(Map<String, OpenAPISchema.Schema> merged, OpenAPI.Param[] overrides,
                                       Map<String, String> typeNames) {
        if (overrides == null) {
            return;
        }
        for (OpenAPI.Param override : overrides) {
            OpenAPISchema.Schema schema = new OpenAPISchema.Schema();
            schema.setName(override.name());
            schema.setBindFor(StringUtils.isBlank(override.bindFor()) ? override.name() : override.bindFor());
            schema.setRequired(override.required());
            schema.setDescription(override.description());
            schema.setDefaultValue(override.defaultValue());
            schema.setType(resolveType(override.type().getSimpleName(), typeNames));
            merged.put(schema.getBindFor(), schema);
        }
    }

    private static void mergeRequestFields(Map<String, OpenAPISchema.Schema> merged, Class<?> requestType,
                                           Map<String, String> typeNames) {
        if (requestType == null) {
            return;
        }
        for (Field field : requestType.getDeclaredFields()) {
            if (shouldSkipField(merged, field)) {
                continue;
            }
            merged.put(field.getName(), toFieldSchema(field, typeNames));
        }
    }

    private static boolean shouldSkipField(Map<String, OpenAPISchema.Schema> merged, Field field) {
        return "serialVersionUID".equals(field.getName()) || merged.containsKey(field.getName());
    }

    private static OpenAPISchema.Schema toFieldSchema(Field field, Map<String, String> typeNames) {
        ApiParam apiParam = field.getAnnotation(ApiParam.class);
        OpenAPISchema.Schema schema = new OpenAPISchema.Schema();
        schema.setBindFor(field.getName());
        schema.setName(apiParam != null && StringUtils.isNotBlank(apiParam.name())
            ? apiParam.name()
            : field.getName());
        schema.setRequired(isRequired(field, apiParam));
        schema.setDescription(apiParam != null ? apiParam.description() : field.getName());
        schema.setDefaultValue(apiParam != null ? apiParam.defaultValue() : "");
        schema.setType(resolveType(field.getType().getSimpleName(), typeNames));
        return schema;
    }

    private static boolean isRequired(Field field, ApiParam apiParam) {
        if (apiParam != null && apiParam.required()) {
            return true;
        }
        return field.getAnnotation(NotNull.class) != null || field.getAnnotation(NotBlank.class) != null;
    }

    public static String resolveTypeName(String simpleName, Map<String, String> typeNames) {
        return resolveType(simpleName, typeNames);
    }

    private static String resolveType(String simpleName, Map<String, String> typeNames) {
        String mapped = typeNames.get(simpleName);
        if (mapped != null) {
            return mapped;
        }
        return "string(" + simpleName + ")";
    }

    public static Map<String, String> defaultTypeNames() {
        Map<String, String> types = new HashMap<>();
        types.put("String", "string");
        types.put("int", TYPE_INTEGER_INT32);
        types.put("Integer", TYPE_INTEGER_INT32);
        types.put("Short", TYPE_INTEGER_INT32);
        types.put("long", "integer(int64)");
        types.put("Long", "integer(int64)");
        types.put("double", "number(double)");
        types.put("Double", "number(double)");
        types.put("float", "number(float)");
        types.put("Float", "number(float)");
        types.put("boolean", TYPE_BOOLEAN);
        types.put("Boolean", TYPE_BOOLEAN);
        types.put("byte", "string(byte)");
        types.put("Byte", "string(byte)");
        types.put("Date", "string(date)");
        return types;
    }
}
