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

package org.apache.streampark.common.util;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Shared OkHttp client for StreamPark's outbound HTTP requests. */
public final class OkHttpUtils {

    private static final long CONNECT_TIMEOUT_SECONDS = 30L;
    private static final long READ_TIMEOUT_SECONDS = 60L;
    private static final long WRITE_TIMEOUT_SECONDS = 60L;
    private static final long CALL_TIMEOUT_SECONDS = 120L;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private static final Interceptor RETRY_INTERCEPTOR = newRetryInterceptor();
    private static final OkHttpClient HTTP_CLIENT = createClient();

    private OkHttpUtils() {
    }

    /**
     * Executes an HTTP request with the shared connection pool and retry policy.
     *
     * <p>The caller owns the returned response and must close it.
     *
     * @param request outbound request
     * @return open upstream response
     * @throws IOException when the request cannot be completed
     */
    public static Response call(Request request) throws IOException {
        return execute(request, HTTP_CLIENT);
    }

    private static Response execute(Request request, OkHttpClient client) throws IOException {
        try {
            return client.newCall(request).execute();
        } catch (IOException e) {
            throw new IOException(
                "Failed to execute HTTP request: " + request.method() + " " + request.url().encodedPath(),
                e);
        }
    }

    private static OkHttpClient createClient() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(256);
        dispatcher.setMaxRequestsPerHost(32);
        return new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(32, 5L, TimeUnit.MINUTES))
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(RETRY_INTERCEPTOR)
            .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .dispatcher(dispatcher)
            .build();
    }

    /** Retries transient failures only for methods whose requests are safe to replay. */
    private static Interceptor newRetryInterceptor() {
        return chain -> {
            Request request = chain.request();
            if (!isRetryableMethod(request.method())) {
                return chain.proceed(request);
            }

            Response response = null;
            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
                try {
                    response = chain.proceed(request);
                    if (response.isSuccessful()
                        || !isRetryableStatus(response.code())
                        || attempt == MAX_RETRY_ATTEMPTS) {
                        return response;
                    }
                    response.close();
                    response = null;
                } catch (IOException e) {
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        throw e;
                    }
                }

                try {
                    Thread.sleep((1L << attempt) * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while retrying HTTP request", e);
                }
            }
            throw new IOException("HTTP retry loop ended without a response");
        };
    }

    private static boolean isRetryableMethod(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }

    private static boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || (statusCode >= 500 && statusCode < 600);
    }
}
