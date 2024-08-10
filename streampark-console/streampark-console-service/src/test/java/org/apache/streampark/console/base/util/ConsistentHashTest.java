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

import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConsistentHashTest {

    List<String> servers = Arrays.asList("Server-A", "Server-B", "Server-C", "Server-D", "Server-E");
    ConsistentHash<String> hash = new ConsistentHash<>(servers);
    Integer jobNum = 100000;
    List<Integer> jobIds = new ArrayList<>();

    @BeforeEach
    public void init() {
        for (int i = 1; i <= jobNum; i ++) {
            jobIds.add(i);
        }
    }


    @Test
    public void initWatch() {
        List<Integer> counts = Arrays.asList(0, 0, 0, 0, 0);
        for (int i : jobIds) {
            String v = hash.get(i);
            switch (v) {
        }
    }

    @Test
    public void addWatch() {
        int jobId = 99;
        String v = hash.get(99);
        System.out.println("node:" + v + " , item:" + jobId);
    }

    @Test
    public void stopServer() {
        servers.remove("A");
        int jobId = 99;
        String v = hash.get(99);
        System.out.println("node:" + v + " , item:" + jobId);
    }
}
