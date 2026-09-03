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

package org.apache.streampark.console.core.service;

import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.FlinkConfigurationUtils;
import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkApplicationConfig;
import org.apache.streampark.console.core.entity.FlinkEffective;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.enums.ConfigFileTypeEnum;
import org.apache.streampark.console.core.enums.EffectiveTypeEnum;
import org.apache.streampark.console.core.service.application.FlinkApplicationConfigService;
import org.apache.streampark.console.core.service.application.FlinkApplicationManageService;
import org.apache.streampark.console.core.service.impl.FlinkSavepointServiceImpl;
import org.apache.streampark.console.core.util.FlinkEnvUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.flink.configuration.CheckpointingOptions.SAVEPOINT_DIRECTORY;
import static org.apache.flink.streaming.api.environment.ExecutionCheckpointingOptions.CHECKPOINTING_INTERVAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests savepoint configuration resolution performed by {@link FlinkSavepointServiceImpl}. */
class FlinkSavepointServiceTest extends SpringUnitTestBase {

    @Autowired
    private SavepointService savepointService;

    @Autowired
    private FlinkApplicationConfigService configService;

    @Autowired
    private FlinkEffectiveService effectiveService;

    @Autowired
    private FlinkEnvService flinkEnvService;
    @Autowired
    private FlinkClusterService flinkClusterService;
    @Autowired
    FlinkApplicationManageService applicationManageService;

    @AfterEach
    void cleanTestRecordsInDatabase() {
        savepointService.remove(new QueryWrapper<>());
        configService.remove(new QueryWrapper<>());
        effectiveService.remove(new QueryWrapper<>());
        flinkEnvService.remove(new QueryWrapper<>());
        flinkClusterService.remove(new QueryWrapper<>());
        applicationManageService.remove(new QueryWrapper<>());
    }

    @Test
    void readSavepointFromDynamicProps() {
        String propsWithEmptyTargetValue = "-Dexecution.checkpointing.savepoint-dir=";
        String props = "-Dexecution.checkpointing.savepoint-dir=hdfs:///test";
        FlinkSavepointServiceImpl savepointServiceImpl = (FlinkSavepointServiceImpl) savepointService;

        assertThat(savepointServiceImpl.getSavepointFromDynamicProps(null)).isNull();
        assertThat(savepointServiceImpl.getSavepointFromDynamicProps(props)).isEqualTo("hdfs:///test");
        assertThat(savepointServiceImpl.getSavepointFromDynamicProps(propsWithEmptyTargetValue))
            .isEmpty();
    }

    @Test
    void readSavepointFromAppConfig() {
        FlinkSavepointServiceImpl savepointServiceImpl = (FlinkSavepointServiceImpl) savepointService;
        FlinkApplication app = new FlinkApplication();
        Long appId = 1L;
        Long appCfgId = 1L;
        app.setId(appId);

        app.setAppType(ApplicationType.APACHE_FLINK.getType());
        assertThat(savepointServiceImpl.getSavepointFromConfig(app)).isNull();

        app.setAppType(ApplicationType.STREAMPARK_FLINK.getType());
        app.setJobType(FlinkJobType.FLINK_JAR.getMode());
        assertThat(savepointServiceImpl.getSavepointFromConfig(app)).isNull();

        String ckDir = SAVEPOINT_DIRECTORY.key() + "=hdfs:///test";

        FlinkApplicationConfig appCfg = new FlinkApplicationConfig();
        appCfg.setId(appCfgId);
        appCfg.setAppId(appId);
        appCfg.setContent(ckDir);
        appCfg.setFormat(ConfigFileTypeEnum.PROPERTIES.getValue());
        configService.save(appCfg);
        assertThat(savepointServiceImpl.getSavepointFromConfig(app)).isNull();

        appCfg.setContent(
            DeflaterUtils.zipString(
                ckDir + "\n"
                    + String.format("%s=%s", CHECKPOINTING_INTERVAL.key(),
                        "3min")));

        configService.updateById(appCfg);
        FlinkEffective effective = new FlinkEffective();
        effective.setTargetId(appCfg.getId());
        effective.setAppId(appId);
        effective.setTargetType(EffectiveTypeEnum.CONFIG.getType());
        effectiveService.save(effective);
        assertThat(savepointServiceImpl.getSavepointFromConfig(app)).isEqualTo("hdfs:///test");
    }

    @Test
    void readSavepointFromDeployConfig(@TempDir Path flinkHome) throws Exception {
        FlinkSavepointServiceImpl savepointServiceImpl = (FlinkSavepointServiceImpl) savepointService;
        Long appId = 1L;
        Long idOfFlinkEnv = 1L;
        Long teamId = 1L;
        FlinkApplication application = new FlinkApplication();
        application.setId(appId);
        application.setTeamId(teamId);
        application.setVersionId(idOfFlinkEnv);
        application.setDeployMode(FlinkDeployMode.YARN_APPLICATION.getMode());
        applicationManageService.save(application);

        FlinkEnv flinkEnv = new FlinkEnv();
        flinkEnv.setFlinkName("mockFlinkName");
        flinkEnv.setFlinkHome(flinkHome.toString());
        flinkEnv.setId(idOfFlinkEnv);
        flinkEnv.setVersion("1.15.3");
        flinkEnv.setScalaVersion("2.12");
        Path confDir = Files.createDirectories(flinkHome.resolve("conf"));
        Files.writeString(
            confDir.resolve(FlinkConfigurationUtils.LEGACY_FLINK_CONF_FILENAME),
            SAVEPOINT_DIRECTORY.key() + ": hdfs:///test");
        FlinkEnvUtils.sync(flinkEnv);
        flinkEnvService.save(flinkEnv);

        assertThat(savepointServiceImpl.getSavepointFromDeployLayer(application))
            .isEqualTo("hdfs:///test");

        Long clusterId = 1L;
        application.setDeployMode(FlinkDeployMode.REMOTE.getMode());
        application.setFlinkClusterId(clusterId);
        assertThatThrownBy(() -> savepointServiceImpl.getSavepointFromDeployLayer(application))
            .isInstanceOf(NullPointerException.class);
    }
}
