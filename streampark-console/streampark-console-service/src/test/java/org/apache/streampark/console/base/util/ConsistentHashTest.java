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

package org.apache.streampark.console.base.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ConsistentHashTest {

    Long startTime;

    List<String> servers;

    ConsistentHash<String> hash;

    Integer jobNum = 300000;

    List<Integer> jobIds = new ArrayList<>();

    @BeforeEach
    public void init() {
        startTime = System.currentTimeMillis();
        servers = new ArrayList<>(Arrays.asList("Server-A", "Server-B", "Server-C"));
        hash = new ConsistentHash<>(servers);
        for (int i = 0; i < jobNum; i++) {
            jobIds.add(i);
        }
    }

    @Test
    public void initWatch() {
        Map<String, Long> counts = jobIds.stream()
            .map(hash::get)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        counts.forEach((k, v) -> {
            log.info("node:{}, initWatch count:{}", k, v);
        });
        log.info("time: {}ms", System.currentTimeMillis() - startTime);
    }

    @Test
    public void addServer() {
        servers.add("Server-D");
        hash.add("Server-D");
        Map<String, Long> counts = jobIds.stream()
            .map(hash::get)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        counts.forEach((k, v) -> {
            log.info("node:{}, addServer count:{}", k, v);
        });
        log.info("time: {}ms", System.currentTimeMillis() - startTime);
    }

    @Test
    public void removeServer() {
        servers.remove("Server-C");
        hash.remove("Server-C");
        Map<String, Long> counts = jobIds.stream()
            .map(hash::get)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        counts.forEach((k, v) -> {
            log.info("node:{}, removeServer count:{}", k, v);
        });
        log.info("time: {}ms", System.currentTimeMillis() - startTime);
    }
}
