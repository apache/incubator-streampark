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

package com.uber.profiling.reporters;

import com.uber.profiling.Reporter;
import com.uber.profiling.util.JsonUtils;

import java.util.List;
import java.util.Map;

public class ConsoleOutputReporter implements Reporter {
    @Override
    public void report(String profilerName, Map<String, Object> metrics) {
        System.out.println(String.format("ConsoleOutputReporter - %s: %s", profilerName, JsonUtils.serialize(metrics)));
    }

    @Override
    public void close() {
    }
}
