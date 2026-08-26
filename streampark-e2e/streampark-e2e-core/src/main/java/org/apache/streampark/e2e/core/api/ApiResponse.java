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

package org.apache.streampark.e2e.core.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

/** Parsed {@code RestResponseBody} envelope from a StreamPark API call. */
@Getter
public final class ApiResponse {

    private static final long CODE_SUCCESS = 200L;

    private final int httpStatus;
    private final JsonNode root;
    private final Long code;
    private final String message;
    private final JsonNode data;

    public ApiResponse(int httpStatus, JsonNode root) {
        this.httpStatus = httpStatus;
        this.root = root;
        this.code = root != null && root.hasNonNull("code") ? root.get("code").asLong() : null;
        this.message = root != null && root.has("message") && !root.get("message").isNull()
            ? root.get("message").asText()
            : null;
        this.data = root != null && root.has("data") ? root.get("data") : null;
    }

    public boolean isSuccess() {
        return code != null && code.longValue() == CODE_SUCCESS;
    }

    public String text() {
        return root == null ? "" : root.toString();
    }
}
