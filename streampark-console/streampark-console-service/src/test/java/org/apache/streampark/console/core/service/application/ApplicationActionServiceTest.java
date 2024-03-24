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

package org.apache.streampark.console.core.service.application;

import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.core.entity.Application;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/** org.apache.streampark.console.core.service.alert.ApplicationActionServiceTest. */
class ApplicationActionServiceTest extends SpringUnitTestBase {
  @Autowired private ApplicationActionService applicationActionService;

  @AfterEach
  void cleanTestRecordsInDatabase() {
    applicationActionService.remove(new QueryWrapper<>());
  }

  @Test
  void testUpdateApplicationActionServiceById() {
    Application app = new Application();
    app.setDefaultModeIngress("defaultModeIngress");
    app.setK8sNamespace("k8sNamespace");
    app.setState(0);
    app.setTeamId(0L);
    app.setJobType(0);
    app.setProjectId(0L);
    app.setUserId(0L);
    app.setJobName("jobName");
    app.setAppId("appId");
    app.setJobId("jobId");
    app.setJobManagerUrl("jobManagerUrl");
    app.setVersionId(0L);
    app.setClusterId("clusterId");
    app.setFlinkImage("flinkImage");
    app.setK8sName("k8sName");
    app.setK8sRestExposedType(0);
    app.setK8sPodTemplate("k8sPodTemplate");
    app.setK8sJmPodTemplate("k8sJmPodTemplate");
    app.setK8sTmPodTemplate("k8sTmPodTemplate");
    app.setIngressTemplate("ingressTemplate");
    app.setK8sHadoopIntegration(false);
    app.setRelease(0);
    app.setBuild(false);
    app.setRestartSize(0);
    app.setRestartCount(0);
    app.setOptionState(0);
    app.setAlertId(0L);
    app.setArgs("args");
    app.setModule("module");
    app.setOptions("options");
    app.setHotParams("hotParams");
    app.setResolveOrder(0);
    app.setExecutionMode(0);
    app.setDynamicProperties("dynamicProperties");
    app.setAppType(0);
    app.setTracking(0);
    app.setJar("jar");
    app.setJarCheckSum(0L);
    app.setMainClass("mainClass");
    Date startTime = new Date();
    app.setStartTime(startTime);
    Date endTime = new Date();
    app.setEndTime(endTime);
    app.setDuration(0L);
    app.setCpMaxFailureInterval(0);
    app.setCpFailureRateInterval(0);
    app.setCpFailureAction(0);
    app.setTotalTM(0);
    app.setHadoopUser("hadoopUser");
    app.setTotalSlot(0);
    app.setAvailableSlot(0);
    app.setJmMemory(0);
    app.setTmMemory(0);
    app.setTotalTask(0);
    app.setFlinkClusterId(0L);
    app.setDescription("description");
    Date optionTime = new Date();
    app.setOptionTime(optionTime);
    app.setResourceFrom(0);
    app.setTags("tags");
    app.setProbing(false);
    applicationActionService.save(app);
    app.setDefaultModeIngress("updatedDefaultModeIngress");
    app.setK8sNamespace("updatedK8sNamespace");
    app.setState(0);
    app.setTeamId(null);
    app.setJobType(null);
    app.setProjectId(null);
    app.setUserId(null);
    app.setJobName("updatedJobName");
    app.setAppId("updatedAppId");
    app.setJobId("updatedJobId");
    app.setJobManagerUrl("updatedJobManagerUrl");
    app.setVersionId(null);
    app.setClusterId("updatedClusterId");
    app.setFlinkImage("updatedFlinkImage");
    app.setK8sName("updatedK8sName");
    app.setK8sRestExposedType(null);
    app.setK8sPodTemplate("updatedK8sPodTemplate");
    app.setK8sJmPodTemplate("updatedK8sJmPodTemplate");
    app.setK8sTmPodTemplate("updatedK8sTmPodTemplate");
    app.setIngressTemplate("updatedIngressTemplate");
    app.setK8sHadoopIntegration(null);
    app.setRelease(null);
    app.setBuild(null);
    app.setRestartSize(null);
    app.setRestartCount(null);
    app.setOptionState(null);
    app.setAlertId(null);
    app.setArgs("updatedArgs");
    app.setModule("updatedModule");
    app.setOptions("updatedOptions");
    app.setHotParams("updatedHotParams");
    app.setResolveOrder(null);
    app.setExecutionMode(null);
    app.setDynamicProperties("updatedDynamicProperties");
    app.setAppType(null);
    app.setTracking(null);
    app.setJar("updatedJar");
    app.setJarCheckSum(null);
    app.setMainClass("updatedMainClass");
    app.setStartTime(null);
    app.setEndTime(null);
    app.setDuration(null);
    app.setCpMaxFailureInterval(null);
    app.setCpFailureRateInterval(null);
    app.setCpFailureAction(null);
    app.setTotalTM(null);
    app.setHadoopUser("updatedHadoopUser");
    app.setTotalSlot(null);
    app.setAvailableSlot(null);
    app.setJmMemory(null);
    app.setTmMemory(null);
    app.setTotalTask(null);
    app.setFlinkClusterId(null);
    app.setDescription("updatedDescription");
    app.setOptionTime(null);
    app.setResourceFrom(null);
    app.setTags("updatedTags");
    app.setProbing(null);
    applicationActionService.updateById(app);
    app = applicationActionService.getById(app.getId());

    assertThat(app.getDefaultModeIngress()).isEqualTo("updatedDefaultModeIngress");
    assertThat(app.getK8sNamespace()).isEqualTo("updatedK8sNamespace");
    assertThat(app.getState()).isEqualTo(0);
    assertThat(app.getTeamId()).isEqualTo(0L);
    assertThat(app.getJobType()).isEqualTo(0);
    assertThat(app.getProjectId()).isEqualTo(0L);
    assertThat(app.getUserId()).isEqualTo(0L);
    assertThat(app.getJobName()).isEqualTo("updatedJobName");
    assertThat(app.getAppId()).isEqualTo("updatedAppId");
    assertThat(app.getJobId()).isEqualTo("updatedJobId");
    assertThat(app.getJobManagerUrl()).isEqualTo("updatedJobManagerUrl");
    assertThat(app.getVersionId()).isEqualTo(0L);
    assertThat(app.getClusterId()).isEqualTo("updatedClusterId");
    assertThat(app.getFlinkImage()).isEqualTo("updatedFlinkImage");
    assertThat(app.getK8sName()).isEqualTo("updatedK8sName");
    assertThat(app.getK8sRestExposedType()).isEqualTo(0);
    assertThat(app.getK8sPodTemplate()).isEqualTo("updatedK8sPodTemplate");
    assertThat(app.getK8sJmPodTemplate()).isEqualTo("updatedK8sJmPodTemplate");
    assertThat(app.getK8sTmPodTemplate()).isEqualTo("updatedK8sTmPodTemplate");
    assertThat(app.getIngressTemplate()).isEqualTo("updatedIngressTemplate");
    assertThat(app.getK8sHadoopIntegration()).isEqualTo(false);
    assertThat(app.getRelease()).isEqualTo(0);
    assertThat(app.getBuild()).isEqualTo(false);
    assertThat(app.getRestartSize()).isEqualTo(0);
    assertThat(app.getRestartCount()).isEqualTo(0);
    assertThat(app.getOptionState()).isEqualTo(0);
    assertThat(app.getAlertId()).isEqualTo(0L);
    assertThat(app.getArgs()).isEqualTo("updatedArgs");
    assertThat(app.getModule()).isEqualTo("updatedModule");
    assertThat(app.getOptions()).isEqualTo("updatedOptions");
    assertThat(app.getHotParams()).isEqualTo("updatedHotParams");
    assertThat(app.getResolveOrder()).isEqualTo(0);
    assertThat(app.getExecutionMode()).isEqualTo(0);
    assertThat(app.getDynamicProperties()).isEqualTo("updatedDynamicProperties");
    assertThat(app.getAppType()).isEqualTo(0);
    assertThat(app.getTracking()).isEqualTo(0);
    assertThat(app.getJar()).isEqualTo("updatedJar");
    assertThat(app.getJarCheckSum()).isEqualTo(0L);
    assertThat(app.getMainClass()).isEqualTo("updatedMainClass");
    assertThat(app.getStartTime()).isEqualTo(startTime);
    assertThat(app.getEndTime()).isEqualTo(endTime);
    assertThat(app.getDuration()).isEqualTo(0L);
    assertThat(app.getCpMaxFailureInterval()).isEqualTo(0);
    assertThat(app.getCpFailureRateInterval()).isEqualTo(0);
    assertThat(app.getCpFailureAction()).isEqualTo(0);
    assertThat(app.getTotalTM()).isEqualTo(0);
    assertThat(app.getHadoopUser()).isEqualTo("updatedHadoopUser");
    assertThat(app.getTotalSlot()).isEqualTo(0);
    assertThat(app.getAvailableSlot()).isEqualTo(0);
    assertThat(app.getJmMemory()).isEqualTo(0);
    assertThat(app.getTmMemory()).isEqualTo(0);
    assertThat(app.getTotalTask()).isEqualTo(0);
    assertThat(app.getFlinkClusterId()).isEqualTo(0L);
    assertThat(app.getDescription()).isEqualTo("updatedDescription");
    assertThat(app.getOptionTime()).isEqualTo(optionTime);
    assertThat(app.getResourceFrom()).isEqualTo(0);
    assertThat(app.getTags()).isEqualTo("updatedTags");
    assertThat(app.getProbing()).isEqualTo(false);
  }
}
