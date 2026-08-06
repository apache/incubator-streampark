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
import org.apache.streampark.console.core.managed.api.ProviderContext;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VolcengineOpenApiClientTest {

    private HttpServer server;
    private URI endpoint;
    private VolcengineCredentialResolver credentialResolver;
    private VolcengineFlinkProperties properties;
    private AtomicLong retryDelayMillis;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        credentialResolver = mock(VolcengineCredentialResolver.class);
        properties = new VolcengineFlinkProperties();
        properties.setRequestTimeoutMs(2000);
        properties.setMaxReadRetries(1);
        properties.setInitialBackoffMs(0);
        properties.setMaxBackoffMs(5000);
        properties.setMaxConcurrentRequestsPerAccount(1);
        properties.setMinRequestIntervalMs(0);
        retryDelayMillis = new AtomicLong();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldSignRequestAndRetryRateLimitOnce() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        AtomicReference<String> query = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestId = new AtomicReference<>();
        server.createContext(
            "/",
            exchange -> {
                int current = requests.incrementAndGet();
                query.set(exchange.getRequestURI().getRawQuery());
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                requestId.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
                if (current == 1) {
                    exchange.getResponseHeaders().set("Retry-After", "1");
                    respond(
                        exchange,
                        429,
                        "{\"ResponseMetadata\":{\"RequestId\":\"rate-1\","
                            + "\"Error\":{\"Code\":\"Throttling\"}}}");
                } else {
                    respond(
                        exchange,
                        200,
                        "{\"ResponseMetadata\":{\"RequestId\":\"request-ok\"},"
                            + "\"Result\":{\"ProjectList\":[]}}");
                }
            });
        VolcengineCredentials credentials =
            new VolcengineCredentials("AKLT-test", "secret-test");
        when(credentialResolver.resolve(context())).thenReturn(credentials);

        VolcengineOpenApiResponse response =
            client()
                .get(
                    context(),
                    "ListGMSProject",
                    "2021-06-01",
                    Collections.singletonMap("SearchKey", "中文"));

        assertThat(requests).hasValue(2);
        assertThat(retryDelayMillis).hasValue(1000);
        assertThat(response.getRequestId()).isEqualTo("request-ok");
        assertThat(query.get())
            .contains(
                "Action=ListGMSProject",
                "SearchKey=%e4%b8%ad%e6%96%87",
                "Version=2021-06-01");
        assertThat(authorization.get())
            .contains(
                "Credential=AKLT-test/20260729/cn-beijing/flink/request",
                "SignedHeaders=content-type;host;x-content-sha256;x-date;x-request-id")
            .doesNotContain("secret-test");
        assertThat(requestId.get()).hasSize(32);
        assertThat(credentials.accessKey()).containsOnly('\0');
        assertThat(credentials.secretKey()).containsOnly('\0');
    }

    @Test
    void shouldNotRetryAuthenticationFailureReturnedWithHttpSuccess() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server.createContext(
            "/",
            exchange -> {
                requests.incrementAndGet();
                respond(
                    exchange,
                    200,
                    "{\"ResponseMetadata\":{\"RequestId\":\"auth-request\","
                        + "\"Error\":{\"Code\":\"SignatureDoesNotMatch\","
                        + "\"Message\":\"raw secret-like detail\"}}}");
            });
        when(credentialResolver.resolve(context()))
            .thenReturn(new VolcengineCredentials("AKLT-test", "secret-test"));

        assertThatThrownBy(
            () -> client()
                .get(
                    context(),
                    "ListGMSProject",
                    "2021-06-01",
                    Collections.emptyMap()))
                        .isInstanceOfSatisfying(
                            ManagedFlinkProviderException.class,
                            failure -> {
                                assertThat(failure.getCategory())
                                    .isEqualTo(ProviderErrorCategory.AUTHENTICATION);
                                assertThat(failure.getProviderRequestId()).isEqualTo("auth-request");
                                assertThat(failure.getMessage()).doesNotContain("raw", "secret-like");
                            });
        assertThat(requests).hasValue(1);
    }

    @Test
    void shouldRejectAccountLevelCustomEndpointBeforeResolvingCredential() {
        ProviderContext customContext =
            ProviderContext.builder()
                .cloudAccountId(1L)
                .credentialVersion(0L)
                .region("cn-beijing")
                .endpoint("https://example.com")
                .build();

        assertThatThrownBy(
            () -> client()
                .get(
                    customContext,
                    "ListGMSProject",
                    "2021-06-01",
                    Collections.emptyMap()))
                        .isInstanceOfSatisfying(
                            ManagedFlinkProviderException.class,
                            failure -> assertThat(failure.getCategory())
                                .isEqualTo(ProviderErrorCategory.PROVIDER_CONFIGURATION));
    }

    @Test
    void shouldSignJsonPostRequest() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext(
            "/",
            exchange -> {
                method.set(exchange.getRequestMethod());
                contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                body.set(
                    new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8));
                respond(
                    exchange,
                    200,
                    "{\"ResponseMetadata\":{\"RequestId\":\"request-job\"},"
                        + "\"Result\":{\"Id\":\"job-1\",\"State\":\"RUNNING\"}}");
            });
        when(credentialResolver.resolve(context()))
            .thenReturn(new VolcengineCredentials("AKLT-test", "secret-test"));
        Map<String, String> requestBody = new LinkedHashMap<>();
        requestBody.put("AccountId", "");
        requestBody.put("Id", "job-1");

        VolcengineOpenApiResponse response =
            client()
                .post(
                    context(),
                    "GetGWSApplication",
                    "2021-06-01",
                    Collections.singletonMap("ProjectId", "project-1"),
                    requestBody);

        assertThat(method).hasValue("POST");
        assertThat(contentType).hasValue("application/json");
        assertThat(body).hasValue("{\"AccountId\":\"\",\"Id\":\"job-1\"}");
        assertThat(response.getRequestId()).isEqualTo("request-job");
    }

    @Test
    void shouldResolveAccountIdThroughIamService() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext(
            "/",
            exchange -> {
                query.set(exchange.getRequestURI().getRawQuery());
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                respond(
                    exchange,
                    200,
                    "{\"ResponseMetadata\":{\"RequestId\":\"request-iam\"},"
                        + "\"Result\":{\"User\":{\"AccountId\":\"2101000277\"}}}");
            });
        when(credentialResolver.resolve(context()))
            .thenReturn(new VolcengineCredentials("AKLT-test", "secret-test"));

        assertThat(client().resolveProviderAccountId(context())).isEqualTo("2101000277");
        assertThat(query.get())
            .contains("Action=GetUser", "AccessKeyID=AKLT-test", "Version=2018-01-01");
        assertThat(authorization.get())
            .contains("Credential=AKLT-test/20260729/cn-beijing/iam/request");
    }

    private VolcengineOpenApiClient client() {
        return new VolcengineOpenApiClient(
            credentialResolver,
            new VolcengineRequestSigner(),
            new VolcengineProviderErrorMapper(),
            new ObjectMapper(),
            properties,
            HttpClient.newBuilder().build(),
            endpoint,
            Clock.fixed(Instant.parse("2026-07-29T11:00:00Z"), ZoneOffset.UTC),
            retryDelayMillis::set,
            new VolcengineApiRateLimiter(1, 0));
    }

    private static ProviderContext context() {
        return ProviderContext.builder()
            .cloudAccountId(1L)
            .credentialVersion(0L)
            .region("cn-beijing")
            .build();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, content.length);
        exchange.getResponseBody().write(content);
        exchange.close();
    }
}
