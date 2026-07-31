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

package org.apache.streampark.console.core.managed;

import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.core.entity.CloudAccount;
import org.apache.streampark.console.core.entity.CloudAccountTeam;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.ManagedFlinkEnvironment;
import org.apache.streampark.console.core.mapper.CloudAccountMapper;
import org.apache.streampark.console.core.mapper.CloudAccountTeamMapper;
import org.apache.streampark.console.core.mapper.FlinkClusterMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkEnvironmentMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@TestPropertySource(properties = "server.port=0")
class ManagedFlinkPersistenceTest extends SpringUnitTestBase {

    @Autowired
    private CloudAccountMapper cloudAccountMapper;

    @Autowired
    private CloudAccountTeamMapper cloudAccountTeamMapper;

    @Autowired
    private FlinkClusterMapper flinkClusterMapper;

    @Autowired
    private ManagedFlinkEnvironmentMapper managedFlinkEnvironmentMapper;

    @Test
    void shouldPersistManagedAccountAuthorizationAndEnvironment() throws Exception {
        CloudAccount account = new CloudAccount();
        account.setAccountName("managed-persistence-test");
        account.setProviderType("VOLCENGINE");
        account.setRegion("cn-beijing");
        account.setAccessKeyCiphertext("encrypted-ak");
        account.setSecretKeyCiphertext("encrypted-sk");
        account.setCredentialKeyVersion(1);
        account.setAccessKeyMask("AKLT****TEST");
        account.setConnectivityState(0);
        account.setStatus(1);
        account.setCreateUserId(100000L);
        account.setVersion(0);

        assertThat(cloudAccountMapper.insert(account)).isEqualTo(1);
        assertThat(account.getId()).isNotNull();

        CloudAccount persistedAccount = cloudAccountMapper.selectById(account.getId());
        assertThat(persistedAccount.getAccessKeyCiphertext()).isEqualTo("encrypted-ak");
        assertThat(persistedAccount.getSecretKeyCiphertext()).isEqualTo("encrypted-sk");
        assertThat(new ObjectMapper().writeValueAsString(persistedAccount))
            .doesNotContain("accessKeyCiphertext", "secretKeyCiphertext", "encrypted-ak", "encrypted-sk")
            .contains("\"accessKeyMask\":\"AKLT****TEST\"");

        CloudAccountTeam authorization = new CloudAccountTeam();
        authorization.setCloudAccountId(account.getId());
        authorization.setTeamId(100000L);
        authorization.setPermissionLevel("USE");
        authorization.setCreateUserId(100000L);

        assertThat(cloudAccountTeamMapper.insert(authorization)).isEqualTo(1);
        assertThat(
            cloudAccountTeamMapper.selectOne(
                new LambdaQueryWrapper<CloudAccountTeam>()
                    .eq(CloudAccountTeam::getCloudAccountId, account.getId())
                    .eq(CloudAccountTeam::getTeamId, 100000L))
                .getPermissionLevel())
                    .isEqualTo("USE");

        FlinkCluster cluster = new FlinkCluster();
        cluster.setClusterName("managed-persistence-test");
        cluster.setDeployMode(FlinkDeployMode.MANAGED_APPLICATION.getMode());
        cluster.setVersionId(100000L);

        assertThat(flinkClusterMapper.insert(cluster)).isEqualTo(1);
        assertThat(cluster.getId()).isNotNull();

        ManagedFlinkEnvironment environment = new ManagedFlinkEnvironment();
        environment.setClusterId(cluster.getId());
        environment.setProviderType("VOLCENGINE");
        environment.setCloudAccountId(account.getId());
        environment.setRegion("cn-beijing");
        environment.setProjectId("cwz-test");
        environment.setProjectName("cwz-test");
        environment.setResourcePoolId("paimon-test2");
        environment.setDraftDirectoryId(1L);
        environment.setResourcePoolName("paimon-test2");
        environment.setConsoleUrl("https://console.volcengine.com/flink");
        environment.setCapabilityJson("{\"savepoint\":true}");
        environment.setVersion(0);

        assertThat(managedFlinkEnvironmentMapper.insert(environment)).isEqualTo(1);
        assertThat(managedFlinkEnvironmentMapper.selectById(cluster.getId()))
            .extracting(
                ManagedFlinkEnvironment::getProjectId,
                ManagedFlinkEnvironment::getResourcePoolId,
                ManagedFlinkEnvironment::getVersion)
            .containsExactly("cwz-test", "paimon-test2", 0);
    }
}
