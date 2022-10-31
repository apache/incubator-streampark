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

package com.github.benmanes.caffeine.cache;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

class RefreshCacheTest {

    Cache<String, String> caffeine = null;

    @Test
    void cache() throws Exception {
        if (caffeine == null) {
            caffeine = Caffeine.newBuilder()
                .refreshAfterWrite(50, TimeUnit.MILLISECONDS)
                .build(this::refresh);
        }
        caffeine.put("config", "hadoop");
        int count = 4;
        while (count > 0) {
            System.out.println(caffeine.getIfPresent("config"));
            Thread.sleep(100L);
            --count;
        }
    }

    public String refresh(String value) {
        return UUID.randomUUID() + "@" + value;
    }
}
