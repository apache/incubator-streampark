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

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerImageBuildResponse implements FlinkBuildResult {

    private String workspacePath;
    private String flinkImageTag;
    private Map<String, String> podTemplatePaths;
    private String dockerInnerMainJarPath;
    private boolean pass = true;

    public DockerImageBuildResponse() {
    }

    public DockerImageBuildResponse(
                                    String workspacePath,
                                    String flinkImageTag,
                                    Map<String, String> podTemplatePaths,
                                    String dockerInnerMainJarPath) {
        this(workspacePath, flinkImageTag, podTemplatePaths, dockerInnerMainJarPath, true);
    }

    public DockerImageBuildResponse(
                                    String workspacePath,
                                    String flinkImageTag,
                                    Map<String, String> podTemplatePaths,
                                    String dockerInnerMainJarPath,
                                    boolean pass) {
        this.workspacePath = workspacePath;
        this.flinkImageTag = flinkImageTag;
        this.podTemplatePaths = podTemplatePaths;
        this.dockerInnerMainJarPath = dockerInnerMainJarPath;
        this.pass = pass;
    }

    @Override
    public String workspacePath() {
        return workspacePath;
    }

    public String flinkImageTag() {
        return flinkImageTag;
    }

    public Map<String, String> podTemplatePaths() {
        return podTemplatePaths;
    }

    public String dockerInnerMainJarPath() {
        return dockerInnerMainJarPath;
    }

    @Override
    public boolean pass() {
        return pass;
    }

    @JsonProperty("workspacePath")
    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    @JsonProperty("flinkImageTag")
    public void setFlinkImageTag(String flinkImageTag) {
        this.flinkImageTag = flinkImageTag;
    }

    @JsonProperty("podTemplatePaths")
    public void setPodTemplatePaths(Map<String, String> podTemplatePaths) {
        this.podTemplatePaths = podTemplatePaths;
    }

    @JsonProperty("dockerInnerMainJarPath")
    public void setDockerInnerMainJarPath(String dockerInnerMainJarPath) {
        this.dockerInnerMainJarPath = dockerInnerMainJarPath;
    }

    @JsonProperty("pass")
    public void setPass(boolean pass) {
        this.pass = pass;
    }

    @Override
    public String toString() {
        return "{ workspacePath: "
            + workspacePath
            + ", flinkImageTag: "
            + flinkImageTag
            + ", podTemplatePaths: "
            + podTemplatePaths
            + ", dockerInnerMainJarPath: "
            + dockerInnerMainJarPath
            + ", pass: "
            + pass
            + " }";
    }
}
