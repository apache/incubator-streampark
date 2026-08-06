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

import org.apache.streampark.console.core.bean.ApiContractDocument;
import org.apache.streampark.console.core.bean.OpenAPISchema;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/** Generates TypeScript interfaces from exported API contract metadata. */
public final class ApiTypeScriptGenerator {

    private static final String TS_TYPE_NUMBER = "number";

    private ApiTypeScriptGenerator() {
    }

    public static String generate(ApiContractDocument document) {
        StringBuilder builder = new StringBuilder();
        builder.append("// Auto-generated from console API contract export. Do not edit manually.\n\n");
        builder.append("export interface RestResponseBody<T> {\n");
        builder.append("  status: 'success' | 'error';\n");
        builder.append("  code: ").append(TS_TYPE_NUMBER).append(";\n");
        builder.append("  message?: string;\n");
        builder.append("  data?: T;\n");
        builder.append("}\n\n");

        if (document.getDtoSchemas() != null) {
            for (Map.Entry<String, List<OpenAPISchema.Schema>> entry : document.getDtoSchemas().entrySet()) {
                builder.append("export interface ").append(entry.getKey()).append(" {\n");
                for (OpenAPISchema.Schema schema : entry.getValue()) {
                    builder.append("  ").append(schema.getName()).append(toOptional(schema.isRequired()));
                    builder.append(": ").append(toTsType(schema.getType())).append(";\n");
                }
                builder.append("}\n\n");
            }
        }

        builder.append("export interface ApiEndpointDescriptor {\n");
        builder.append("  controller: string;\n");
        builder.append("  handler: string;\n");
        builder.append("  path: string;\n");
        builder.append("  httpMethod: string;\n");
        builder.append("  requestType?: string;\n");
        builder.append("  responseDataType?: string;\n");
        builder.append("}\n\n");

        builder.append("export const apiEndpoints: ApiEndpointDescriptor[] = [\n");
        for (ApiContractDocument.ApiEndpointDescriptor endpoint : document.getEndpoints()) {
            builder.append("  {");
            builder.append(" controller: '").append(endpoint.getController()).append("',");
            builder.append(" handler: '").append(endpoint.getHandler()).append("',");
            builder.append(" path: '").append(endpoint.getPath()).append("',");
            builder.append(" httpMethod: '").append(endpoint.getHttpMethod()).append("',");
            builder.append(" requestType: '").append(StringUtils.defaultString(endpoint.getRequestType())).append("',");
            builder.append(" responseDataType: '")
                .append(StringUtils.defaultString(endpoint.getResponseDataType()))
                .append("'");
            builder.append(" },\n");
        }
        builder.append("];\n");
        return builder.toString();
    }

    private static String toOptional(boolean required) {
        return required ? "" : "?";
    }

    private static String toTsType(String openApiType) {
        if (openApiType == null) {
            return "unknown";
        }
        if (openApiType.startsWith("integer")) {
            return TS_TYPE_NUMBER;
        }
        if (openApiType.startsWith("number")) {
            return TS_TYPE_NUMBER;
        }
        if ("boolean".equals(openApiType)) {
            return "boolean";
        }
        if (openApiType.startsWith("string")) {
            return "string";
        }
        return "unknown";
    }
}
