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

package org.apache.streampark.console.core.request.sql;

import org.apache.streampark.console.core.request.flink.FlinkSqlDeleteRequest;
import org.apache.streampark.console.core.request.flink.FlinkSqlGetRequest;
import org.apache.streampark.console.core.request.spark.SparkSqlDeleteRequest;
import org.apache.streampark.console.core.request.spark.SparkSqlGetRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import java.util.Set;

class SqlRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void flinkSqlDeleteShouldRequireIdAppIdAndTeamId() {
        Set<ConstraintViolation<FlinkSqlDeleteRequest>> violations = validator.validate(new FlinkSqlDeleteRequest());
        Assertions.assertEquals(3, violations.size());
    }

    @Test
    void flinkSqlGetShouldRequireIdAppIdAndTeamId() {
        FlinkSqlGetRequest request = new FlinkSqlGetRequest();
        request.setAppId(1L);
        request.setTeamId(2L);
        Set<ConstraintViolation<FlinkSqlGetRequest>> violations = validator.validate(request);
        Assertions.assertFalse(violations.isEmpty());
    }

    @Test
    void sparkSqlDeleteShouldRequireSqlAppIdAndTeamId() {
        Set<ConstraintViolation<SparkSqlDeleteRequest>> violations = validator.validate(new SparkSqlDeleteRequest());
        Assertions.assertEquals(3, violations.size());
    }

    @Test
    void sparkSqlGetShouldRequireIdAppIdAndTeamId() {
        SparkSqlGetRequest request = new SparkSqlGetRequest();
        request.setAppId(1L);
        request.setTeamId(2L);
        Set<ConstraintViolation<SparkSqlGetRequest>> violations = validator.validate(request);
        Assertions.assertFalse(violations.isEmpty());
    }
}
