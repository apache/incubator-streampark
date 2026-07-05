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

package org.apache.streampark.flink.connector.elasticsearch6.bean;

import org.apache.streampark.flink.connector.elasticsearch6.conf.ES6Config;

import org.apache.flink.streaming.connectors.elasticsearch6.RestClientFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestClientFactoryImpl implements RestClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(RestClientFactoryImpl.class);
    public final ES6Config config;

    public RestClientFactoryImpl(ES6Config config) {
        this.config = config;
    }

    @Override
    public void configureRestClientBuilder(RestClientBuilder restClientBuilder) {
        String userName = config.userName;
        String password = config.password;
        if (!((userName != null && password != null) || (userName == null && password == null))) {
            throw new IllegalArgumentException(
                "[StreamPark] elasticsearch auth info error,userName,password must be all set,or all not set.");
        }
        CredentialsProvider credentialsProvider = null;
        if (userName != null) {
            credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
        }
        final CredentialsProvider provider = credentialsProvider;
        restClientBuilder.setHttpClientConfigCallback((HttpAsyncClientBuilder httpClientBuilder) -> {
            if (provider != null) {
                httpClientBuilder.setDefaultCredentialsProvider(provider);
                LOG.info("Elasticsearch auth by userName,password...");
            }
            return httpClientBuilder;
        });
        restClientBuilder.setRequestConfigCallback((RequestConfig.Builder requestConfigBuilder) -> {
            if (provider != null) {
                requestConfigBuilder.setAuthenticationEnabled(true);
            }
            requestConfigBuilder.setConnectionRequestTimeout(config.connectRequestTimeout);
            requestConfigBuilder.setConnectTimeout(config.connectTimeout);
            requestConfigBuilder.setMaxRedirects(config.maxRedirects);
            requestConfigBuilder.setRedirectsEnabled(config.redirectsEnabled);
            requestConfigBuilder.setConnectTimeout(config.socketTimeout);
            requestConfigBuilder.setRelativeRedirectsAllowed(config.relativeRedirectsAllowed);
            requestConfigBuilder.setContentCompressionEnabled(config.contentCompressionEnabled);
            requestConfigBuilder.setNormalizeUri(config.normalizeUri);
            return requestConfigBuilder;
        });
        restClientBuilder.setDefaultHeaders(new BasicHeader[] {new BasicHeader("Content-Type", config.contentType)});
        if (config.maxRetry > 0) {
            restClientBuilder.setMaxRetryTimeoutMillis(config.maxRetry);
        }
        if (config.pathPrefix != null) {
            restClientBuilder.setPathPrefix(config.pathPrefix);
        }
    }
}
