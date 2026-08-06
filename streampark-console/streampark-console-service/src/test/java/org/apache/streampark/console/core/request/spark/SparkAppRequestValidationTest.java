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

package org.apache.streampark.console.core.request.spark;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import java.util.Set;

class SparkAppRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createRequestShouldRequireCoreFields() {
        Set<ConstraintViolation<SparkAppCreateRequest>> violations = validator.validate(new SparkAppCreateRequest());
        Assertions.assertTrue(violations.size() >= 5);
    }

    @Test
    void checkNameRequestShouldRequireAppName() {
        SparkAppCheckNameRequest request = new SparkAppCheckNameRequest();
        request.setTeamId(1L);
        Set<ConstraintViolation<SparkAppCheckNameRequest>> violations = validator.validate(request);
        Assertions.assertFalse(violations.isEmpty());
    }

    @Test
    void startRequestShouldRequireIdAndTeamId() {
        Set<ConstraintViolation<SparkAppStartRequest>> violations = validator.validate(new SparkAppStartRequest());
        Assertions.assertEquals(2, violations.size());
    }
}
