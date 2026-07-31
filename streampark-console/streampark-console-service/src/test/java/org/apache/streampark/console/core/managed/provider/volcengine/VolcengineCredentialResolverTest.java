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

import org.apache.streampark.console.core.entity.CloudAccount;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ProviderContext;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;
import org.apache.streampark.console.core.managed.service.CredentialBinding;
import org.apache.streampark.console.core.managed.service.CredentialCryptoService;
import org.apache.streampark.console.core.managed.service.EncryptedCredential;
import org.apache.streampark.console.core.mapper.CloudAccountMapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VolcengineCredentialResolverTest {

    @Test
    void shouldDecryptCurrentEnabledCredentialWithoutCachingIt() {
        CloudAccountMapper mapper = mock(CloudAccountMapper.class);
        CredentialCryptoService cryptoService = mock(CredentialCryptoService.class);
        when(mapper.selectById(1L)).thenReturn(account());
        when(cryptoService.decrypt(any(EncryptedCredential.class), any(CredentialBinding.class)))
            .thenReturn("AKLT-test", "secret-test");
        VolcengineCredentialResolver resolver =
            new VolcengineCredentialResolver(mapper, cryptoService);

        VolcengineCredentials credentials = resolver.resolve(context(2L));

        assertThat(credentials.accessKey()).containsExactly("AKLT-test".toCharArray());
        assertThat(credentials.secretKey()).containsExactly("secret-test".toCharArray());
        verify(cryptoService, org.mockito.Mockito.times(2))
            .decrypt(any(EncryptedCredential.class), any(CredentialBinding.class));
        credentials.close();
    }

    @Test
    void shouldRejectStaleCredentialVersionBeforeDecrypting() {
        CloudAccountMapper mapper = mock(CloudAccountMapper.class);
        CredentialCryptoService cryptoService = mock(CredentialCryptoService.class);
        when(mapper.selectById(1L)).thenReturn(account());
        VolcengineCredentialResolver resolver =
            new VolcengineCredentialResolver(mapper, cryptoService);

        assertThatThrownBy(() -> resolver.resolve(context(1L)))
            .isInstanceOfSatisfying(
                ManagedFlinkProviderException.class,
                failure -> assertThat(failure.getCategory())
                    .isEqualTo(ProviderErrorCategory.AUTHENTICATION));
        verify(cryptoService, never())
            .decrypt(any(EncryptedCredential.class), any(CredentialBinding.class));
    }

    @Test
    void shouldConvertCryptoFailureToSafeProviderConfigurationError() {
        CloudAccountMapper mapper = mock(CloudAccountMapper.class);
        CredentialCryptoService cryptoService = mock(CredentialCryptoService.class);
        when(mapper.selectById(1L)).thenReturn(account());
        when(cryptoService.decrypt(any(EncryptedCredential.class), any(CredentialBinding.class)))
            .thenThrow(new IllegalStateException("raw key detail"));
        VolcengineCredentialResolver resolver =
            new VolcengineCredentialResolver(mapper, cryptoService);

        assertThatThrownBy(() -> resolver.resolve(context(2L)))
            .isInstanceOfSatisfying(
                ManagedFlinkProviderException.class,
                failure -> {
                    assertThat(failure.getCategory())
                        .isEqualTo(ProviderErrorCategory.PROVIDER_CONFIGURATION);
                    assertThat(failure.getProviderCode()).isEqualTo("CredentialDecryptFailed");
                    assertThat(failure.getMessage()).doesNotContain("raw key detail");
                });
    }

    private static CloudAccount account() {
        CloudAccount account = new CloudAccount();
        account.setId(1L);
        account.setProviderType("VOLCENGINE");
        account.setStatus(1);
        account.setVersion(2);
        account.setCredentialKeyVersion(1);
        account.setAccessKeyCiphertext("ciphertext-ak");
        account.setSecretKeyCiphertext("ciphertext-sk");
        return account;
    }

    private static ProviderContext context(long version) {
        return ProviderContext.builder()
            .cloudAccountId(1L)
            .credentialVersion(version)
            .region("cn-beijing")
            .build();
    }
}
