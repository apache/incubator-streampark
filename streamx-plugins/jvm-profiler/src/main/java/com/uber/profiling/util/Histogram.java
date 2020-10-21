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

package com.uber.profiling.util;

import java.util.concurrent.atomic.AtomicLong;

public class Histogram {

    private AtomicLong count = new AtomicLong(0);
    private AtomicLong sum = new AtomicLong(0);
    private AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private AtomicLong max = new AtomicLong(Long.MIN_VALUE);

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
