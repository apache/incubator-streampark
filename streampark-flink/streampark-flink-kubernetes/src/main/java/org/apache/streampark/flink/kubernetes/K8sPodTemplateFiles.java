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

package org.apache.streampark.flink.kubernetes;

import org.apache.flink.configuration.Configuration;

import java.util.Map;

/**
 * @param tmplFiles key of flink pod template configuration -> absolute file path of pod template
 */
public class K8sPodTemplateFiles {

    private final Map<String, String> tmplFiles;

    public K8sPodTemplateFiles(Map<String, String> tmplFiles) {
        this.tmplFiles = tmplFiles;
    }

    public Map<String, String> tmplFiles() {
        return tmplFiles;
    }

    /** merge k8s pod template configuration to Flink Configuration */
    public void mergeToFlinkConf(Configuration flinkConf) {
        tmplFiles.entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
            .forEach(e -> flinkConf.setString(e.getKey(), e.getValue()));
    }
}
