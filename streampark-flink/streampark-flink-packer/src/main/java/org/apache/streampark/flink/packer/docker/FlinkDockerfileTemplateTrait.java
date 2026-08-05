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

import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.fs.LfsOperator;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/** Flink image dockerfile template. */
public abstract class FlinkDockerfileTemplateTrait {

    protected static final String DEFAULT_DOCKER_FILE_NAME = "Dockerfile";
    protected static final String FLINK_LIB_PATH = "lib";
    protected static final String FLINK_HOME = "$FLINK_HOME";

    private Path workspace;
    private String mainJarName;
    private String extraLibName;

    public abstract String workspacePath();

    public abstract String flinkBaseImage();

    public abstract String flinkMainJarPath();

    public abstract Set<String> flinkExtraLibPaths();

    public abstract String offerDockerfileContent();

    /** Startup flink main jar path inner Docker */
    public String innerMainJarPath() {
        return "local:///opt/flink/usrlib/" + mainJarName();
    }

    protected Path workspace() {
        if (workspace == null) {
            Path path = Paths.get(workspacePath()).toAbsolutePath();
            if (!LfsOperator.exists(workspacePath())) {
                LfsOperator.mkdirs(workspacePath());
            }
            workspace = path;
        }
        return workspace;
    }

    public String mainJarName() {
        if (mainJarName == null) {
            Path mainJarPath = Paths.get(flinkMainJarPath()).toAbsolutePath();
            if (!mainJarPath.getParent().equals(workspace())) {
                LfsOperator.copy(
                    mainJarPath.toString(),
                    workspace().toString() + "/" + mainJarPath.getFileName().toString());
            }
            mainJarName = mainJarPath.getFileName().toString();
        }
        return mainJarName;
    }

    public String extraLibName() {
        if (extraLibName == null) {
            LfsOperator.mkCleanDirs(workspace().toString() + "/" + FLINK_LIB_PATH);
            for (String libPath : flinkExtraLibPaths()) {
                File f = new File(libPath);
                if (!f.exists() || !f.getName().endsWith(Constants.JAR_SUFFIX)) {
                    continue;
                }
                if (f.isDirectory()) {
                    File[] files = f.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && file.getName().endsWith(Constants.JAR_SUFFIX)) {
                                LfsOperator.copy(file.getAbsolutePath(), workspace().toString() + "/" + FLINK_LIB_PATH);
                            }
                        }
                    }
                } else if (f.isFile()) {
                    LfsOperator.copy(f.getAbsolutePath(), workspace().toString() + "/" + FLINK_LIB_PATH);
                }
            }
            extraLibName = FLINK_LIB_PATH;
        }
        return extraLibName;
    }

    public File writeDockerfile() throws IOException {
        File output = new File(workspacePath() + "/" + DEFAULT_DOCKER_FILE_NAME);
        FileUtils.write(output, offerDockerfileContent(), "UTF-8");
        return output;
    }

    public File writeDockerfile(String dockerfileName) throws IOException {
        File output = new File(workspacePath() + "/" + dockerfileName);
        FileUtils.write(output, offerDockerfileContent(), "UTF-8");
        return output;
    }
}
