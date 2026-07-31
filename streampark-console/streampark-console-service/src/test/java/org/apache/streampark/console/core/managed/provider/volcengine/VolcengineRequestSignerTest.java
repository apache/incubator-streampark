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

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VolcengineRequestSignerTest {

    @Test
    void shouldMatchDeterministicOfficialSigningAlgorithmVector() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("Version", "2021-06-01");
        query.put("SearchKey", "中文 a");
        query.put("PageSize", "200");
        query.put("PageNum", "1");
        query.put("Action", "ListGMSProject");

        VolcengineRequestSigner.SignedRequest signed;
        try (
            VolcengineCredentials credentials =
                new VolcengineCredentials("AKLTEXAMPLE", "test-secret")) {
            signed =
                new VolcengineRequestSigner()
                    .sign(
                        "GET",
                        URI.create("https://open.volcengineapi.com"),
                        query,
                        new byte[0],
                        "cn-beijing",
                        "0123456789abcdef0123456789abcdef",
                        Instant.parse("2026-07-29T11:00:00Z"),
                        credentials);
        }

        assertThat(signed.getCanonicalQuery())
            .isEqualTo(
                "Action=ListGMSProject&PageNum=1&PageSize=200&"
                    + "SearchKey=%e4%b8%ad%e6%96%87%20a&Version=2021-06-01");
        assertThat(signed.getHeaders().get("X-Date")).isEqualTo("20260729T110000Z");
        assertThat(signed.getHeaders().get("Authorization"))
            .isEqualTo(
                "HMAC-SHA256 Credential=AKLTEXAMPLE/20260729/cn-beijing/flink/request, "
                    + "SignedHeaders=content-type;host;x-content-sha256;x-date;x-request-id, "
                    + "Signature=b35392bcb63778c4923f12c127e48f4b04cb6c3450922d3c9dca553c4368e3a2");
    }

    @Test
    void shouldClearCredentialArraysAfterUse() {
        VolcengineCredentials credentials =
            new VolcengineCredentials("temporary-ak", "temporary-sk");

        credentials.close();

        assertThat(credentials.accessKey()).containsOnly('\0');
        assertThat(credentials.secretKey()).containsOnly('\0');
    }
}
