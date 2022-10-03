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

package org.apache.streampark.plugin.profiling.util;

import java.util.concurrent.atomic.AtomicLong;

public class Histogram {

    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong sum = new AtomicLong(0);
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

    public void appendValue(long value) {
        count.incrementAndGet();
        sum.addAndGet(value);

        min.updateAndGet(x -> value < x ? value : x);
        max.updateAndGet(x -> value > x ? value : x);
    }

    public long getCount() {
        return count.get();
    }

    public long getSum() {
        return sum.get();
    }

    public long getMin() {
        return min.get();
    }

    public long getMax() {
        return max.get();
    }
}
