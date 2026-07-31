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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Signed Volcengine Flink OpenAPI client with bounded read retries and per-account limits. */
@Component
public class VolcengineOpenApiClient {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private final VolcengineCredentialResolver credentialResolver;

    private final VolcengineRequestSigner signer;

    private final VolcengineProviderErrorMapper errorMapper;

    private final ObjectMapper objectMapper;

    private final VolcengineFlinkProperties properties;

    private final HttpClient httpClient;

    private final URI endpoint;

    private final Clock clock;

    private final Sleeper sleeper;

    private final VolcengineApiRateLimiter rateLimiter;

    @Autowired
    public VolcengineOpenApiClient(
                                   VolcengineCredentialResolver credentialResolver,
                                   VolcengineRequestSigner signer,
                                   VolcengineProviderErrorMapper errorMapper,
                                   ObjectMapper objectMapper,
                                   VolcengineFlinkProperties properties) {
        this(
            credentialResolver,
            signer,
            errorMapper,
            objectMapper,
            properties,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(),
            URI.create(properties.getEndpoint()),
            Clock.systemUTC(),
            Thread::sleep,
            new VolcengineApiRateLimiter(
                properties.getMaxConcurrentRequestsPerAccount(),
                properties.getMinRequestIntervalMs()));
    }

    VolcengineOpenApiClient(
                            VolcengineCredentialResolver credentialResolver,
                            VolcengineRequestSigner signer,
                            VolcengineProviderErrorMapper errorMapper,
                            ObjectMapper objectMapper,
                            VolcengineFlinkProperties properties,
                            HttpClient httpClient,
                            URI endpoint,
                            Clock clock,
                            Sleeper sleeper,
                            VolcengineApiRateLimiter rateLimiter) {
        validateEndpoint(endpoint);
        this.credentialResolver = credentialResolver;
        this.signer = signer;
        this.errorMapper = errorMapper;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.clock = clock;
        this.sleeper = sleeper;
        this.rateLimiter = rateLimiter;
    }

    VolcengineOpenApiResponse get(
                                  ProviderContext context,
                                  String action,
                                  String version,
                                  Map<String, String> parameters) {
        return request(
            context,
            "GET",
            action,
            version,
            parameters,
            new byte[0],
            VolcengineRequestSigner.CONTENT_TYPE,
            properties.getMaxReadRetries());
    }

