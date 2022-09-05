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

package org.apache.streampark.plugin.profiling.profiler;

import org.apache.streampark.plugin.profiling.Profiler;
import org.apache.streampark.plugin.profiling.Reporter;
import org.apache.streampark.plugin.profiling.util.ProcFileUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IOProfiler extends ProfilerBase implements Profiler {
    public static final String PROFILER_NAME = "IO";

    private long interval = Constants.DEFAULT_METRIC_INTERVAL;

    private Reporter reporter;

    public IOProfiler(Reporter reporter) {
        setReporter(reporter);
    }

    @Override
    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
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
        Long readBytes = ProcFileUtils.getBytesValue(procMap, "read_bytes");
        Long writeBytes = ProcFileUtils.getBytesValue(procMap, "write_bytes");

        List<Map<String, Object>> cpuTime = ProcFileUtils.getProcStatCpuTime();

        Map<String, Object> map = new HashMap<>();

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
        ioMap.put("read_bytes", readBytes);
        ioMap.put("write_bytes", writeBytes);

        map.put("stat", cpuTime);

        if (reporter != null) {
            reporter.report(PROFILER_NAME, map);
        }
    }
}
