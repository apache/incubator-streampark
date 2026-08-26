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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** HTTP client for StreamPark REST API integration tests. */
@Slf4j
@Getter
public final class ApiClient {

    private static final String ADMIN = "admin";
    private static final String PASSWORD = "streampark";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String token;
    private Long teamId;
    private Long userId;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
    }

    public ApiResponse login() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("username", ADMIN);
        params.put("password", PASSWORD);
        ApiResponse response = postForm("/passport/signin", params, false);
        if (!response.isSuccess() || response.getData() == null) {
            throw new IllegalStateException("Login failed: " + response.text());
        }
        JsonNode data = response.getData();
        this.token = data.path("token").asText(null);
        JsonNode user = data.path("user");
        if (!user.isMissingNode()) {
            this.userId = user.path("userId").isNull() ? null : user.path("userId").asLong();
            this.teamId =
                user.path("lastTeamId").isNull() ? null : user.path("lastTeamId").asLong();
        }
        if (token == null || teamId == null) {
            throw new IllegalStateException("Login response missing token or teamId: " + response.text());
        }
        return response;
    }

    public ApiResponse postForm(String path, Map<String, String> params) {
        return postForm(path, params, true);
    }

    public ApiResponse postForm(String path, Map<String, String> params, boolean withAuth) {
        return exchange("POST", path, "application/x-www-form-urlencoded", encodeForm(params), withAuth);
    }

    public ApiResponse postJson(String path, String jsonBody) {
        return exchange("POST", path, "application/json", jsonBody, true);
    }

    public ApiResponse putForm(String path, Map<String, String> params) {
        return exchange("PUT", path, "application/x-www-form-urlencoded", encodeForm(params), true);
    }

    public ApiResponse deleteForm(String path, Map<String, String> params) {
        return exchange("DELETE", path, "application/x-www-form-urlencoded", encodeForm(params), true);
    }

    public Map<String, String> teamParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("teamId", String.valueOf(teamId));
        return params;
    }

    public Map<String, String> params(String... keyValues) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            params.put(keyValues[i], keyValues[i + 1]);
        }
        return params;
    }

    public ObjectNode objectNode() {
        return objectMapper.createObjectNode();
    }

    public String writeJson(ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    public Optional<Long> findRecordId(ArrayNode records, String field, String expected) {
        for (JsonNode record : records) {
            if (expected.equals(record.path(field).asText())) {
                return Optional.of(record.path("id").asLong());
            }
        }
        return Optional.empty();
    }

    public Optional<Long> findUserIdByUsername(String username) {
        ApiResponse response = postForm("/user/list", params("pageNum", "1", "pageSize", "100"));
        return findInPageRecords(response, "username", username).map(node -> node.path("userId").asLong());
    }

    public Optional<Long> findRoleIdByName(String roleName) {
        ApiResponse response = postForm("/role/list", params("pageNum", "1", "pageSize", "100"));
        return findInPageRecords(response, "roleName", roleName).map(node -> node.path("roleId").asLong());
    }

    public Optional<String> findMenuIdByTitle(String title) {
        ApiResponse response = postForm("/menu/list", new HashMap<>());
        if (!response.isSuccess() || response.getData() == null) {
            return Optional.empty();
        }
        return findMenuIdRecursive(response.getData(), title);
    }

    public Optional<JsonNode> findInPageRecords(ApiResponse response, String field, String expected) {
        JsonNode data = response.getData();
        if (data == null || !data.has("records")) {
            return Optional.empty();
        }
        for (JsonNode record : data.get("records")) {
            if (expected.equals(record.path(field).asText())) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    public boolean pageRecordsContain(ApiResponse response, String field, String expected) {
        return findInPageRecords(response, field, expected).isPresent();
    }

    public boolean listContains(ApiResponse response, String field, String expected) {
        JsonNode data = response.getData();
        if (data == null || !data.isArray()) {
            return false;
        }
        for (JsonNode item : data) {
            if (expected.equals(item.path(field).asText())) {
                return true;
            }
        }
        return false;
    }

    private Optional<String> findMenuIdRecursive(JsonNode menus, String title) {
        if (menus == null) {
            return Optional.empty();
        }
        if (menus.isArray()) {
            for (JsonNode menu : menus) {
                Optional<String> found = findMenuIdRecursive(menu, title);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }
        if (title.equals(menus.path("title").asText()) || title.equals(menus.path("menuName").asText())) {
            return Optional.of(String.valueOf(menus.path("menuId").asLong()));
        }
        Iterator<Map.Entry<String, JsonNode>> fields = menus.fields();
        while (fields.hasNext()) {
            JsonNode child = fields.next().getValue();
            Optional<String> found = findMenuIdRecursive(child, title);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private ApiResponse exchange(
                                 String method, String path, String contentType, String body, boolean withAuth) {
        try {
            HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", contentType);
            if (withAuth) {
                Objects.requireNonNull(token, "Call login() before authenticated requests");
                builder.header("Authorization", token);
            }
            builder.method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> httpResponse =
                httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = parseJson(httpResponse.body());
            log.debug("{} {} -> {} {}", method, path, httpResponse.statusCode(), httpResponse.body());
            return new ApiResponse(httpResponse.statusCode(), root);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request interrupted: " + path, e);
        } catch (IOException e) {
            throw new IllegalStateException("HTTP request failed: " + path, e);
        }
    }

    private JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            throw new IllegalStateException("Invalid JSON response: " + body, e);
        }
    }

    private static String encodeForm(Map<String, String> params) {
        return params.entrySet().stream()
            .map(
                entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue() == null ? "" : entry.getValue()))
            .collect(Collectors.joining("&"));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
