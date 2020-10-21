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

public class ProfilerRunnableTest {
    @Test
    public void invokeRunnable() {
        final AtomicInteger i = new AtomicInteger(10);

        ProfilerRunner profilerRunnable = new ProfilerRunner(new Profiler() {
            @Override
            public long getIntervalMillis() {
                return 0;
            }

            @Override
            public void setReporter(Reporter reporter) {
            }

            @Override
            public void profile() {
                i.incrementAndGet();
            }
        });

        profilerRunnable.run();

        Assert.assertEquals(11, i.get());
    }
}
