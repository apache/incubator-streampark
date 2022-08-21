/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.File;
import java.io.IOException;

public class FlinkJobWatch {

    public static String jobDeploymentsWatch(String nameSpace, String jobName) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            String log = client.apps().deployments()
                .inNamespace(nameSpace)
                .withName(jobName).getLog();

            File dir = new File("");
            String projectPath = dir.getCanonicalPath();

            String path = projectPath + "/" + nameSpace + "_" + jobName + ".log";
            File file = new File(path);
            Files.asCharSink(file, Charsets.UTF_8).write(log);
            return path;
        } catch (IOException e) {
            return null;
        }

    }
}
