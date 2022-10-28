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

package org.apache.streampark.plugin.profiling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

class AgentThreadFactoryTest {

    @Test
    void newThread() throws InterruptedException {
        final AtomicInteger i = new AtomicInteger(10);

        AgentThreadFactory threadFactory = new AgentThreadFactory();
        Thread thread =
            threadFactory.newThread(
                new Runnable() {
                    @Override
                    public void run() {
                        i.incrementAndGet();
                    }
                });

        thread.start();
        thread.join();

        Assertions.assertEquals(11, i.get());
    }
}
