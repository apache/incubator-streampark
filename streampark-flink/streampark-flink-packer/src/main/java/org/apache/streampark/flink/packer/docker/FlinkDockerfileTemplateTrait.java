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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public abstract class FlinkDockerfileTemplateTrait {

    protected static final String DEFAULT_DOCKER_FILE_NAME = "Dockerfile";
    protected static final String FLINK_LIB_PATH = "lib";
    protected static final String FLINK_HOME = "$FLINK_HOME";

    public abstract String workspacePath();
    public abstract String flinkBaseImage();
    public abstract String flinkMainJarPath();
    public abstract Set<String> flinkExtraLibPaths();
    public abstract String offerDockerfileContent();

    public String innerMainJarPath() {
        return "local:///opt/flink/usrlib/" + mainJarName();
    }

    protected Path workspace() {
        Path path = Paths.get(workspacePath()).toAbsolutePath();
        if (!LfsOperator.getInstance().exists(workspacePath())) {
            LfsOperator.getInstance().mkdirs(workspacePath());
        }
        return path;
    }

    protected String mainJarName() {
        Path mainJarPath = Paths.get(flinkMainJarPath()).toAbsolutePath();
        if (!mainJarPath.getParent().equals(workspace())) {
            LfsOperator.getInstance().copy(mainJarPath.toString(),
                workspace().toString() + "/" + mainJarPath.getFileName());
        }
        return mainJarPath.getFileName().toString();
    }

    protected String extraLibName() {
        LfsOperator.getInstance().mkCleanDirs(workspace().toString() + "/" + FLINK_LIB_PATH);
        for (String libPath : flinkExtraLibPaths()) {
            File f = new File(libPath);
            if (!f.exists() || !f.getName().endsWith(Constants.JAR_SUFFIX))
                continue;
            if (f.isDirectory()) {
                for (File jar : f.listFiles()) {
                    if (jar.isFile() && jar.getName().endsWith(Constants.JAR_SUFFIX)) {
                        LfsOperator.getInstance().copy(jar.getAbsolutePath(),
                            workspace().toString() + "/" + FLINK_LIB_PATH);
                    }
                }
            } else {
                LfsOperator.getInstance().copy(f.getAbsolutePath(), workspace().toString() + "/" + FLINK_LIB_PATH);
            }
        }
        return FLINK_LIB_PATH;
    }

    public File writeDockerfile() throws Exception {
        File output = new File(workspacePath() + "/" + DEFAULT_DOCKER_FILE_NAME);
        FileUtils.write(output, offerDockerfileContent(), "UTF-8");
        return output;
    }
}
