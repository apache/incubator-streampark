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

package org.apache.streampark.flink.client.bean;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClientBeanUtilsTest {

    @Test
    void toSerializableMapShouldConvertNonSerializableValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("serializable", "value");
        input.put("custom", new Object());

        Map<String, Serializable> result = ClientBeanUtils.toSerializableMap(input);

        assertThat(result).containsEntry("serializable", "value");
        assertThat(result.get("custom")).isNotNull();
    }

    @Test
    void copyPropertiesMapShouldReturnNullForNullInput() {
        assertThat(ClientBeanUtils.copyPropertiesMap(null)).isNull();
    }

    @Test
    void toSerializableMapShouldReturnNullForNullInput() {
        assertThat(ClientBeanUtils.toSerializableMap(null)).isNull();
    }
}
