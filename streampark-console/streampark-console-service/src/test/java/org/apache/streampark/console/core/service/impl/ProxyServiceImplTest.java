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

package org.apache.streampark.console.core.service.impl;

import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies that request data cannot alter proxy authority or escape its route. */
class ProxyServiceImplTest {

    private final ProxyServiceImpl service = new ProxyServiceImpl();

    @Test
    void appendProxyPathAndQuery() {
        MockHttpServletRequest request =
            request("/proxy/flink/7/jobs/42", "state=RUNNING");

        HttpUrl target = service.proxyUrl(
            "http://cluster.internal:8081/base/", request, "/proxy/flink/7");

        assertThat(target.host()).isEqualTo("cluster.internal");
        assertThat(target.port()).isEqualTo(8081);
        assertThat(target.encodedPath()).isEqualTo("/base/jobs/42");
        assertThat(target.queryParameter("state")).isEqualTo("RUNNING");
    }

    @Test
    void rejectProxyTraversal() {
        String[] paths = {
                "/proxy/flink/7/../admin",
                "/proxy/flink/7/%2e%2e/admin",
                "/proxy/flink/7/%252e%252e/admin"
        };

        for (String path : paths) {
            MockHttpServletRequest request = request(path, null);
            assertThatThrownBy(
                () -> service.proxyUrl(
                    "http://cluster.internal:8081/", request, "/proxy/flink/7"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("traversal");
        }
    }

    @Test
    void rejectPathOutsideRoute() {
        MockHttpServletRequest request = request("/proxy/flink/70/jobs", null);

        assertThatThrownBy(
            () -> service.proxyUrl(
                "http://cluster.internal:8081/", request, "/proxy/flink/7"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("outside the proxy route");
    }

    private static MockHttpServletRequest request(String path, String query) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setQueryString(query);
        return request;
    }
}
