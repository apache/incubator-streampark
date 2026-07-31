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

package org.apache.streampark.console.core.managed.provider.volcengine;

import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/** Maps Volcengine service codes and HTTP status to provider-neutral error categories. */
@Component
class VolcengineProviderErrorMapper {

    private static final Pattern SAFE_CODE = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    ManagedFlinkProviderException fromResponse(
                                               int httpStatus,
                                               JsonNode root,
                                               String fallbackRequestId) {
        return fromResponse(httpStatus, root, fallbackRequestId, null);
    }

    ManagedFlinkProviderException fromResponse(
                                               int httpStatus,
                                               JsonNode root,
                                               String fallbackRequestId,
                                               Long retryAfterMillis) {
        JsonNode metadata = firstObject(root, "ResponseMetadata", "responseMetadata");
        JsonNode error = metadata == null ? null : firstObject(metadata, "Error", "error");
        if (error == null) {
            error = firstObject(root, "Error", "error");
        }
        String code = firstText(error, "Code", "code", "Type", "type");
        String requestId =
            firstNonBlank(
                firstText(metadata, "RequestId", "RequestID", "requestId", "request_id"),
                firstText(root, "RequestId", "RequestID", "requestId", "request_id"),
                fallbackRequestId);

        if (httpStatus >= 200 && httpStatus < 300 && code == null) {
            return null;
        }
        String safeCode = safeCode(code, httpStatus);
        ProviderErrorCategory category = category(safeCode, httpStatus);
        return new ManagedFlinkProviderException(
            category,
            safeCode,
            requestId,
            "Volcengine Flink request failed.",
            retryAfterMillis);
    }

    ManagedFlinkProviderException networkFailure() {
        return new ManagedFlinkProviderException(
            ProviderErrorCategory.TRANSIENT,
            "NetworkError",
            null,
            "Volcengine Flink request could not reach the service.");
    }

    private static ProviderErrorCategory category(String code, int status) {
        String normalized = code.toLowerCase(Locale.ROOT);
        if (status == 429
            || containsAny(
                normalized,
                "throttling",
                "requestlimitexceeded",
                "toomanyrequests",
                "ratelimit")) {
            return ProviderErrorCategory.RATE_LIMIT;
        }
        if (containsAny(
            normalized,
            "invalidcredential",
            "invalidaccesskey",
            "signaturedoesnotmatch",
            "authenticationfailed",
            "invalidsecret")) {
            return ProviderErrorCategory.AUTHENTICATION;
        }
        if (status == 401
            || status == 403
            || containsAny(
                normalized,
                "accessdenied",
                "permissiondenied",
                "forbidden",
                "unauthorizedoperation")) {
            return ProviderErrorCategory.AUTHORIZATION;
        }
        if (status == 404 || containsAny(normalized, "notfound", "notexist")) {
            return ProviderErrorCategory.NOT_FOUND;
        }
        if (status == 409
            || containsAny(
                normalized, "conflict", "resourceinuse", "alreadyexist", "operationinprogress")) {
            return ProviderErrorCategory.CONFLICT;
        }
        if (containsAny(
            normalized,
            "quota",
            "insufficientcapacity",
            "insufficientresource",
            "resourceexhausted")) {
            return ProviderErrorCategory.QUOTA;
        }
        if (containsAny(
            normalized,
            "invalidhostalias",
            "invaliddns",
            "providerconfiguration",
            "endpointconfiguration")) {
            return ProviderErrorCategory.PROVIDER_CONFIGURATION;
        }
        if (status == 400
            || status == 422
            || containsAny(
                normalized,
                "invalidparameter",
                "missingparameter",
                "invalidargument",
                "user_input_exception",
                "serialization",
                "deserialization")) {
            return ProviderErrorCategory.VALIDATION;
        }
        if (status == 408 || status >= 500) {
            return ProviderErrorCategory.TRANSIENT;
        }
        return ProviderErrorCategory.UNKNOWN;
    }

    private static String safeCode(String code, int status) {
        if (code != null && SAFE_CODE.matcher(code).matches()) {
            return code;
        }
        return status > 0 ? "Http" + status : "UnknownProviderError";
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static JsonNode firstObject(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isObject()) {
                return value;
            }
        }
        return null;
    }

    private static String firstText(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isValueNode()) {
                String text = value.asText();
                if (!text.trim().isEmpty()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
