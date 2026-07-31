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

import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.CloudAccount;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.managed.api.CredentialCheckResult;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProvider;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderException;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderRegistry;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;
import org.apache.streampark.console.core.managed.api.ProviderErrorCategory;
import org.apache.streampark.console.core.managed.model.CloudAccountCreateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountPageRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountUpdateRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountVersionedIdRequest;
import org.apache.streampark.console.core.managed.model.CloudAccountView;
import org.apache.streampark.console.core.mapper.CloudAccountMapper;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Transactional
@TestPropertySource(properties = {
        "server.port=0",
        "streampark.managed-flink.enabled=true",
        "streampark.managed-flink.providers.volcengine.enabled=true",
        "streampark.managed-flink.credentials.active-key-version=1",
        "streampark.managed-flink.credentials.master-keys="
            + "1=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class CloudAccountServiceTest extends SpringUnitTestBase {

    private static final long USER_ID = 100000L;
    private static final String ACCESS_KEY = "AKLT-manage-account-test";
    private static final String SECRET_KEY = "secret-manage-account-test";

    @Autowired
    private CloudAccountService cloudAccountService;

    @Autowired
    private CloudAccountMapper cloudAccountMapper;

    @Autowired
    private FlinkClusterMapper flinkClusterMapper;

    @Autowired
    private ManagedFlinkEnvironmentMapper managedFlinkEnvironmentMapper;

    @Autowired
    private CredentialCryptoService credentialCryptoService;

    @MockBean
    private ManagedFlinkAuditContext auditContext;

    @MockBean
    private ManagedFlinkProviderRegistry providerRegistry;

    private ManagedFlinkProvider provider;

    @BeforeEach
    void setUpAuditUser() {
        when(auditContext.currentUserId()).thenReturn(USER_ID);
        provider = mock(ManagedFlinkProvider.class);
        when(providerRegistry.getRequired(ManagedFlinkProviderType.VOLCENGINE))
            .thenReturn(provider);
    }

    @Test
    void shouldCreateEncryptedAccountAndReturnOnlyMaskedCredentials() throws Exception {
        Long accountId = createAccount("encrypted-account");

        CloudAccount persisted = cloudAccountMapper.selectById(accountId);
        assertThat(persisted.getId()).isEqualTo(accountId);
        assertThat(persisted.getAccessKeyCiphertext())
            .startsWith("v1.1.")
            .doesNotContain(ACCESS_KEY);
        assertThat(persisted.getSecretKeyCiphertext())
            .startsWith("v1.1.")
            .doesNotContain(SECRET_KEY);
        assertThat(decrypt(persisted, CredentialField.ACCESS_KEY)).isEqualTo(ACCESS_KEY);
        assertThat(decrypt(persisted, CredentialField.SECRET_KEY)).isEqualTo(SECRET_KEY);
        assertThat(persisted.getCreateUserId()).isEqualTo(USER_ID);

        CloudAccountView view = cloudAccountService.get(accountId);
        String json = new ObjectMapper().writeValueAsString(view);
        assertThat(view.getAccessKeyMask()).isEqualTo("AKLT****test");
        assertThat(json)
            .doesNotContain(
                "accessKeyCiphertext",
                "secretKeyCiphertext",
                ACCESS_KEY,
                SECRET_KEY)
            .contains("\"accessKeyMask\":\"AKLT****test\"");

        CloudAccountPageRequest pageRequest = new CloudAccountPageRequest();
        pageRequest.setAccountName("encrypted");
        pageRequest.setProviderType("volcengine");
        pageRequest.setStatus(1);
        IPage<CloudAccountView> page = cloudAccountService.page(pageRequest);
        assertThat(page.getRecords())
            .extracting(CloudAccountView::getId)
            .containsExactly(accountId);
    }

    @Test
    void shouldUpdateMetadataWithoutChangingCredentials() {
        Long accountId = createAccount("metadata-account");
        CloudAccount before = cloudAccountMapper.selectById(accountId);
        before.setConnectivityState(1);
        before.setLastCheckTime(new Date());
        before.setLastErrorCode("OLD_ERROR");
        before.setLastErrorMessage("sanitized old error");
        assertThat(cloudAccountMapper.updateById(before)).isEqualTo(1);
        CloudAccountUpdateRequest request =
            updateRequest(accountId, 0, "metadata-account-renamed");
        request.setRegion("cn-shanghai");
        request.setDescription(null);

        cloudAccountService.update(request);

        CloudAccount updated = cloudAccountMapper.selectById(accountId);
        assertThat(updated.getAccountName()).isEqualTo("metadata-account-renamed");
        assertThat(updated.getRegion()).isEqualTo("cn-shanghai");
        assertThat(updated.getDescription()).isNull();
        assertThat(updated.getVersion()).isEqualTo(1);
        assertThat(updated.getConnectivityState()).isZero();
        assertThat(updated.getLastCheckTime()).isNull();
        assertThat(updated.getLastErrorCode()).isNull();
        assertThat(updated.getLastErrorMessage()).isNull();
        assertThat(updated.getAccessKeyCiphertext())
            .isEqualTo(before.getAccessKeyCiphertext());
        assertThat(updated.getSecretKeyCiphertext())
            .isEqualTo(before.getSecretKeyCiphertext());
    }

    @Test
    void shouldUpdateNonNullDescriptionWithoutDuplicatingSetClause() {
        Long accountId = createAccount("description-account");
        CloudAccountUpdateRequest request =
            updateRequest(accountId, 0, "description-account");
        request.setDescription("updated description");

        cloudAccountService.update(request);

        CloudAccount updated = cloudAccountMapper.selectById(accountId);
        assertThat(updated.getDescription()).isEqualTo("updated description");
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldRotateCredentialsAndResetConnectivityEvidence() {
        Long accountId = createAccount("rotation-account");
        CloudAccount before = cloudAccountMapper.selectById(accountId);
        before.setConnectivityState(1);
        before.setLastCheckTime(new Date());
        before.setLastErrorCode("OLD_ERROR");
        before.setLastErrorMessage("sanitized old error");
        assertThat(cloudAccountMapper.updateById(before)).isEqualTo(1);

        CloudAccountUpdateRequest request =
            updateRequest(accountId, 0, "rotation-account");
        request.setAccessKey("AKLT-rotated-account-test");
        request.setSecretKey("secret-rotated-account-test");
        cloudAccountService.update(request);

        CloudAccount updated = cloudAccountMapper.selectById(accountId);
        assertThat(updated.getAccessKeyCiphertext())
            .isNotEqualTo(before.getAccessKeyCiphertext());
        assertThat(updated.getSecretKeyCiphertext())
            .isNotEqualTo(before.getSecretKeyCiphertext());
        assertThat(updated.getAccessKeyMask()).isEqualTo("AKLT****test");
        assertThat(updated.getConnectivityState()).isZero();
        assertThat(updated.getLastCheckTime()).isNull();
        assertThat(updated.getLastErrorCode()).isNull();
        assertThat(updated.getLastErrorMessage()).isNull();
        assertThat(decrypt(updated, CredentialField.ACCESS_KEY))
            .isEqualTo("AKLT-rotated-account-test");
        assertThat(decrypt(updated, CredentialField.SECRET_KEY))
            .isEqualTo("secret-rotated-account-test");
    }

    @Test
    void shouldTestCredentialAndPersistSuccessfulConnectivityEvidence() {
        Long accountId = createAccount("connectivity-success-account");
        when(provider.validateCredential(any()))
            .thenReturn(
                CredentialCheckResult.builder()
                    .success(true)
                    .providerRequestId("provider-request")
                    .message("valid")
                    .build());

        CloudAccountView tested =
            cloudAccountService.test(versionedRequest(accountId, 0));

        assertThat(tested.getConnectivityState()).isEqualTo(1);
        assertThat(tested.getLastCheckTime()).isNotNull();
        assertThat(tested.getLastErrorCode()).isNull();
        assertThat(tested.getLastErrorMessage()).isNull();
        assertThat(tested.getProviderRequestId()).isEqualTo("provider-request");
        assertThat(tested.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldPersistOnlySafeConnectivityFailureAndRejectStaleTest() {
        Long accountId = createAccount("connectivity-failure-account");
        when(provider.validateCredential(any()))
            .thenThrow(
                new ManagedFlinkProviderException(
                    ProviderErrorCategory.AUTHENTICATION,
                    "SignatureDoesNotMatch",
                    "provider-request",
                    "raw provider detail with secret"));

        CloudAccountView tested =
            cloudAccountService.test(versionedRequest(accountId, 0));

        assertThat(tested.getConnectivityState()).isEqualTo(2);
        assertThat(tested.getLastErrorCode())
            .isEqualTo("AUTHENTICATION:SignatureDoesNotMatch");
        assertThat(tested.getLastErrorMessage())
            .isEqualTo("Managed Flink connectivity check failed.")
            .doesNotContain("raw", "secret");
        assertThat(tested.getProviderRequestId()).isEqualTo("provider-request");
        assertThat(tested.getVersion()).isEqualTo(1);

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(
                () -> cloudAccountService.test(versionedRequest(accountId, 0)))
            .withMessageContaining("modified by another request");
    }

    @Test
    void shouldRejectPartialCredentialUpdateAndStaleVersion() {
        Long accountId = createAccount("concurrent-account");
        CloudAccountUpdateRequest partial =
            updateRequest(accountId, 0, "concurrent-account");
        partial.setAccessKey("partial-access-key");

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> cloudAccountService.update(partial))
            .withMessageContaining("updated together");

        CloudAccountUpdateRequest accepted =
            updateRequest(accountId, 0, "concurrent-account-updated");
        cloudAccountService.update(accepted);
        CloudAccountUpdateRequest stale =
            updateRequest(accountId, 0, "stale-name");
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> cloudAccountService.update(stale))
            .withMessageContaining("modified by another request");
        assertThat(cloudAccountService.get(accountId).getAccountName())
            .isEqualTo("concurrent-account-updated");
    }

    @Test
    void shouldDisableIdempotentlyAndRejectStaleDisable() {
        Long staleAccountId = createAccount("stale-disable-account");
        CloudAccountUpdateRequest update =
            updateRequest(staleAccountId, 0, "stale-disable-account-updated");
        cloudAccountService.update(update);
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(
                () -> cloudAccountService.disable(versionedRequest(staleAccountId, 0)))
            .withMessageContaining("modified by another request");

        Long accountId = createAccount("disable-account");
        cloudAccountService.disable(versionedRequest(accountId, 0));
        CloudAccount disabled = cloudAccountMapper.selectById(accountId);
        assertThat(disabled.getStatus()).isZero();
        assertThat(disabled.getVersion()).isEqualTo(1);

        cloudAccountService.disable(versionedRequest(accountId, 0));
        assertThat(cloudAccountMapper.selectById(accountId).getVersion()).isEqualTo(1);
    }

    @Test
    void shouldProtectReferencedAccountAndDeleteUnreferencedAccount() {
        Long accountId = createAccount("referenced-account");
        FlinkCluster cluster = new FlinkCluster();
        cluster.setClusterName("referenced-account-cluster");
        cluster.setDeployMode(FlinkDeployMode.MANAGED_APPLICATION.getMode());
        cluster.setVersionId(USER_ID);
        assertThat(flinkClusterMapper.insert(cluster)).isEqualTo(1);

        ManagedFlinkEnvironment environment = new ManagedFlinkEnvironment();
        environment.setClusterId(cluster.getId());
        environment.setProviderType("VOLCENGINE");
        environment.setCloudAccountId(accountId);
        environment.setRegion("cn-beijing");
        environment.setProjectId("cwz-test");
        environment.setResourcePoolId("paimon-test2");
        environment.setDraftDirectoryId(1L);
        environment.setVersion(0);
        assertThat(managedFlinkEnvironmentMapper.insert(environment)).isEqualTo(1);

        CloudAccountVersionedIdRequest deleteRequest = versionedRequest(accountId, 0);
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> cloudAccountService.delete(deleteRequest))
            .withMessageContaining("referenced");

        assertThat(managedFlinkEnvironmentMapper.deleteById(cluster.getId())).isEqualTo(1);
        cloudAccountService.delete(deleteRequest);
        assertThat(cloudAccountMapper.selectById(accountId)).isNull();
        cloudAccountService.delete(deleteRequest);
    }

    @Test
    void shouldRejectDuplicateNamesAndCustomEndpoints() {
        createAccount("unique-account");
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> createAccount("unique-account"))
            .withMessageContaining("already exists");

        CloudAccountCreateRequest request = createRequest("custom-endpoint-account");
        request.setEndpoint("https://custom.example.test");
        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> cloudAccountService.create(request))
            .withMessageContaining("Custom managed Flink endpoints");
    }

    private Long createAccount(String name) {
        return cloudAccountService.create(createRequest(name));
    }

    private static CloudAccountCreateRequest createRequest(String name) {
        CloudAccountCreateRequest request = new CloudAccountCreateRequest();
        request.setAccountName(name);
        request.setProviderType("VOLCENGINE");
        request.setRegion("cn-beijing");
        request.setAccessKey(ACCESS_KEY);
        request.setSecretKey(SECRET_KEY);
        request.setDescription("managed account test");
        return request;
    }

    private static CloudAccountUpdateRequest updateRequest(
                                                           Long id,
                                                           int version,
                                                           String name) {
        CloudAccountUpdateRequest request = new CloudAccountUpdateRequest();
        request.setId(id);
        request.setVersion(version);
        request.setAccountName(name);
        request.setRegion("cn-beijing");
        return request;
    }

    private static CloudAccountVersionedIdRequest versionedRequest(
                                                                   Long id,
                                                                   int version) {
        CloudAccountVersionedIdRequest request = new CloudAccountVersionedIdRequest();
        request.setId(id);
        request.setVersion(version);
        return request;
    }

    private String decrypt(CloudAccount account, CredentialField field) {
        String ciphertext = field == CredentialField.ACCESS_KEY
            ? account.getAccessKeyCiphertext()
            : account.getSecretKeyCiphertext();
        CredentialBinding binding =
            new CredentialBinding(account.getProviderType(), account.getId(), field);
        return credentialCryptoService.decrypt(
            new EncryptedCredential(account.getCredentialKeyVersion(), ciphertext),
            binding);
    }
}
