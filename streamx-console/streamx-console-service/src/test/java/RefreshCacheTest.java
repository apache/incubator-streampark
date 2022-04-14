/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.streamxhub.streamx.common.util.DateUtils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RefreshCacheTest {

    Cache<String, String> caffeine = null;

    @Test
    public void cache() throws Exception {
        if (caffeine == null) {
            caffeine = Caffeine.newBuilder()
                .refreshAfterWrite(10, TimeUnit.SECONDS)
                .build(this::refresh);
        }
        caffeine.put("config", "hadoop");
        while (true) {
            System.out.println(caffeine.getIfPresent("config"));
            Thread.sleep(2000);
        }
    }

    public String refresh(String value) {
        return UUID.randomUUID() + "@" + value;
    }

    @Test
    public void task() throws InterruptedException {
        System.out.println(DateUtils.format(new Date(), DateUtils.fullFormat(), TimeZone.getDefault()));
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println(DateUtils.format(new Date(), DateUtils.fullFormat(), TimeZone.getDefault()));
            }
        }, 1000 * 10, 1000 * 10);

        Thread.sleep(Long.MAX_VALUE);

    }
}
