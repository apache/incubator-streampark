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
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.entity.CloudAccount;
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.managed.api.CredentialCheckResult;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProvider;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderRegistry;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ProviderContext;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;
import org.apache.streampark.console.core.managed.model.CloudAccountCreateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountPageRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountUpdateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountVersionedIdRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountView;
import org.apache.streampark.console.core.mapper.CloudAccountMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Default encrypted cloud account service. */
@Service
@RequiredArgsConstructor
public class CloudAccountServiceImpl implements CloudAccountService {

    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;
    private static final int CONNECTIVITY_UNTESTED = 0;
    private static final int CONNECTIVITY_CONNECTED = 1;
    private static final int CONNECTIVITY_FAILED = 2;
    private static final Set<String> ALLOWED_SORT_FIELDS =
        Set.of(
            "accountName",
            "providerType",
            "status",
            "createTime",
            "modifyTime",
            "create_time",
            "modify_time");

    private final CloudAccountMapper cloudAccountMapper;
    private final ManagedFlinkEnvironmentMapper managedFlinkEnvironmentMapper;
    private final CredentialCryptoService credentialCryptoService;
    private final ManagedFlinkFeatureGate featureGate;
    private final ManagedFlinkAuditContext auditContext;
    private final ManagedFlinkProviderRegistry providerRegistry;
    private final TransactionTemplate transactionTemplate;

    @Override
    public IPage<CloudAccountView> page(CloudAccountPageRequest request) {
        validateSortField(request);
        Page<CloudAccount> page = MybatisPager.getPage(request);
        String providerType = normalizeOptionalProviderType(request.getProviderType());
        IPage<CloudAccount> result = cloudAccountMapper.selectPage(
            page,
            new LambdaQueryWrapper<CloudAccount>()
                .like(
                    StringUtils.isNotBlank(request.getAccountName()),
                    CloudAccount::getAccountName,
                    StringUtils.trim(request.getAccountName()))
                .eq(
                    providerType != null,
                    CloudAccount::getProviderType,
                    providerType)
                .eq(
                    request.getStatus() != null,
                    CloudAccount::getStatus,
                    request.getStatus()));
        return result.convert(CloudAccountModelMapper::toView);
    }

    @Override
    public CloudAccountView test(CloudAccountVersionedIdRequest request) {
        CloudAccount account = requireAccount(request.getId());
        ManagedFlinkProviderType providerType = parseProviderType(account.getProviderType());
        featureGate.requireWriteEnabled(providerType);
        ApiAlertException.throwIfFalse(
            Objects.equals(account.getStatus(), STATUS_ENABLED),
            "Disabled cloud accounts cannot be tested.");
        ApiAlertException.throwIfFalse(
            Objects.equals(account.getVersion(), request.getVersion()),
            "Cloud account was modified by another request. Refresh and retry.");

        ProviderContext context =
            ProviderContext.builder()
                .cloudAccountId(account.getId())
                .credentialVersion(account.getVersion().longValue())
                .region(account.getRegion())
                .endpoint(account.getEndpoint())
                .build();
        String providerRequestId = null;
        try {
            ManagedFlinkProvider provider = providerRegistry.getRequired(providerType);
            CredentialCheckResult result = provider.validateCredential(context);
            providerRequestId = result == null ? null : result.getProviderRequestId();
            if (result == null || !result.isSuccess()) {
                persistConnectivity(
                    request,
                    CONNECTIVITY_FAILED,
                    ProviderErrorCategory.AUTHENTICATION,
                    "CredentialRejected");
            } else {
                persistConnectivity(request, CONNECTIVITY_CONNECTED, null, null);
            }
        } catch (ManagedFlinkProviderException exception) {
            providerRequestId = exception.getProviderRequestId();
            persistConnectivity(
                request,
                CONNECTIVITY_FAILED,
                exception.getCategory(),
                exception.getProviderCode());
        }
        return get(request.getId()).toBuilder()
            .providerRequestId(providerRequestId)
            .build();
    }

