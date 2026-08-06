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

import com.volcengine.ApiClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VolcengineSdkClientFactoryTest {

    @Test
    void shouldCreateIsolatedClientAndDisableSdkRetry() {
        VolcengineFlinkProperties properties = new VolcengineFlinkProperties();
        properties.setConnectTimeoutMs(1234);
        properties.setRequestTimeoutMs(5678);
        VolcengineSdkClientFactory factory = new VolcengineSdkClientFactory(properties);
        VolcengineCredentials credentials = new VolcengineCredentials("ak", "sk");

        ApiClient client;
        try (
            VolcengineSdkClientFactory.Session session =
                factory.open(context(), credentials)) {
            client = session.api().getApiClient();
            assertThat(client.getEndpoint()).isEqualTo("open.volcengineapi.com");
            assertThat(client.getRegion()).isEqualTo("cn-beijing");
            assertThat(client.isAutoRetry()).isFalse();
            assertThat(client.getNumMaxRetries()).isZero();
            assertThat(client.getConnectTimeout()).isEqualTo(1234);
            assertThat(client.getReadTimeout()).isEqualTo(5678);
            assertThat(client.getWriteTimeout()).isEqualTo(5678);
            assertThat(client.getCredentialProvider()).isNull();
            assertThat(client.getCredentials().getAccessKey()).isEqualTo("ak");
        }

        assertThat(client.getCredentials()).isNull();
        assertThat(credentials.accessKey()).containsOnly('\0');
        assertThat(credentials.secretKey()).containsOnly('\0');
    }

    @Test
    void shouldRejectCustomOrNonOfficialEndpoints() {
        VolcengineFlinkProperties properties = new VolcengineFlinkProperties();
        VolcengineSdkClientFactory factory = new VolcengineSdkClientFactory(properties);
        ProviderContext custom =
            ProviderContext.builder()
                .cloudAccountId(1L)
                .credentialVersion(1L)
                .region("cn-beijing")
                .endpoint("https://example.com")
                .build();

        assertThatThrownBy(
            () -> factory.open(custom, new VolcengineCredentials("ak", "sk")))
                .isInstanceOf(ManagedFlinkProviderException.class)
                .hasMessage("Volcengine SDK client configuration is invalid.");

        properties.setEndpoint("https://example.com");
        assertThatThrownBy(
            () -> factory.open(context(), new VolcengineCredentials("ak", "sk")))
                .isInstanceOf(ManagedFlinkProviderException.class)
                .hasMessage("Volcengine SDK client configuration is invalid.");
    }

    private static ProviderContext context() {
        return ProviderContext.builder()
            .cloudAccountId(1L)
            .credentialVersion(1L)
            .region("cn-beijing")
            .providerConfigJson("{\"fixture\":true}")
            .providerConfigVersion(1)
            .build();
    }
}
