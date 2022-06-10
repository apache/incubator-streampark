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

import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.PropertiesUtils;

import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class YarnTest {

    @Test
    public void vcore() throws IOException, YarnException {
        int numYarnMaxVcores = HadoopUtils.yarnClient().getNodeReports(NodeState.RUNNING)
            .stream()
            .mapToInt(report -> report.getCapability().getVirtualCores())
            .max()
            .orElse(0);
        System.out.println(numYarnMaxVcores);
    }

    @Test
    public void getURL() {
        /**
         * 将hadoop的配置文件放到一个目录下,
         * 在运行该类的时候加上jvm级别的参数(idea里的 vmOption ) -DHADOOP_CONF_DIR=${目录}
         */
        String url = HadoopUtils.getRMWebAppURL(true);
        System.out.println(url);
    }

    @Test
    public void loadFlinkYaml() {
        String path = System.getenv("FLINK_HOME").concat("/conf/flink-conf.yaml");
        File yaml = new File(path);
        Map<String, String> map = PropertiesUtils.loadFlinkConfYaml(yaml);
        System.out.println(map.size());
    }

}
