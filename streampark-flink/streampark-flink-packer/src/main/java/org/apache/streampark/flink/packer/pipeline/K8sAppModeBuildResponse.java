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

package org.apache.streampark.flink.packer.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sAppModeBuildResponse extends AbstractFlinkBuildResponse {

    private String flinkBaseImage;
    private String mainJarPath;
    private Set<String> extraLibJarPaths;

    public K8sAppModeBuildResponse() {
    }

    public K8sAppModeBuildResponse(
                                   String workspacePath,
                                   String flinkBaseImage,
                                   String mainJarPath,
                                   Set<String> extraLibJarPaths,
                                   boolean pass) {
        super(workspacePath, pass);
        this.flinkBaseImage = flinkBaseImage;
        this.mainJarPath = mainJarPath;
        this.extraLibJarPaths = extraLibJarPaths;
    }

    @JsonProperty("flinkBaseImage")
    public String flinkBaseImage() {
        return flinkBaseImage;
    }

    @JsonProperty("mainJarPath")
    public String mainJarPath() {
        return mainJarPath;
    }

    @JsonProperty("extraLibJarPaths")
    public Set<String> extraLibJarPaths() {
        return extraLibJarPaths;
    }

    @JsonProperty("flinkBaseImage")
    public void setFlinkBaseImage(String flinkBaseImage) {
        this.flinkBaseImage = flinkBaseImage;
    }

    @JsonProperty("mainJarPath")
    public void setMainJarPath(String mainJarPath) {
        this.mainJarPath = mainJarPath;
    }

    @JsonProperty("extraLibJarPaths")
    public void setExtraLibJarPaths(Set<String> extraLibJarPaths) {
        this.extraLibJarPaths = extraLibJarPaths;
    }

    @Override
    public String toString() {
        return "{ workspacePath: "
            + workspacePath()
            + ", flinkBaseImage: "
            + flinkBaseImage
            + ", mainJarPath: "
            + mainJarPath
            + ", extraLibJarPaths: "
            + extraLibJarPaths
            + ", pass: "
            + pass()
            + " }";
    }
}
