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
import org.apache.streampark.common.enums.DevelopmentMode;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.console.SpringTestBase;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.Effective;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.enums.ConfigFileType;
import org.apache.streampark.console.core.enums.EffectiveType;
import org.apache.streampark.console.core.service.application.OpApplicationInfoService;
import org.apache.streampark.console.core.service.impl.SavePointServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.flink.configuration.CheckpointingOptions.SAVEPOINT_DIRECTORY;
import static org.apache.flink.streaming.api.environment.ExecutionCheckpointingOptions.CHECKPOINTING_INTERVAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for the implementation {@link
 * org.apache.streampark.console.core.service.impl.SavePointServiceImpl} of {@link
 * SavePointService}.
 */
class SavePointServiceTest extends SpringTestBase {

  @Autowired private SavePointService savePointService;

  @Autowired private ApplicationConfigService configService;

  @Autowired private EffectiveService effectiveService;

  @Autowired private FlinkEnvService flinkEnvService;
  @Autowired private FlinkClusterService flinkClusterService;
  @Autowired OpApplicationInfoService opApplicationInfoService;

  @AfterEach
  void cleanTestRecordsInDatabase() {
    savePointService.remove(new QueryWrapper<>());
    configService.remove(new QueryWrapper<>());
    effectiveService.remove(new QueryWrapper<>());
    flinkEnvService.remove(new QueryWrapper<>());
    flinkClusterService.remove(new QueryWrapper<>());
    opApplicationInfoService.remove(new QueryWrapper<>());
  }

  /**
   * This part will be migrated into the corresponding test cases about
   * PropertiesUtils.extractDynamicPropertiesAsJava.
   */
  @Test
  void testGetSavepointFromDynamicProps() {
    String propsWithEmptyTargetValue = "-Dstate.savepoints.dir=";
    String props = "-Dstate.savepoints.dir=hdfs:///test";
    SavePointServiceImpl savePointServiceImpl = (SavePointServiceImpl) savePointService;

    assertThat(savePointServiceImpl.getSavepointFromDynamicProps(null)).isNull();
    assertThat(savePointServiceImpl.getSavepointFromDynamicProps(props)).isEqualTo("hdfs:///test");
    assertThat(savePointServiceImpl.getSavepointFromDynamicProps(propsWithEmptyTargetValue))
        .isEmpty();
  }

  @Test
  void testGetSavepointFromAppCfgIfStreamParkOrSQLJob() {
    SavePointServiceImpl savePointServiceImpl = (SavePointServiceImpl) savePointService;
    Application app = new Application();
    Long appId = 1L;
    Long appCfgId = 1L;
    app.setId(appId);

    // Test for non-(StreamPark job Or FlinkSQL job)
    app.setAppType(ApplicationType.APACHE_FLINK.getType());
    assertThat(savePointServiceImpl.getSavepointFromAppCfgIfStreamParkOrSQLJob(app)).isNull();
    app.setAppType(ApplicationType.STREAMPARK_FLINK.getType());
    app.setJobType(DevelopmentMode.CUSTOM_CODE.getValue());
    assertThat(savePointServiceImpl.getSavepointFromAppCfgIfStreamParkOrSQLJob(app)).isNull();

    // Test for (StreamPark job Or FlinkSQL job) without application config.
    app.setAppType(ApplicationType.STREAMPARK_FLINK.getType());
    assertThat(savePointServiceImpl.getSavepointFromAppCfgIfStreamParkOrSQLJob(app)).isNull();
    app.setAppType(ApplicationType.STREAMPARK_FLINK.getType());
    app.setJobType(DevelopmentMode.CUSTOM_CODE.getValue());
    assertThat(savePointServiceImpl.getSavepointFromAppCfgIfStreamParkOrSQLJob(app)).isNull();

    // Test for (StreamPark job Or FlinkSQL job) with application config just disabled checkpoint.
    ApplicationConfig appCfg = new ApplicationConfig();
    appCfg.setId(appCfgId);
    appCfg.setAppId(appId);
    appCfg.setContent("state.savepoints.dir=hdfs:///test");
    appCfg.setFormat(ConfigFileType.PROPERTIES.getValue());
    configService.save(appCfg);
    assertThat(savePointServiceImpl.getSavepointFromAppCfgIfStreamParkOrSQLJob(app)).isNull();

    // Test for (StreamPark job or FlinkSQL job) with application config and enabled checkpoint and
    // configured value.

    // Test for non-value for CHECKPOINTING_INTERVAL
    appCfg.setContent("");
    configService.updateById(appCfg);
    assertThat(savePointServiceImpl.getSavepointFromAppCfgIfStreamParkOrSQLJob(app)).isNull();

    // Test for configured CHECKPOINTING_INTERVAL
    appCfg.setContent(
        DeflaterUtils.zipString(
            "state.savepoints.dir=hdfs:///test\n"
                + String.format("%s=%s", CHECKPOINTING_INTERVAL.key(), "3min")));
    configService.updateById(appCfg);
    Effective effective = new Effective();
    effective.setTargetId(appCfg.getId());
    effective.setAppId(appId);
    effective.setTargetType(EffectiveType.CONFIG.getType());
    effectiveService.save(effective);
    assertThat(savePointServiceImpl.getSavepointFromAppCfgIfStreamParkOrSQLJob(app))
        .isEqualTo("hdfs:///test");
  }

  @Test
  void testGetSavepointFromDeployLayer() throws JsonProcessingException {
    SavePointServiceImpl savePointServiceImpl = (SavePointServiceImpl) savePointService;
    Long appId = 1L;
    Long idOfFlinkEnv = 1L;
    Long teamId = 1L;
    Application application = new Application();
    application.setId(appId);
    application.setTeamId(teamId);
    application.setVersionId(idOfFlinkEnv);
    application.setExecutionMode(ExecutionMode.YARN_APPLICATION.getMode());
    opApplicationInfoService.save(application);

    FlinkEnv flinkEnv = new FlinkEnv();
    flinkEnv.setFlinkName("mockFlinkName");
    flinkEnv.setFlinkHome("/tmp");
    flinkEnv.setId(idOfFlinkEnv);
    flinkEnv.setVersion("1.15.3");
    flinkEnv.setScalaVersion("2.12");
    flinkEnv.setFlinkConf(DeflaterUtils.zipString(SAVEPOINT_DIRECTORY.key() + ": hdfs:///test"));
    flinkEnvService.save(flinkEnv);

    // Test for non-remote mode
    assertThat(savePointServiceImpl.getSavepointFromDeployLayer(application))
        .isEqualTo("hdfs:///test");

    // Start the test lines for remote mode
    Long clusterId = 1L;

    // Test for it without cluster.
    application.setExecutionMode(ExecutionMode.REMOTE.getMode());
    application.setFlinkClusterId(clusterId);
    assertThatThrownBy(() -> savePointServiceImpl.getSavepointFromDeployLayer(application))
        .isInstanceOf(NullPointerException.class);

    // Ignored.
    // Test for it with empty config
    // Test for it with the configured empty target value
    // Test for it with the configured non-empty target value

  }
}
