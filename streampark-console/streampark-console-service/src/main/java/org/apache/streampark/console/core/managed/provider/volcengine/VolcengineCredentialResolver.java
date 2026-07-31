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
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ProviderContext;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;
import org.apache.streampark.console.core.managed.service.CredentialBinding;
import org.apache.streampark.console.core.managed.service.CredentialCryptoService;
import org.apache.streampark.console.core.managed.service.CredentialField;
import org.apache.streampark.console.core.managed.service.EncryptedCredential;
import org.apache.streampark.console.core.mapper.CloudAccountMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Resolves and decrypts one credential version immediately before an OpenAPI request. */
@Component
@RequiredArgsConstructor
class VolcengineCredentialResolver {

    private static final int STATUS_ENABLED = 1;

    private final CloudAccountMapper cloudAccountMapper;

    private final CredentialCryptoService credentialCryptoService;

    VolcengineCredentials resolve(ProviderContext context) {
        CloudAccount account = cloudAccountMapper.selectById(context.getCloudAccountId());
        if (account == null
            || !ManagedFlinkProviderType.VOLCENGINE.name().equals(account.getProviderType())
            || !Objects.equals(account.getStatus(), STATUS_ENABLED)
            || !Objects.equals(
                context.getCredentialVersion(), account.getVersion().longValue())) {
            throw providerException(
                ProviderErrorCategory.AUTHENTICATION, "CredentialVersionUnavailable");
        }
        try {
            String accessKey =
                credentialCryptoService.decrypt(
                    encrypted(account.getCredentialKeyVersion(), account.getAccessKeyCiphertext()),
                    binding(account, CredentialField.ACCESS_KEY));
            String secretKey =
                credentialCryptoService.decrypt(
                    encrypted(account.getCredentialKeyVersion(), account.getSecretKeyCiphertext()),
                    binding(account, CredentialField.SECRET_KEY));
            return new VolcengineCredentials(accessKey, secretKey);
        } catch (RuntimeException exception) {
            throw providerException(
                ProviderErrorCategory.PROVIDER_CONFIGURATION, "CredentialDecryptFailed");
        }
    }

    private static EncryptedCredential encrypted(int keyVersion, String ciphertext) {
        return new EncryptedCredential(keyVersion, ciphertext);
    }

    private static CredentialBinding binding(CloudAccount account, CredentialField field) {
        return new CredentialBinding(account.getProviderType(), account.getId(), field);
    }

    private static ManagedFlinkProviderException providerException(
                                                                   ProviderErrorCategory category,
                                                                   String code) {
        return new ManagedFlinkProviderException(
            category, code, null, "Volcengine credential is unavailable.");
    }
}
