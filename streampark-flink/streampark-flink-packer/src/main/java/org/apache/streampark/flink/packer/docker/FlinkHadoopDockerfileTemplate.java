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

package org.apache.streampark.flink.packer.docker;

import org.apache.streampark.common.fs.LfsOperator;
import org.apache.streampark.common.util.HadoopConfigUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;

import java.nio.file.Paths;
import java.util.Set;

@Data
@Accessors(fluent = true)
@AllArgsConstructor
public class FlinkHadoopDockerfileTemplate extends FlinkDockerfileTemplateTrait {
    private String workspacePath;
    private String flinkBaseImage;
    private String flinkMainJarPath;
    private Set<String> flinkExtraLibPaths;
    @Nullable private String hadoopConfDirPath;
    @Nullable private String hiveConfDirPath;

    @Override
    public String offerDockerfileContent() {
        String hadoopConfDir =
                workspace().relativize(Paths.get(hadoopConfDirPath == null ? "" : hadoopConfDirPath))
                        .toString();
        String hiveConfDir =
                workspace().relativize(Paths.get(hiveConfDirPath == null ? "" : hiveConfDirPath))
                        .toString();
        StringBuilder dockerfile =
                new StringBuilder("FROM ")
                        .append(flinkBaseImage)
                        .append("\nRUN mkdir -p ")
                        .append(FLINK_HOME)
                        .append("/usrlib\n");
        if (hadoopConfDir != null && !hadoopConfDir.isEmpty()) {
            dockerfile
                    .append("COPY ")
                    .append(hadoopConfDir)
                    .append(" /opt/hadoop-conf\n")
                    .append("ENV HADOOP_CONF_DIR /opt/hadoop-conf\n");
        }
        if (hiveConfDir != null && !hiveConfDir.isEmpty()) {
            dockerfile
                    .append("COPY ")
                    .append(hiveConfDir)
                    .append(" /opt/hive-conf\n")
                    .append("ENV HIVE_CONF_DIR /opt/hive-conf\n");
        }
        dockerfile
                .append("COPY ")
                .append(extraLibName())
                .append(" ")
                .append(FLINK_HOME)
                .append("/lib/\n")
                .append("COPY ")
                .append(mainJarName())
                .append(" ")
                .append(FLINK_HOME)
                .append("/usrlib/")
                .append(mainJarName())
                .append("\n");
        return dockerfile.toString();
    }

    public static FlinkHadoopDockerfileTemplate fromSystemHadoopConf(
            String workspacePath,
            String flinkBaseImage,
            String flinkMainJarPath,
            Set<String> flinkExtraLibPaths) {
        String hadoopConfDir = resolveConfDir(HadoopConfigUtils.getSystemHadoopConfDir().orElse(null), workspacePath, "hadoop-conf");
        String hiveConfDir = resolveConfDir(HadoopConfigUtils.getSystemHiveConfDir().orElse(null), workspacePath, "hive-conf");
        return new FlinkHadoopDockerfileTemplate(
                workspacePath,
                flinkBaseImage,
                flinkMainJarPath,
                flinkExtraLibPaths,
                hadoopConfDir,
                hiveConfDir);
    }

    private static String resolveConfDir(String path, String workspacePath, String dirName) {
        if (path == null || !LfsOperator.getInstance().exists(path)) {
            return "";
        }
        String dstDir = workspacePath + "/" + dirName;
        LfsOperator.getInstance().mkCleanDirs(dstDir);
        LfsOperator.getInstance().copyDir(path, dstDir);
        return dstDir;
    }
}
