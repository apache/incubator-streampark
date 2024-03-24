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

import org.apache.streampark.console.SpringUnitTestBase;
import org.apache.streampark.console.core.entity.Variable;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** org.apache.streampark.console.core.service.VariableServiceTest */
class VariableServiceTest extends SpringUnitTestBase {

  @Autowired private VariableService variableService;

  @AfterEach
  void cleanTestRecordsInDatabase() {
    variableService.remove(new QueryWrapper<>());
  }

  /** Test whether the variable will be replaced normally */
  @Test
  void testNormalReplace() {
    Long teamId = 100000L;
    Long userId = 100000L;
    String variableCode = "collect_kafka.brokers-520room";
    String variableVariable = "broker1:port,broker3:port,broker3:port";
    Variable variable = new Variable();
    variable.setVariableCode(variableCode);
    variable.setVariableValue(variableVariable);
    variable.setDescription("420机房采集kafka broker, 集群规模50台.");
    variable.setCreatorId(userId);
    variable.setTeamId(teamId);
    variableService.save(variable);
    Variable findVariable = variableService.findByVariableCode(teamId, variableCode);
    Assertions.assertNotNull(findVariable);
    String paramWithPlaceholders = "--kafka.brokers ${" + variableCode + "}";
    String realParam = variableService.replaceVariable(teamId, paramWithPlaceholders);
    Assertions.assertEquals(realParam, "--kafka.brokers " + variableVariable);
  }

  /** Test whether the variable cannot be replaced normally */
  @Test
  void testAbnormalReplace() {
    Long teamId = 100000L;
    Long userId = 100000L;
    // It contains a non-normal character '#' which should not be matched
    String variableCode = "collect_#kafkabrokers-520room";
    String variableVariable = "broker1:port,broker3:port,broker3:port";
    Variable variable = new Variable();
    variable.setVariableCode(variableCode);
    variable.setVariableValue(variableVariable);
    variable.setDescription("420机房采集kafka broker, 集群规模50台.");
    variable.setCreatorId(userId);
    variable.setTeamId(teamId);
    variableService.save(variable);
    Variable findVariable = variableService.findByVariableCode(teamId, variableCode);
    Assertions.assertNotNull(findVariable);
    String paramWithPlaceholders = "--kafka.brokers ${" + variableCode + "}";
    String realParam = variableService.replaceVariable(teamId, paramWithPlaceholders);
    Assertions.assertNotEquals("--kafka.brokers " + variableVariable, realParam);
  }

  @Test
  void testUpdateVariableServiceById() {
    Variable variable = new Variable();
    variable.setVariableCode("variableCode");
    variable.setVariableValue("variableValue");
    variable.setDescription("description");
    variable.setCreatorId(0L);
    variable.setTeamId(0L);
    variable.setDesensitization(false);
    variableService.save(variable);
    variable.setVariableCode("updatedVariableCode");
    variable.setVariableValue("updatedVariableValue");
    variable.setDescription("updatedDescription");
    variable.setCreatorId(null);
    variable.setTeamId(null);
    variable.setDesensitization(null);
    variableService.updateById(variable);
    variable = variableService.getById(variable.getId());

    assertThat(variable.getVariableCode()).isEqualTo("updatedVariableCode");
    assertThat(variable.getVariableValue()).isEqualTo("updatedVariableValue");
    assertThat(variable.getDescription()).isEqualTo("updatedDescription");
    assertThat(variable.getCreatorId()).isEqualTo(0L);
    assertThat(variable.getTeamId()).isEqualTo(0L);
    assertThat(variable.getDesensitization()).isEqualTo(false);
  }
}
