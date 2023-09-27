/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.streampark.console.core.service;

import org.apache.streampark.common.enums.FlinkExecutionMode;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.console.SpringIntegrationTestBase;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.service.application.ApplicationActionService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.console.core.service.impl.FlinkClusterServiceImpl;
import org.apache.streampark.console.core.watcher.FlinkAppHttpWatcher;
import org.apache.streampark.testcontainer.flink.FlinkStandaloneSessionCluster;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.streampark.console.core.watcher.FlinkAppHttpWatcher.WATCHING_INTERVAL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link
 * org.apache.streampark.console.core.service.application.ApplicationManageService}.
 */
@Disabled("Disabled due to unstable performance.")
class ApplicationManageServiceITest extends SpringIntegrationTestBase {

  static FlinkStandaloneSessionCluster cluster =
      FlinkStandaloneSessionCluster.builder().slotsNumPerTm(4).slf4jLogConsumer(null).build();

  @Autowired private ApplicationManageService applicationManageService;

  @Autowired private ApplicationActionService applicationActionService;

  @Autowired private FlinkClusterService clusterService;

  @Autowired private FlinkEnvService envService;

  @Autowired private AppBuildPipeService appBuildPipeService;

  @Autowired private FlinkSqlService sqlService;

  @Autowired private FlinkAppHttpWatcher flinkAppHttpWatcher;

  @BeforeAll
  static void setup() {
    cluster.start();
  }

  @AfterAll
  static void teardown() {
    cluster.stop();
  }

  @AfterEach
  void clear() {
    applicationManageService.getBaseMapper().delete(new QueryWrapper<>());
    clusterService.getBaseMapper().delete(new QueryWrapper<>());
    envService.getBaseMapper().delete(new QueryWrapper<>());
    appBuildPipeService.getBaseMapper().delete(new QueryWrapper<>());
    sqlService.getBaseMapper().delete(new QueryWrapper<>());
  }

  @Test
  @Timeout(value = 180)
  void testStartAppOnRemoteSessionMode() throws Exception {
    FlinkEnv flinkEnv = new FlinkEnv();
    flinkEnv.setFlinkHome(defaultFlinkHome);
    flinkEnv.setFlinkName(DEFAULT_FLINK_VERSION);
    flinkEnv.setId(1L);
    envService.create(flinkEnv);
    FlinkCluster flinkCluster = new FlinkCluster();
    flinkCluster.setId(1L);
    flinkCluster.setAddress(cluster.getFlinkJobManagerUrl());
    flinkCluster.setExecutionMode(FlinkExecutionMode.REMOTE.getMode());
    flinkCluster.setClusterName("docker-Cluster-1.17.1");
    flinkCluster.setVersionId(1L);
    flinkCluster.setUserId(100000L);
    ((FlinkClusterServiceImpl) clusterService).internalCreate(flinkCluster);
    Application appParam = new Application();
    appParam.setId(100000L);
    appParam.setTeamId(100000L);
    Application application = applicationManageService.getApp(appParam);
    application.setFlinkClusterId(1L);
    application.setSqlId(100000L);
    application.setVersionId(1L);
    application.setExecutionMode(FlinkExecutionMode.REMOTE.getMode());

    // Avoid exceptional error.
    application.setFlinkSql(
        new String(Base64.getDecoder().decode(application.getFlinkSql().getBytes())));
    FlinkSql flinkSql = sqlService.getEffective(application.getId(), false);
    flinkSql.setSql(DeflaterUtils.zipString(flinkSql.getSql()));
    sqlService.getBaseMapper().updateById(flinkSql);

    // Continue operations link.
    applicationManageService.update(application);
    appBuildPipeService.buildApplication(100000L, false);

    CompletableFuture<Boolean> buildCompletableFuture =
        CompletableFuture.supplyAsync(
            () -> {
              while (true) {
                Application app = applicationManageService.getById(100000L);
                if (app != null && app.getReleaseState() == ReleaseStateEnum.DONE) {
                  break;
                }
              }
              return true;
            });
    buildCompletableFuture.get();

    applicationActionService.start(applicationManageService.getById(100000L), false);
    CompletableFuture<Boolean> completableFuture =
        CompletableFuture.supplyAsync(
            () -> {
              while (true) {
                if (flinkAppHttpWatcher.tryQueryFlinkAppState(application.getId())
                    == FlinkAppStateEnum.RUNNING) {
                  break;
                }
              }
              return true;
            });

    assertThat(completableFuture.get(WATCHING_INTERVAL.toMillis() * 24, TimeUnit.MILLISECONDS))
        .isEqualTo(true);
  }
}
