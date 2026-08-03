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

/** Base spark docker file image template. */
public class SparkDockerfileTemplate extends SparkDockerfileTemplateTrait {

    private final String workspacePath;
    private final String sparkBaseImage;
    private final String sparkMainJarPath;
    private final Set<String> sparkExtraLibPaths;

    public SparkDockerfileTemplate(
                                   String workspacePath,
                                   String sparkBaseImage,
                                   String sparkMainJarPath,
                                   Set<String> sparkExtraLibPaths) {
        this.workspacePath = workspacePath;
        this.sparkBaseImage = sparkBaseImage;
        this.sparkMainJarPath = sparkMainJarPath;
        this.sparkExtraLibPaths = sparkExtraLibPaths;
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
        return "FROM " + sparkBaseImage + "\n"
            + "USER root\n"
            + "RUN mkdir -p " + SPARK_HOME + "/usrlib\n"
            + "COPY " + mainJarName() + " " + SPARK_HOME + "/usrlib/" + mainJarName() + "\n"
            + "COPY " + extraLibName() + " " + SPARK_HOME + "/lib/\n";
    }
}
