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

package org.apache.streampark.console.core.component;

import org.apache.streampark.console.core.bean.OpenAPISchema;
import org.apache.streampark.console.core.request.flink.FlinkAppStartRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class RequestDtoSchemaBuilderTest {

    @Test
    void shouldBuildSchemaFromRequestDtoFields() {
        List<OpenAPISchema.Schema> schemas = RequestDtoSchemaBuilder.build(
            FlinkAppStartRequest.class,
            null,
            RequestDtoSchemaBuilder.defaultTypeNames());

        Map<String, OpenAPISchema.Schema> byBindFor = schemas.stream()
            .collect(java.util.stream.Collectors.toMap(OpenAPISchema.Schema::getBindFor, s -> s));

        Assertions.assertTrue(byBindFor.containsKey("id"));
        Assertions.assertTrue(byBindFor.containsKey("restoreOrTriggerSavepoint"));
        Assertions.assertEquals("restoreFromSavepoint", byBindFor.get("restoreOrTriggerSavepoint").getName());
    }

    @Test
    void shouldMarkNotNullFieldsAsRequired() {
        List<OpenAPISchema.Schema> schemas = RequestDtoSchemaBuilder.build(
            FlinkAppStartRequest.class,
            null,
            RequestDtoSchemaBuilder.defaultTypeNames());

        Map<String, OpenAPISchema.Schema> byBindFor = schemas.stream()
            .collect(java.util.stream.Collectors.toMap(OpenAPISchema.Schema::getBindFor, s -> s));

        Assertions.assertTrue(byBindFor.get("id").isRequired());
        Assertions.assertTrue(byBindFor.get("teamId").isRequired());
        Assertions.assertFalse(byBindFor.get("savepointPath").isRequired());
    }
}
