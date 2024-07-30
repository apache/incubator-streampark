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

package org.apache.streampark.console.base.enums;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

@Slf4j
public class StatusTest {

    @Test
    void checkStatusCodeRepeatValue() {
        ServiceLoader<Status> load = ServiceLoader.load(Status.class);
        Iterator<Status> iterator = load.iterator();
        Map<String, Status> beansOfType = null;
        Map<Integer, List<Status>> codeToBeans = new HashMap<>();
        List<Status> repeatStatus = new ArrayList<>();

        for (Map.Entry<String, Status> entry : beansOfType.entrySet()) {
            Status value = entry.getValue();
            Assertions.assertTrue(value.getClass().isEnum());
            Status[] enumConstants = value.getClass().getEnumConstants();
            Arrays.stream(enumConstants).forEach(
                subValue -> codeToBeans.computeIfAbsent(subValue.getCode(), k -> new ArrayList<>()).add(subValue));
        }
        for (Map.Entry<Integer, List<Status>> entry : codeToBeans.entrySet()) {
            if (entry.getValue().size() > 1) {
                repeatStatus.addAll(entry.getValue());
            }
        }
        if (!repeatStatus.isEmpty()) {
            log.error("Repeat the list of status codes:\n{}", repeatStatus);
        }
        Assertions.assertTrue(repeatStatus.isEmpty());
    }
}