    @Override
    public CloudAccountView get(Long id) {
        return CloudAccountModelMapper.toView(requireAccount(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CloudAccountCreateRequest request) {
        ManagedFlinkProviderType providerType = parseProviderType(request.getProviderType());
        featureGate.requireWriteEnabled(providerType);
        rejectCustomEndpoint(request.getEndpoint());
        ensureNameUnique(providerType.name(), request.getAccountName(), null);

        long accountId = IdWorker.getId();
        EncryptedCredential encryptedAccessKey = credentialCryptoService.encrypt(
            request.getAccessKey(),
            binding(providerType.name(), accountId, CredentialField.ACCESS_KEY));
        EncryptedCredential encryptedSecretKey = credentialCryptoService.encrypt(
            request.getSecretKey(),
            binding(providerType.name(), accountId, CredentialField.SECRET_KEY));
        ApiAlertException.throwIfFalse(
            encryptedAccessKey.getKeyVersion() == encryptedSecretKey.getKeyVersion(),
            "Credential master key changed while creating the cloud account.");

        CloudAccount account = new CloudAccount();
        account.setId(accountId);
        account.setAccountName(request.getAccountName().trim());
        account.setProviderType(providerType.name());
        account.setRegion(request.getRegion().trim());
        account.setEndpoint(null);
        account.setAccessKeyCiphertext(encryptedAccessKey.getCiphertext());
        account.setSecretKeyCiphertext(encryptedSecretKey.getCiphertext());
        account.setCredentialKeyVersion(encryptedAccessKey.getKeyVersion());
        account.setAccessKeyMask(maskAccessKey(request.getAccessKey()));
        account.setConnectivityState(CONNECTIVITY_UNTESTED);
        account.setStatus(STATUS_ENABLED);
        account.setDescription(StringUtils.trimToNull(request.getDescription()));
        account.setCreateUserId(auditContext.currentUserId());
        account.setVersion(0);

        ApiAlertException.throwIfFalse(
            cloudAccountMapper.insert(account) == 1, "Failed to create the cloud account.");
        return accountId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CloudAccountUpdateRequest request) {
        CloudAccount existing = requireAccount(request.getId());
        ManagedFlinkProviderType providerType = parseProviderType(existing.getProviderType());
        featureGate.requireWriteEnabled(providerType);
        rejectCustomEndpoint(request.getEndpoint());
        ensureNameUnique(providerType.name(), request.getAccountName(), request.getId());

        boolean accessKeyProvided = StringUtils.isNotBlank(request.getAccessKey());
        boolean secretKeyProvided = StringUtils.isNotBlank(request.getSecretKey());
        boolean connectivityContextChanged =
            accessKeyProvided || !Objects.equals(existing.getRegion(), request.getRegion().trim());
        ApiAlertException.throwIfFalse(
            accessKeyProvided == secretKeyProvided,
            "Access key and secret key must be updated together.");

        CloudAccount changes = new CloudAccount();
        changes.setAccountName(request.getAccountName().trim());
        changes.setRegion(request.getRegion().trim());
        changes.setEndpoint(null);
        changes.setVersion(request.getVersion() + 1);

        if (accessKeyProvided) {
            EncryptedCredential encryptedAccessKey = credentialCryptoService.encrypt(
                request.getAccessKey(),
                binding(existing.getProviderType(), existing.getId(), CredentialField.ACCESS_KEY));
            EncryptedCredential encryptedSecretKey = credentialCryptoService.encrypt(
                request.getSecretKey(),
                binding(existing.getProviderType(), existing.getId(), CredentialField.SECRET_KEY));
            ApiAlertException.throwIfFalse(
                encryptedAccessKey.getKeyVersion() == encryptedSecretKey.getKeyVersion(),
                "Credential master key changed while updating the cloud account.");
            changes.setAccessKeyCiphertext(encryptedAccessKey.getCiphertext());
            changes.setSecretKeyCiphertext(encryptedSecretKey.getCiphertext());
            changes.setCredentialKeyVersion(encryptedAccessKey.getKeyVersion());
            changes.setAccessKeyMask(maskAccessKey(request.getAccessKey()));
        }
        if (connectivityContextChanged) {
            changes.setConnectivityState(CONNECTIVITY_UNTESTED);
            changes.setLastCheckTime(null);
            changes.setLastErrorCode(null);
            changes.setLastErrorMessage(null);
        }

        LambdaUpdateWrapper<CloudAccount> updateWrapper =
            new LambdaUpdateWrapper<CloudAccount>()
                .eq(CloudAccount::getId, request.getId())
                .eq(CloudAccount::getVersion, request.getVersion())
                .set(CloudAccount::getEndpoint, null)
                .set(
                    CloudAccount::getDescription,
                    StringUtils.trimToNull(request.getDescription()));
        if (connectivityContextChanged) {
            updateWrapper
                .set(CloudAccount::getLastCheckTime, null)
                .set(CloudAccount::getLastErrorCode, null)
                .set(CloudAccount::getLastErrorMessage, null);
        }
        int updated = cloudAccountMapper.update(changes, updateWrapper);
        requireUpdated(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(CloudAccountVersionedIdRequest request) {
        CloudAccount existing = requireAccount(request.getId());
        featureGate.requireWriteEnabled(parseProviderType(existing.getProviderType()));
        if (Objects.equals(existing.getStatus(), STATUS_DISABLED)) {
            return;
        }

        int updated = cloudAccountMapper.update(
            null,
            new LambdaUpdateWrapper<CloudAccount>()
                .eq(CloudAccount::getId, request.getId())
                .eq(CloudAccount::getVersion, request.getVersion())
                .set(CloudAccount::getStatus, STATUS_DISABLED)
                .set(CloudAccount::getVersion, request.getVersion() + 1));
        requireUpdated(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(CloudAccountVersionedIdRequest request) {
        CloudAccount existing = cloudAccountMapper.selectById(request.getId());
        if (existing == null) {
            return;
        }
        featureGate.requireWriteEnabled(parseProviderType(existing.getProviderType()));
        Long environmentCount = managedFlinkEnvironmentMapper.selectCount(
            new LambdaQueryWrapper<ManagedFlinkEnvironment>()
                .eq(ManagedFlinkEnvironment::getCloudAccountId, request.getId()));
        ApiAlertException.throwIfTrue(
            environmentCount != null && environmentCount > 0,
            "Cloud account is referenced by managed Flink environments and cannot be deleted.");
        ApiAlertException.throwIfFalse(
            Objects.equals(existing.getVersion(), request.getVersion()),
            "Cloud account was modified by another request. Refresh and retry.");

        int deleted = cloudAccountMapper.delete(
            new LambdaQueryWrapper<CloudAccount>()
                .eq(CloudAccount::getId, request.getId())
                .eq(CloudAccount::getVersion, request.getVersion()));
        ApiAlertException.throwIfFalse(
            deleted == 1, "Cloud account was modified by another request. Refresh and retry.");
    }

    private CloudAccount requireAccount(Long id) {
        ApiAlertException.throwIfNull(id, "Cloud account id is required.");
        CloudAccount account = cloudAccountMapper.selectById(id);
        ApiAlertException.throwIfNull(account, "Cloud account does not exist.");
        return account;
    }

    private void ensureNameUnique(String providerType, String accountName, Long excludedId) {
        Long count = cloudAccountMapper.selectCount(
            new LambdaQueryWrapper<CloudAccount>()
                .eq(CloudAccount::getProviderType, providerType)
                .eq(CloudAccount::getAccountName, accountName.trim())
                .ne(excludedId != null, CloudAccount::getId, excludedId));
        ApiAlertException.throwIfTrue(
            count != null && count > 0,
            "Cloud account name already exists for provider " + providerType + ".");
    }

    private static void validateSortField(CloudAccountPageRequest request) {
        ApiAlertException.throwIfFalse(
            ALLOWED_SORT_FIELDS.contains(request.getSortField()),
            "Unsupported cloud account sort field: " + request.getSortField());
    }

    private static String normalizeOptionalProviderType(String providerType) {
        return StringUtils.isBlank(providerType)
            ? null
            : parseProviderType(providerType).name();
    }

    private static ManagedFlinkProviderType parseProviderType(String providerType) {
        try {
            return ManagedFlinkProviderType.valueOf(providerType.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new ApiAlertException("Unsupported managed Flink provider type.");
        }
    }

    private static void rejectCustomEndpoint(String endpoint) {
        ApiAlertException.throwIfTrue(
            StringUtils.isNotBlank(endpoint),
            "Custom managed Flink endpoints are not enabled in the current release.");
    }

    private static CredentialBinding binding(
                                             String providerType,
                                             long accountId,
                                             CredentialField field) {
        return new CredentialBinding(providerType, accountId, field);
    }

    private static String maskAccessKey(String accessKey) {
        String value = accessKey.trim();
        if (value.length() <= 4) {
            return "****";
        }
        if (value.length() <= 8) {
            return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private void persistConnectivity(
                                     CloudAccountVersionedIdRequest request,
                                     int connectivityState,
                                     ProviderErrorCategory errorCategory,
                                     String errorCode) {
        transactionTemplate.executeWithoutResult(
            status -> {
                String safeErrorCode =
                    errorCategory == null
                        ? null
                        : errorCategory.name() + ":" + safeProviderCode(errorCode);
                int updated =
                    cloudAccountMapper.update(
                        null,
                        new LambdaUpdateWrapper<CloudAccount>()
                            .eq(CloudAccount::getId, request.getId())
                            .eq(CloudAccount::getVersion, request.getVersion())
                            .set(CloudAccount::getConnectivityState, connectivityState)
                            .set(CloudAccount::getLastCheckTime, new Date())
                            .set(CloudAccount::getLastErrorCode, safeErrorCode)
                            .set(
                                CloudAccount::getLastErrorMessage,
                                errorCategory == null
                                    ? null
                                    : "Managed Flink connectivity check failed.")
                            .set(CloudAccount::getVersion, request.getVersion() + 1));
                requireUpdated(updated);
            });
    }

    private static String safeProviderCode(String errorCode) {
        if (errorCode != null && errorCode.matches("[A-Za-z0-9._-]{1,128}")) {
            return errorCode;
        }
        return "UnknownProviderError";
    }

    private static void requireUpdated(int updated) {
        ApiAlertException.throwIfFalse(
            updated == 1, "Cloud account was modified by another request. Refresh and retry.");
    }

}
