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

import org.apache.streampark.common.configuration.option.CoreOptions;
import org.apache.streampark.common.util.OkHttpUtils;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.toolkit.StringPool;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

/** Common web-path helpers and servlet proxy support for the Console. */
public final class WebUtils {

    /** Inbound connection headers and metadata reconstructed by OkHttp. */
    private static final Set<String> REQUEST_SKIP_HEADERS =
        Collections.unmodifiableSet(
            new HashSet<>(
                Arrays.asList(
                    "connection",
                    "keep-alive",
                    "proxy-authenticate",
                    "proxy-authorization",
                    "te",
                    "trailer",
                    "transfer-encoding",
                    "upgrade",
                    "content-length")));

    private static final Set<String> RESPONSE_SKIP_HEADERS =
        Collections.unmodifiableSet(
            new HashSet<>(
                Arrays.asList(
                    "connection",
                    "keep-alive",
                    "proxy-authenticate",
                    "proxy-authorization",
                    "te",
                    "trailer",
                    "transfer-encoding",
                    "upgrade",
                    "content-length")));

    private static final String TEMP = "temp";
    private static final String LIB = "lib";
    private static final String PLUGINS = "plugins";
    private static final String CLIENT = "client";
    private WebUtils() {
    }

    /**
     * camel to underscore
     *
     * @param value value
     * @return underscore
     */
    public static String camelToUnderscore(String value) {
        if (StringUtils.isBlank(value) || value.contains("_")) {
            return value;
        }
        String[] arr = StringUtils.splitByCharacterTypeCamelCase(value);
        if (arr.length == 0) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        IntStream.range(0, arr.length)
            .forEach(
                i -> {
                    if (i != arr.length - 1) {
                        result.append(arr[i]).append(StringPool.UNDERSCORE);
                    } else {
                        result.append(arr[i]);
                    }
                });
        return StringUtils.lowerCase(result.toString());
    }

    public static String getAppHome() {
        return System.getProperty(CoreOptions.APP_HOME.key());
    }

    public static File getAppDir(String dir) {
        return new File(getAppHome(), dir);
    }

    public static File getAppTempDir() {
        return getAppDir(TEMP);
    }

    public static File getAppLibDir() {
        return getAppDir(LIB);
    }

    public static File getAppClientDir() {
        return getAppDir(CLIENT);
    }

    public static File getPluginDir() {
        return getAppDir(PLUGINS);
    }

    /** Proxies a request and streams the upstream response to the servlet response. */
    public static void http(
                            String url,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        Headers.Builder headersBuilder = new Headers.Builder();
        Set<String> skippedHeaders =
            skippedHeaders(request.getHeader("Connection"), REQUEST_SKIP_HEADERS);
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String normalizedName = headerName.toLowerCase(Locale.ROOT);
                if ("referer".equals(normalizedName)
                    || "origin".equals(normalizedName)
                    || "host".equals(normalizedName)
                    || skippedHeaders.contains(normalizedName)
                    || normalizedName.startsWith("access-control-")) {
                    continue;
                }
                Enumeration<String> values = request.getHeaders(headerName);
                if (values == null) {
                    continue;
                }
                while (values.hasMoreElements()) {
                    String value = values.nextElement();
                    if (value != null) {
                        headersBuilder.add(headerName, value);
                    }
                }
            }
        }

        Request.Builder requestBuilder =
            new Request.Builder().url(url).headers(headersBuilder.build());
        // OkHttp derives Host and Content-Length from the upstream URL and replayable request body.
        String method = request.getMethod();
        if (HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method)) {
            requestBuilder.method(method, null);
        } else {
            byte[] content = org.apache.commons.io.IOUtils.toByteArray(request.getInputStream());
            MediaType contentType =
                StringUtils.isNotBlank(request.getContentType())
                    ? MediaType.parse(request.getContentType())
                    : null;
            requestBuilder.method(method, RequestBody.create(content, contentType));
        }

        try (Response upstream = OkHttpUtils.call(requestBuilder.build())) {
            writeProxyResponse(upstream, response);
        }
    }

    private static void writeProxyResponse(
                                           Response upstream, HttpServletResponse downstream) throws IOException {
        copyProxyResponseHeaders(upstream, downstream);
        downstream.setStatus(upstream.code());
        downstream.setHeader("Access-Control-Allow-Origin", "*");
        if (HttpMethod.HEAD.matches(upstream.request().method())) {
            return;
        }
        ResponseBody body = upstream.body();
        if (body != null) {
            org.apache.commons.io.IOUtils.copy(body.byteStream(), downstream.getOutputStream());
        }
    }

    private static void copyProxyResponseHeaders(
                                                 Response upstream,
                                                 HttpServletResponse downstream) {
        Set<String> skippedHeaders =
            skippedHeaders(upstream.header("Connection"), RESPONSE_SKIP_HEADERS);
        upstream.headers().forEach(
            header -> {
                String normalizedName = header.getFirst().toLowerCase(Locale.ROOT);
                if (!normalizedName.startsWith("access-control-")
                    && !skippedHeaders.contains(normalizedName)) {
                    downstream.addHeader(header.getFirst(), header.getSecond());
                }
            });
    }

    /** Adds header names declared by {@code Connection} to the fixed exclusion set. */
    private static Set<String> skippedHeaders(String connection, Set<String> fixedHeaders) {
        Set<String> result = new HashSet<>(fixedHeaders);
        if (connection != null) {
            Arrays.stream(connection.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .forEach(result::add);
        }
        return result;
    }

}
