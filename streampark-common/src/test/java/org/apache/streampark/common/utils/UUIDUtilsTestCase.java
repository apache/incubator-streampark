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

package org.apache.streampark.common.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class UUIDUtilsTestCase {

    @Test
    void testGenerateUUID() {
        long uuid = UUIDUtils.generateUUID();
        assertThat(String.valueOf(uuid).length()).isEqualTo(14);
    }

    @Test
    @SneakyThrows
    void testNoDuplicateGenerateUUID() {
        int threadNum = 10;
        int uuidNum = 10000;

        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        Map<String, List<Long>> nodeUUIDs = new ConcurrentHashMap<>();

        for (int i = 0; i < threadNum; i++) {
            new Thread(() -> {
                List<Long> uuids = new ArrayList<>(uuidNum);
                for (int j = 0; j < uuidNum; j++) {
                    uuids.add(UUIDUtils.generateUUID());
                }
                nodeUUIDs.put(Thread.currentThread().getName(), uuids);
                countDownLatch.countDown();
            }).start();
        }

        countDownLatch.await();
        Set<Long> totalUUIDs = new HashSet<>();
        nodeUUIDs.values().forEach(totalUUIDs::addAll);

        assertEquals(uuidNum * threadNum, totalUUIDs.size());
    }
}
