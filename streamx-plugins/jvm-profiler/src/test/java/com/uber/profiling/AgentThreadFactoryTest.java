/*
 * Copyright (c) 2018 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.profiling;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class AgentThreadFactoryTest {
    @Test
    public void newThread() throws InterruptedException {
        final AtomicInteger i = new AtomicInteger(10);

        AgentThreadFactory threadFactory = new AgentThreadFactory();
        Thread thread = threadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                i.incrementAndGet();
            }
        });

        thread.start();
        thread.join();

        Assert.assertEquals(11, i.get());
    }
}
