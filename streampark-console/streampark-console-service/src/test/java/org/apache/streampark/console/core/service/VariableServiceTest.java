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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** org.apache.streampark.console.core.service.VariableServiceTest */
class VariableServiceTest extends SpringUnitTestBase {

    @Autowired
    private VariableService variableService;

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
}
