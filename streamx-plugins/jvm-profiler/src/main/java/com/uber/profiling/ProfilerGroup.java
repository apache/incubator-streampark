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

import java.util.ArrayList;
import java.util.List;

public class ProfilerGroup {
    private List<Profiler> oneTimeProfilers;
    private List<Profiler> periodicProfilers;

    public ProfilerGroup(List<Profiler> oneTimeProfilers, List<Profiler> periodicProfilers) {
        this.oneTimeProfilers = new ArrayList<>(oneTimeProfilers);
        this.periodicProfilers = new ArrayList<>(periodicProfilers);
    }

    public List<Profiler> getOneTimeProfilers() {
        return oneTimeProfilers;
    }

    public List<Profiler> getPeriodicProfilers() {
        return periodicProfilers;
    }
}
