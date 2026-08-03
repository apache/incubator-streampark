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

package org.apache.streampark.flink.kubernetes;

import org.apache.streampark.common.util.ThreadUtils;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChangeEventBus {

    private static final int CPU_NUM = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

    private final ThreadPoolExecutor execPool =
        new ThreadPoolExecutor(
            CPU_NUM,
            CPU_NUM * 5,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            ThreadUtils.threadFactory("streampark-k8s-watching-thread"));

    final AsyncEventBus asyncEventBus =
        new AsyncEventBus("[StreamPark][flink-k8s]AsyncEventBus", execPool);

    final EventBus syncEventBus = new EventBus("[StreamPark][flink-k8s]SyncEventBus");

    public void postAsync(Object event) {
        asyncEventBus.post(event);
    }

    public void postSync(Object event) {
        syncEventBus.post(event);
    }

    public void registerListener(Object listener) {
        asyncEventBus.register(listener);
        syncEventBus.register(listener);
    }
}
