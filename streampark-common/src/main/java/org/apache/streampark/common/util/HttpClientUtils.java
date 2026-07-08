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

import org.apache.hc.client5.http.auth.AuthSchemeFactory;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.SPNegoSchemeFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Apache HttpClient 5 utility methods. */
public final class HttpClientUtils {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;

    static {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(5);
        CONNECTION_MANAGER = connectionManager;
    }

    private HttpClientUtils() {
    }

    private static CloseableHttpClient getHttpClient() {
        return HttpClients.custom().setConnectionManager(CONNECTION_MANAGER).build();
    }

    private static HttpGet getHttpGet(String url, Map<String, Object> params, RequestConfig config) {
        HttpGet httpGet;
        if (params == null) {
            httpGet = new HttpGet(url);
        } else {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setPath(url);
            uriBuilder.setParameters(paramsToNameValuePairs(params));
            try {
                httpGet = new HttpGet(uriBuilder.build());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (config != null) {
            httpGet.setConfig(config);
        }
        return httpGet;
    }

    public static String httpGetRequest(String url, RequestConfig config) {
        return getHttpResult(getHttpGet(url, null, config));
    }

    public static String httpGetRequest(
                                        String url, RequestConfig config, Map<String, Object> params) {
        return getHttpResult(getHttpGet(url, params, config));
    }

    public static String httpGetRequest(
                                        String url,
                                        RequestConfig config,
                                        Map<String, Object> headers,
                                        Map<String, Object> params) {
        HttpGet httpGet = getHttpGet(url, params, config);
        if (headers != null) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                httpGet.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return getHttpResult(httpGet);
    }

    public static String httpPostRequest(String url) {
        HttpPost httpPost = new HttpPost(url);
        return getHttpResult(httpPost);
    }

    public static String httpPostRequest(String url, Map<String, Object> params) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new UrlEncodedFormEntity(paramsToNameValuePairs(params), DEFAULT_CHARSET));
        return getHttpResult(httpPost);
    }

    public static String httpPostRequest(
                                         String url, Map<String, Object> params, Map<String, Object> headers) {
        return httpRequest(new HttpPost(url), headers, params);
    }

    private static String httpRequest(
                                      HttpUriRequestBase httpUri, Map<String, Object> headers,
                                      Map<String, Object> params) {
        if (headers != null) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                httpUri.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        httpUri.setEntity(new UrlEncodedFormEntity(paramsToNameValuePairs(params), DEFAULT_CHARSET));
        return getHttpResult(httpUri);
    }

    private static List<NameValuePair> paramsToNameValuePairs(Map<String, Object> params) {
        if (params == null) {
            return Collections.emptyList();
        }
        List<NameValuePair> pairs = new ArrayList<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            pairs.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
        }
        return pairs;
    }

    public static String httpAuthGetRequest(String url, RequestConfig config) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        Credentials credentials =
            new Credentials() {

                @Override
                public char[] getPassword() {
                    return null;
                }

                @Override
                public Principal getUserPrincipal() {
                    return null;
                }
            };
        credentialsProvider.setCredentials(new AuthScope(null, -1), credentials);

        RegistryBuilder<AuthSchemeFactory> authSchemeRegistry =
            RegistryBuilder.<AuthSchemeFactory>create()
                .register(StandardAuthScheme.SPNEGO, SPNegoSchemeFactory.DEFAULT);

        CloseableHttpClient httpAuthClient =
            HttpClientBuilder.create()
                .setDefaultAuthSchemeRegistry(authSchemeRegistry.build())
                .setDefaultCredentialsProvider(credentialsProvider)
                .setConnectionManager(CONNECTION_MANAGER)
                .build();

        return getHttpResult(getHttpGet(url, null, config), httpAuthClient);
    }

    private static String getHttpResult(HttpUriRequestBase request) {
        return getHttpResult(request, getHttpClient());
    }

    private static String getHttpResult(HttpUriRequestBase request, CloseableHttpClient httpClient) {
        try {
            return httpClient.execute(
                request,
                response -> {
                    if (response.getEntity() != null) {
                        return EntityUtils.toString(response.getEntity());
                    }
                    return null;
                });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
