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

package org.apache.streampark.console.core.managed.service;

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.CloudAccount;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProvider;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderRegistry;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ProviderContext;
import org.apache.streampark.console.core.mapper.CloudAccountMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolves Team-authorized, secret-free provider contexts. */
@Service
@RequiredArgsConstructor
class ManagedFlinkProviderContextService {

    private static final int STATUS_ENABLED = 1;

    private final CloudAccountAuthorizationService authorizationService;
    private final CloudAccountMapper cloudAccountMapper;
    private final ManagedFlinkProviderRegistry providerRegistry;
    private final ManagedFlinkFeatureGate featureGate;

    ManagedFlinkProviderSession resolve(Long teamId, Long accountId) {
        authorizationService.requireAuthorized(teamId, accountId);
        return resolveEnabledAccount(accountId, null, null);
    }

    ManagedFlinkProviderSession resolve(
                                        Long teamId,
                                        Long accountId,
                                        String providerConfigJson,
                                        Integer providerConfigVersion) {
        authorizationService.requireAuthorized(teamId, accountId);
        return resolveEnabledAccount(accountId, providerConfigJson, providerConfigVersion);
    }

    ManagedFlinkProviderSession resolveForSystem(
                                                 Long accountId,
                                                 String providerConfigJson,
                                                 Integer providerConfigVersion) {
        return resolveEnabledAccount(accountId, providerConfigJson, providerConfigVersion);
    }

    private ManagedFlinkProviderSession resolveEnabledAccount(
                                                              Long accountId,
                                                              String providerConfigJson,
                                                              Integer providerConfigVersion) {
        CloudAccount account = cloudAccountMapper.selectById(accountId);
        ApiAlertException.throwIfTrue(
            account == null
                || account.getStatus() == null
                || account.getStatus() != STATUS_ENABLED,
            "Managed Flink cloud account is no longer available.");
        ManagedFlinkProviderType providerType =
            ManagedFlinkProviderType.valueOf(account.getProviderType());
        featureGate.requireWriteEnabled(providerType);
        ManagedFlinkProvider provider;
        try {
            provider = providerRegistry.getRequired(providerType);
        } catch (IllegalArgumentException exception) {
            throw new ApiAlertException(
                "Managed Flink provider is not available in this deployment.");
        }
        ProviderContext context =
            ProviderContext.builder()
                .cloudAccountId(account.getId())
                .credentialVersion(account.getVersion().longValue())
                .region(account.getRegion())
                .endpoint(account.getEndpoint())
                .providerConfigJson(providerConfigJson)
                .providerConfigVersion(providerConfigVersion)
                .build();
        return new ManagedFlinkProviderSession(providerType, provider, context, account);
    }
}
