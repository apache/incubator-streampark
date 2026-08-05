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

import java.util.Set;

/** Base flink docker file image template. */
public class FlinkDockerfileTemplate extends FlinkDockerfileTemplateTrait {

    private final String workspacePath;
    private final String flinkBaseImage;
    private final String flinkMainJarPath;
    private final Set<String> flinkExtraLibPaths;

    public FlinkDockerfileTemplate(
                                   String workspacePath,
                                   String flinkBaseImage,
                                   String flinkMainJarPath,
                                   Set<String> flinkExtraLibPaths) {
        this.workspacePath = workspacePath;
        this.flinkBaseImage = flinkBaseImage;
        this.flinkMainJarPath = flinkMainJarPath;
        this.flinkExtraLibPaths = flinkExtraLibPaths;
    }

    @Override
    public String workspacePath() {
        return workspacePath;
    }

    @Override
    public String flinkBaseImage() {
        return flinkBaseImage;
    }

    @Override
    public String flinkMainJarPath() {
        return flinkMainJarPath;
    }

    @Override
    public Set<String> flinkExtraLibPaths() {
        return flinkExtraLibPaths;
    }

    @Override
    public String offerDockerfileContent() {
        return "FROM " + flinkBaseImage + "\n"
            + "RUN mkdir -p " + FLINK_HOME + "/usrlib\n"
            + "COPY " + extraLibName() + " " + FLINK_HOME + "/lib/\n"
            + "COPY " + mainJarName() + " " + FLINK_HOME + "/usrlib/" + mainJarName() + "\n";
    }
}
