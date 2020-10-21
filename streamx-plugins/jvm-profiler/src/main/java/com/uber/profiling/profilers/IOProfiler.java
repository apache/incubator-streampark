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

package com.uber.profiling.profilers;

import com.uber.profiling.Profiler;
import com.uber.profiling.Reporter;
import com.uber.profiling.util.ProcFileUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IOProfiler extends ProfilerBase implements Profiler {
    public final static String PROFILER_NAME = "IO";

    private long intervalMillis = Constants.DEFAULT_METRIC_INTERVAL;

    private Reporter reporter;

    public IOProfiler(Reporter reporter) {
        setReporter(reporter);
    }

    @Override
    public long getIntervalMillis() {
        return intervalMillis;
    }

    public void setIntervalMillis(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public synchronized void profile() {
        // See http://man7.org/linux/man-pages/man5/proc.5.html for details about /proc/[pid]/io
        Map<String, String> procMap = ProcFileUtils.getProcIO();
        Long rchar = ProcFileUtils.getBytesValue(procMap, "rchar");
        Long wchar = ProcFileUtils.getBytesValue(procMap, "wchar");
        Long read_bytes = ProcFileUtils.getBytesValue(procMap, "read_bytes");
        Long write_bytes = ProcFileUtils.getBytesValue(procMap, "write_bytes");

        List<Map<String, Object>> cpuTime = ProcFileUtils.getProcStatCpuTime();
        
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("epochMillis", System.currentTimeMillis());
        map.put("name", getProcessName());
        map.put("host", getHostName());
        map.put("processUuid", getProcessUuid());
        map.put("appId", getAppId());

        if (getTag() != null) {
            map.put("tag", getTag());
        }

        if (getCluster() != null) {
            map.put("cluster", getCluster());
        }
        
        if (getRole() != null) {
            map.put("role", getRole());
        }

        Map<String, Object> selfMap = new HashMap<String, Object>();
        map.put("self", selfMap);
        
        Map<String, Object> ioMap = new HashMap<String, Object>();
        selfMap.put("io", ioMap);

        ioMap.put("rchar", rchar);
        ioMap.put("wchar", wchar);
        ioMap.put("read_bytes", read_bytes);
        ioMap.put("write_bytes", write_bytes);

        map.put("stat", cpuTime);
        
        if (reporter != null) {
            reporter.report(PROFILER_NAME, map);
        }
    }
}
