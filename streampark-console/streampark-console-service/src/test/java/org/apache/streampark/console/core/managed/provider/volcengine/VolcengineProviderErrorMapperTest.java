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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VolcengineProviderErrorMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VolcengineProviderErrorMapper mapper = new VolcengineProviderErrorMapper();

    @Test
    void shouldTreatEmbeddedErrorAsFailureEvenWithHttpSuccess() throws Exception {
        ManagedFlinkProviderException failure =
            mapper.fromResponse(
                200,
                response("SignatureDoesNotMatch", "request-auth"),
                null);

        assertThat(failure.getCategory()).isEqualTo(ProviderErrorCategory.AUTHENTICATION);
        assertThat(failure.getProviderRequestId()).isEqualTo("request-auth");
        assertThat(failure.isRetryable()).isFalse();
    }

    @Test
    void shouldMapRateLimitAndTransientStatusesAsRetryable() throws Exception {
        ManagedFlinkProviderException rateLimit =
            mapper.fromResponse(429, response("Throttling", "request-rate"), null);
        ManagedFlinkProviderException unavailable =
            mapper.fromResponse(503, response("InternalError", "request-503"), null);

        assertThat(rateLimit.getCategory()).isEqualTo(ProviderErrorCategory.RATE_LIMIT);
        assertThat(rateLimit.isRetryable()).isTrue();
        assertThat(unavailable.getCategory()).isEqualTo(ProviderErrorCategory.TRANSIENT);
        assertThat(unavailable.isRetryable()).isTrue();
    }

    @Test
    void shouldMapAuthorizationValidationQuotaConflictAndNotFound() throws Exception {
        assertCategory("AccessDenied", 403, ProviderErrorCategory.AUTHORIZATION);
        assertCategory("InvalidParameterValue", 400, ProviderErrorCategory.VALIDATION);
        assertCategory("USER_INPUT_EXCEPTION", 200, ProviderErrorCategory.VALIDATION);
        assertCategory("QuotaExceeded", 400, ProviderErrorCategory.QUOTA);
        assertCategory("MetaResourceAlreadyExist", 409, ProviderErrorCategory.CONFLICT);
        assertCategory("ResourceNotFound", 404, ProviderErrorCategory.NOT_FOUND);
    }

    @Test
    void shouldReplaceUnsafeProviderCodesAndMessages() throws Exception {
        ManagedFlinkProviderException failure =
            mapper.fromResponse(
                400,
                response("unsafe code with secret=plaintext", "request-safe"),
                null);

        assertThat(failure.getProviderCode()).isEqualTo("Http400");
        assertThat(failure.getMessage()).isEqualTo("Volcengine Flink request failed.");
        assertThat(failure.toString()).doesNotContain("plaintext");
    }

    @Test
    void shouldAcceptSuccessfulResponseWithoutErrorNode() throws Exception {
        JsonNode root = objectMapper.readTree("{\"ResponseMetadata\":{\"RequestId\":\"ok\"}}");

        assertThat(mapper.fromResponse(200, root, null)).isNull();
    }

    private void assertCategory(
                                String code,
                                int status,
                                ProviderErrorCategory expected) throws Exception {
        assertThat(mapper.fromResponse(status, response(code, "request"), null).getCategory())
            .isEqualTo(expected);
    }

    private JsonNode response(String code, String requestId) throws Exception {
        return objectMapper.readTree(
            "{\"ResponseMetadata\":{\"RequestId\":\""
                + requestId
                + "\",\"Error\":{\"Code\":\""
                + code
                + "\",\"Message\":\"raw provider detail\"}}}");
    }
}
