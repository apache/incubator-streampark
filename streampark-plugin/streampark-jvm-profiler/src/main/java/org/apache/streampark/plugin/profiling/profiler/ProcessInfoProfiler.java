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

import org.apache.streampark.plugin.profiling.AgentImpl;
import org.apache.streampark.plugin.profiling.Profiler;
import org.apache.streampark.plugin.profiling.Reporter;
import org.apache.streampark.plugin.profiling.util.ProcFileUtils;
import org.apache.streampark.plugin.profiling.util.ProcessUtils;
import org.apache.streampark.plugin.profiling.util.SparkAppCmdInfo;
import org.apache.streampark.plugin.profiling.util.SparkUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessInfoProfiler extends ProfilerBase implements Profiler {
    public static final String PROFILER_NAME = "ProcessInfo";

    private String jvmInputArguments = "";
    private String jvmClassPath = "";
    private Long jvmXmxBytes = null;
    private String cmdline = "";

    private Reporter reporter;

    public ProcessInfoProfiler(Reporter reporter) {
        setReporter(reporter);

        init();
    }

    @Override
    public long getInterval() {
        return 0;
    }

    @Override
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void profile() {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("agentVersion", AgentImpl.VERSION);

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

        // TODO support non spark application
        // TODO also possible to use SparkContext to get spark jar/class info

        SparkAppCmdInfo cmdInfo = SparkUtils.probeCmdInfo();
        if (cmdInfo != null) {
            map.put("appJar", cmdInfo.getAppJar());
            map.put("appClass", cmdInfo.getAppClass());

            // TODO add app arguments
        }

        if (getRole() != null) {
            map.put("role", getRole());
        }

        if (jvmXmxBytes != null) {
            map.put("xmxBytes", jvmXmxBytes);
        }

        String jvmInputArgumentsToReport = jvmInputArguments;
        String jvmClassPathToReport = jvmClassPath;

        // Do not report jvmInputArguments and jvmClassPath if cmdline is not empty.
        // This is because cmdline will contain duplicate information for
        // jvmInputArguments/jvmClassPath.
        if (!cmdline.isEmpty()) {
            jvmInputArgumentsToReport = "";
            jvmClassPathToReport = "";
        }

        if (jvmInputArgumentsToReport.length() + jvmClassPathToReport.length() + cmdline.length() <= Constants.MAX_STRING_LENGTH) {
            map.put("jvmInputArguments", jvmInputArgumentsToReport);
            map.put("jvmClassPath", jvmClassPathToReport);
            map.put("cmdline", cmdline);

            if (reporter != null) {
                reporter.report(PROFILER_NAME, map);
            }
        } else {
            List<String> jvmInputArgumentsFragements =
                org.apache.streampark.plugin.profiling.util.StringUtils.splitByLength(
                    jvmInputArgumentsToReport, Constants.MAX_STRING_LENGTH);
            List<String> jvmClassPathFragements =
                org.apache.streampark.plugin.profiling.util.StringUtils.splitByLength(
                    jvmClassPathToReport, Constants.MAX_STRING_LENGTH);
            List<String> cmdlineFragements =
                org.apache.streampark.plugin.profiling.util.StringUtils.splitByLength(
                    cmdline, Constants.MAX_STRING_LENGTH);

            long fragmentSeq = 0;
            long fragmentCount =
                jvmInputArgumentsFragements.size()
                    + jvmClassPathFragements.size()
                    + cmdlineFragements.size();

            for (String entry : jvmInputArgumentsFragements) {
                Map<String, Object> fragmentMap = createFragmentMap(map, fragmentSeq++, fragmentCount);
                fragmentMap.put("jvmInputArguments", entry);

                if (reporter != null) {
                    reporter.report(PROFILER_NAME, fragmentMap);
                }
            }

            for (String entry : jvmClassPathFragements) {
                Map<String, Object> fragmentMap = createFragmentMap(map, fragmentSeq++, fragmentCount);
                fragmentMap.put("jvmClassPath", entry);

                if (reporter != null) {
                    reporter.report(PROFILER_NAME, fragmentMap);
                }
            }

            for (String entry : cmdlineFragements) {
                Map<String, Object> fragmentMap = createFragmentMap(map, fragmentSeq++, fragmentCount);
                fragmentMap.put("cmdline", entry);

                if (reporter != null) {
                    reporter.report(PROFILER_NAME, fragmentMap);
                }
            }
        }
    }

    private void init() {
        jvmInputArguments = StringUtils.join(ProcessUtils.getJvmInputArguments(), " ");
        jvmClassPath = ProcessUtils.getJvmClassPath();
        jvmXmxBytes = ProcessUtils.getJvmXmxBytes();

        cmdline = ProcFileUtils.getCmdline();
        if (cmdline == null) {
            cmdline = "";
        }
    }

    private Map<String, Object> createFragmentMap(
        Map<String, Object> copyFrom, long fragmentSeq, long fragmentCount) {
        Map<String, Object> fragmentMap = new HashMap<String, Object>(copyFrom);
        fragmentMap.put("fragmentSeq", fragmentSeq);
        fragmentMap.put("fragmentCount", fragmentCount);
        fragmentMap.put("jvmInputArguments", "");
        fragmentMap.put("jvmClassPath", "");
        fragmentMap.put("cmdline", "");

        return fragmentMap;
    }
}
