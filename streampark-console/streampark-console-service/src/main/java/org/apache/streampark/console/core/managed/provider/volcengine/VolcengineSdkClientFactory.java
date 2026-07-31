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

import com.volcengine.ApiClient;
import com.volcengine.flink20250101.Flink20250101Api;
import com.volcengine.sign.Credentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;

/** Creates isolated SDK clients with explicit credentials and write retries disabled. */
@Component
@RequiredArgsConstructor
class VolcengineSdkClientFactory {

    private static final String OFFICIAL_HOST = "open.volcengineapi.com";

    private final VolcengineFlinkProperties properties;

    Session open(ProviderContext context, VolcengineCredentials plaintext) {
        validate(context);
        URI endpoint = URI.create(properties.getEndpoint());
        ApiClient client =
            new ApiClient()
                .setEndpoint(endpoint.getHost())
                .setRegion(context.getRegion())
                .setCredentials(
                    Credentials.getCredentials(
                        new String(plaintext.accessKey()), new String(plaintext.secretKey())))
                .setAutoRetry(false)
                .setNumMaxRetries(0)
                .setConnectTimeout(properties.getConnectTimeoutMs())
                .setReadTimeout(properties.getRequestTimeoutMs())
                .setWriteTimeout(properties.getRequestTimeoutMs());
        return new Session(new Flink20250101Api(client), plaintext);
    }

    private void validate(ProviderContext context) {
        if (context.getEndpoint() != null && !context.getEndpoint().trim().isEmpty()) {
            throw configuration("CustomEndpointDisabled");
        }
        if (context.getRegion() == null || context.getRegion().trim().isEmpty()) {
            throw new ManagedFlinkProviderException(
                ProviderErrorCategory.VALIDATION,
                "RegionRequired",
                null,
                "Volcengine Flink region is required.");
        }
        URI endpoint = URI.create(properties.getEndpoint());
        if (!"https".equalsIgnoreCase(endpoint.getScheme())
            || !OFFICIAL_HOST.equalsIgnoreCase(endpoint.getHost())
            || endpoint.getUserInfo() != null
            || endpoint.getQuery() != null
            || endpoint.getFragment() != null
            || (endpoint.getPath() != null
                && !endpoint.getPath().isEmpty()
                && !"/".equals(endpoint.getPath()))) {
            throw configuration("UnsupportedEndpoint");
        }
    }

    private static ManagedFlinkProviderException configuration(String code) {
        return new ManagedFlinkProviderException(
            ProviderErrorCategory.PROVIDER_CONFIGURATION,
            code,
            null,
            "Volcengine SDK client configuration is invalid.");
    }

    static final class Session implements AutoCloseable {

        private final Flink20250101Api api;

        private final VolcengineCredentials plaintext;

        private Session(Flink20250101Api api, VolcengineCredentials plaintext) {
            this.api = api;
            this.plaintext = plaintext;
        }

        Flink20250101Api api() {
            return api;
        }

        @Override
        public void close() {
            api.getApiClient().setCredentials(null);
            plaintext.close();
        }
    }
}
