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

package org.apache.streampark.console.base.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WebUtilsTest {

    private HttpServer server;
    private AtomicReference<String> method;
    private AtomicReference<String> requestBody;
    private AtomicReference<String> forwardedHeader;
    private AtomicReference<String> skippedHeader;
    private String endpoint;

    @BeforeEach
    void setUp() throws IOException {
        method = new AtomicReference<>();
        requestBody = new AtomicReference<>();
        forwardedHeader = new AtomicReference<>();
        skippedHeader = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
            "/proxy",
            exchange -> {
                captureRequest(exchange);
                byte[] responseBody = "upstream response".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("X-Upstream", "ok");
                exchange.getResponseHeaders().add("Allow", "GET, POST");
                exchange.getResponseHeaders().add("Connection", "X-Upstream-Hop");
                exchange.getResponseHeaders().add("X-Upstream-Hop", "internal");
                exchange.sendResponseHeaders(201, responseBody.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(responseBody);
                }
            });
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/proxy";
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void forwardProxyRequestAndResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/proxy");
        request.setContentType("application/json");
        request.setContent("{\"name\":\"streampark\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Request-Id", "request-1");
        request.addHeader("X-Request-Hop", "internal");
        request.addHeader("Origin", "http://console.example");
        request.addHeader("Connection", "keep-alive, X-Request-Hop");
        MockHttpServletResponse response = new MockHttpServletResponse();

        WebUtils.http(endpoint, request, response);

        assertThat(method.get()).isEqualTo("POST");
        assertThat(requestBody.get()).isEqualTo("{\"name\":\"streampark\"}");
        assertThat(forwardedHeader.get()).isEqualTo("request-1");
        assertThat(skippedHeader.get()).isNull();
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeader("X-Upstream")).isEqualTo("ok");
        assertThat(response.getHeader("Allow")).isEqualTo("GET, POST");
        assertThat(response.getHeader("X-Upstream-Hop")).isNull();
        assertThat(response.getContentAsString()).isEqualTo("upstream response");
    }

    private void captureRequest(HttpExchange exchange) throws IOException {
        method.set(exchange.getRequestMethod());
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        forwardedHeader.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
        skippedHeader.set(exchange.getRequestHeaders().getFirst("X-Request-Hop"));
    }
}
