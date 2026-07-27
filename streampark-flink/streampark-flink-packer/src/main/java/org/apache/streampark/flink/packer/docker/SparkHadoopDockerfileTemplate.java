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

import javax.annotation.Nullable;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

/** spark-hadoop integration docker image template. */
public class SparkHadoopDockerfileTemplate extends SparkDockerfileTemplateTrait {

    private final String workspacePath;
    private final String sparkBaseImage;
    private final String sparkMainJarPath;
    private final Set<String> sparkExtraLibPaths;
    private final String hadoopConfDir;
    private final String hiveConfDir;

    public SparkHadoopDockerfileTemplate(
                                         String workspacePath,
                                         String sparkBaseImage,
                                         String sparkMainJarPath,
                                         Set<String> sparkExtraLibPaths,
                                         @Nullable String hadoopConfDirPath,
                                         @Nullable String hiveConfDirPath) {
        this.workspacePath = workspacePath;
        this.sparkBaseImage = sparkBaseImage;
        this.sparkMainJarPath = sparkMainJarPath;
        this.sparkExtraLibPaths = sparkExtraLibPaths;
        this.hadoopConfDir =
            workspace()
                .relativize(Paths.get(hadoopConfDirPath == null ? "" : hadoopConfDirPath))
                .toString();
        this.hiveConfDir =
            workspace()
                .relativize(Paths.get(hiveConfDirPath == null ? "" : hiveConfDirPath))
                .toString();
    }

    @Override
    public String workspacePath() {
        return workspacePath;
    }

    @Override
    public String sparkBaseImage() {
        return sparkBaseImage;
    }

    @Override
    public String sparkMainJarPath() {
        return sparkMainJarPath;
    }

    @Override
    public Set<String> sparkExtraLibPaths() {
        return sparkExtraLibPaths;
    }

    @Override
    public String offerDockerfileContent() {
        StringBuilder dockerfile =
            new StringBuilder(
                "FROM " + sparkBaseImage + "\n"
                    + "RUN mkdir -p " + SPARK_HOME + "/usrlib\n");
        if (!hadoopConfDir.isEmpty()) {
            dockerfile
                .append("COPY ")
                .append(hadoopConfDir)
                .append(" /opt/hadoop-conf\n")
                .append("ENV HADOOP_CONF_DIR /opt/hadoop-conf\n");
        }
        if (!hiveConfDir.isEmpty()) {
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
            .append(SPARK_HOME)
            .append("/lib/\n")
            .append("COPY ")
            .append(mainJarName())
            .append(" ")
            .append(SPARK_HOME)
            .append("/usrlib/")
            .append(mainJarName())
            .append("\n");
        return dockerfile.toString();
    }

    public static SparkHadoopDockerfileTemplate fromSystemHadoopConf(
                                                                     String workspacePath,
                                                                     String sparkBaseImage,
                                                                     String sparkMainJarPath,
                                                                     Set<String> sparkExtraLibPaths) {
        String hadoopConfDir = "";
        Optional<String> systemHadoopConf = HadoopConfigUtils.getSystemHadoopConfDirAsJava();
        if (systemHadoopConf.isPresent() && LfsOperator.exists(systemHadoopConf.get())) {
            hadoopConfDir = workspacePath + "/hadoop-conf";
            LfsOperator.mkCleanDirs(hadoopConfDir);
            LfsOperator.copyDir(systemHadoopConf.get(), hadoopConfDir);
        }
        String hiveConfDir = "";
        Optional<String> systemHiveConf = HadoopConfigUtils.getSystemHiveConfDirAsJava();
        if (systemHiveConf.isPresent() && LfsOperator.exists(systemHiveConf.get())) {
            hiveConfDir = workspacePath + "/hive-conf";
            LfsOperator.mkCleanDirs(hiveConfDir);
            LfsOperator.copyDir(systemHiveConf.get(), hiveConfDir);
        }
        return new SparkHadoopDockerfileTemplate(
            workspacePath,
            sparkBaseImage,
            sparkMainJarPath,
            sparkExtraLibPaths,
            hadoopConfDir,
            hiveConfDir);
    }
}
