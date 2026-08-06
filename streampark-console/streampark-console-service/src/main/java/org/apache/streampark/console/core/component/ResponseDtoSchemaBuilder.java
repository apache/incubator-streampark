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
import org.apache.streampark.console.core.bean.OpenAPISchema;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds OpenAPI-like field schemas from response DTO classes. */
public final class ResponseDtoSchemaBuilder {

    private ResponseDtoSchemaBuilder() {
    }

    public static List<OpenAPISchema.Schema> build(Class<?> responseType, Map<String, String> typeNames) {
        List<OpenAPISchema.Schema> schemas = new ArrayList<>();
        if (responseType == null || responseType == Void.class || responseType == void.class) {
            return schemas;
        }
        for (Field field : responseType.getDeclaredFields()) {
            if ("serialVersionUID".equals(field.getName())) {
                continue;
            }
            ApiParam apiParam = field.getAnnotation(ApiParam.class);
            OpenAPISchema.Schema schema = new OpenAPISchema.Schema();
            schema.setBindFor(field.getName());
            schema.setName(
                apiParam != null && StringUtils.isNotBlank(apiParam.name()) ? apiParam.name() : field.getName());
            schema.setRequired(false);
            schema.setDescription(apiParam != null ? apiParam.description() : field.getName());
            schema.setDefaultValue(apiParam != null ? apiParam.defaultValue() : "");
            schema.setType(RequestDtoSchemaBuilder.resolveTypeName(field.getType().getSimpleName(), typeNames));
            schemas.add(schema);
        }
        return schemas;
    }
}