    VolcengineOpenApiResponse post(
                                   ProviderContext context,
                                   String action,
                                   String version,
                                   Map<String, String> parameters,
                                   Object body) {
        byte[] content;
        try {
            content = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.VALIDATION,
                "InvalidRequestBody",
                null,
                "Unable to serialize Volcengine Flink request.");
        }
        return request(
            context,
            "POST",
            action,
            version,
            parameters,
            content,
            JSON_CONTENT_TYPE,
            properties.getMaxReadRetries());
    }

    VolcengineOpenApiResponse postOnce(
                                       ProviderContext context,
                                       String action,
                                       String version,
                                       Map<String, String> parameters,
                                       Object body) {
        byte[] content;
        try {
            content = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.VALIDATION,
                "InvalidRequestBody",
                null,
                "Unable to serialize Volcengine Flink request.");
        }
        return request(
            context,
            "POST",
            action,
            version,
            parameters,
            content,
            JSON_CONTENT_TYPE,
            0);
    }

    private VolcengineOpenApiResponse request(
                                              ProviderContext context,
                                              String method,
                                              String action,
                                              String version,
                                              Map<String, String> parameters,
                                              byte[] body,
                                              String contentType,
                                              int maxRetries) {
        validateContext(context);
        Map<String, String> query = new LinkedHashMap<>();
        query.put("Action", action);
        query.put("Version", version);
        query.putAll(parameters);

        try (VolcengineCredentials credentials = credentialResolver.resolve(context)) {
            ManagedFlinkProviderException lastFailure = null;
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                try {
                    return execute(
                        context,
                        method,
                        query,
                        body,
                        contentType,
                        credentials);
                } catch (ManagedFlinkProviderException exception) {
                    lastFailure = exception;
                    if (!exception.isRetryable()
                        || attempt >= maxRetries) {
                        throw exception;
                    }
                    sleep(retryDelayMillis(exception, attempt));
                }
            }
            throw lastFailure;
        }
    }

    private VolcengineOpenApiResponse execute(
                                              ProviderContext context,
                                              String method,
                                              Map<String, String> query,
                                              byte[] body,
                                              String contentType,
                                              VolcengineCredentials credentials) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        VolcengineRequestSigner.SignedRequest signed =
            signer.sign(
                method,
                endpoint,
                query,
                body,
                contentType,
                context.getRegion(),
                requestId,
                clock.instant(),
                credentials);
        URI requestUri = URI.create(endpoint.toString() + "?" + signed.getCanonicalQuery());
        HttpRequest.Builder request =
            HttpRequest.newBuilder(requestUri)
                .timeout(Duration.ofMillis(properties.getRequestTimeoutMs()));
        if ("POST".equals(method)) {
            request.POST(HttpRequest.BodyPublishers.ofByteArray(body));
        } else {
            request.GET();
        }
        signed.getHeaders().forEach(request::header);

        HttpResponse<String> response;
        try (
            VolcengineApiRateLimiter.Permit ignored =
                rateLimiter.acquire(context.getCloudAccountId())) {
            response =
                httpClient.send(
                    request.build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw errorMapper.networkFailure();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.TRANSIENT,
                "RequestInterrupted",
                null,
                "Volcengine Flink request was interrupted.");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.UNKNOWN,
                "InvalidResponse",
                header(response, "x-request-id"),
                "Volcengine Flink returned an invalid response.");
        }
        ManagedFlinkProviderException failure =
            errorMapper.fromResponse(
                response.statusCode(),
                root,
                header(response, "x-request-id"),
                retryAfterMillis(response));
        if (failure != null) {
            throw failure;
        }
        return new VolcengineOpenApiResponse(
            root,
            firstNonBlank(
                text(root.path("ResponseMetadata"), "RequestId", "RequestID"),
                header(response, "x-request-id"),
                requestId));
    }

    private long backoffMillis(int attempt) {
        long base =
            Math.min(
                properties.getMaxBackoffMs(),
                properties.getInitialBackoffMs() * (1L << Math.min(attempt, 20)));
        if (base <= 0) {
            return 0;
        }
        return Math.min(
            properties.getMaxBackoffMs(),
            base + ThreadLocalRandom.current().nextLong(base / 2 + 1));
    }

    private long retryDelayMillis(ManagedFlinkProviderException exception, int attempt) {
        Long retryAfterMillis = exception.getRetryAfterMillis();
        if (retryAfterMillis == null) {
            return backoffMillis(attempt);
        }
        return Math.min(properties.getMaxBackoffMs(), Math.max(0, retryAfterMillis));
    }

    private Long retryAfterMillis(HttpResponse<?> response) {
        String value = header(response, "Retry-After");
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds < 0 ? null : Math.multiplyExact(seconds, 1000L);
        } catch (ArithmeticException | NumberFormatException ignored) {
            try {
                return Math.max(
                    0,
                    Duration.between(
                        clock.instant(),
                        ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                            .toInstant())
                        .toMillis());
            } catch (DateTimeParseException dateTimeParseException) {
                return null;
            }
        }
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.TRANSIENT,
                "RetryInterrupted",
                null,
                "Volcengine Flink retry was interrupted.");
        }
    }

    private static void validateEndpoint(URI endpoint) {
        String scheme = endpoint.getScheme();
        String host = endpoint.getHost();
        boolean localTest =
            "http".equalsIgnoreCase(scheme)
                && ("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host));
        if (!(localTest
            || ("https".equalsIgnoreCase(scheme)
                && "open.volcengineapi.com".equalsIgnoreCase(host)))
            || (endpoint.getRawPath() != null
                && !endpoint.getRawPath().isEmpty()
                && !"/".equals(endpoint.getRawPath()))) {
            throw new IllegalArgumentException("Unsupported Volcengine OpenAPI endpoint");
        }
    }

    private static void validateContext(ProviderContext context) {
        if (context.getEndpoint() != null && !context.getEndpoint().trim().isEmpty()) {
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.PROVIDER_CONFIGURATION,
                "CustomEndpointDisabled",
                null,
                "Custom Volcengine Flink endpoints are disabled.");
        }
        if (context.getRegion() == null || context.getRegion().trim().isEmpty()) {
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.VALIDATION,
                "RegionRequired",
                null,
                "Volcengine Flink region is required.");
        }
    }

    private static String header(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).orElse(null);
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isValueNode() && !value.asText().trim().isEmpty()) {
                return value.asText().trim();
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

    interface Sleeper {

        void sleep(long millis) throws InterruptedException;
    }
}
