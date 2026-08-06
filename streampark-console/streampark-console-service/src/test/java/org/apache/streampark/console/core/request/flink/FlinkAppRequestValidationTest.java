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

package org.apache.streampark.console.core.request.flink;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import java.util.Set;

class FlinkAppRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createRequestShouldRequireCoreFields() {
        FlinkAppCreateRequest request = new FlinkAppCreateRequest();
        Set<ConstraintViolation<FlinkAppCreateRequest>> violations = validator.validate(request);
        Assertions.assertTrue(violations.size() >= 5);
    }

    @Test
    void createRequestShouldPassWithRequiredFields() {
        FlinkAppCreateRequest request = new FlinkAppCreateRequest();
        request.setTeamId(1L);
        request.setJobType(2);
        request.setDeployMode(4);
        request.setVersionId(1L);
        request.setAppType(2);
        request.setJobName("demo");

        Set<ConstraintViolation<FlinkAppCreateRequest>> violations = validator.validate(request);
        Assertions.assertTrue(violations.isEmpty());
    }
}
