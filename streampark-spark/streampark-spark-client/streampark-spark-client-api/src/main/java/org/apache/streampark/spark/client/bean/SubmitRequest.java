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

package org.apache.streampark.spark.client.bean;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.SparkVersion;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.SparkDeployMode;
import org.apache.streampark.common.enums.SparkJobType;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.HdfsUtils;
import org.apache.streampark.common.util.JsonUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class SubmitRequest {

    private static final Map<String, String> DEFAULT_SUBMIT_PARAM;

    static {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("spark.driver.cores", "1");
        defaults.put("spark.driver.memory", "1g");
        defaults.put("spark.executor.cores", "1");
        defaults.put("spark.executor.memory", "1g");
        defaults.put("spark.executor.instances", "2");
        DEFAULT_SUBMIT_PARAM = Collections.unmodifiableMap(defaults);
    }

    private SparkVersion sparkVersion;
    private SparkDeployMode deployMode;
    private String sparkYaml;
    private SparkJobType jobType;
    private long id;
    private String appName;
    private String mainClass;
    private String appConf;
    private Map<String, String> appProperties = new HashMap<>();
    private List<String> appArgs;
    private ApplicationType applicationType;
    @Nullable
    private String hadoopUser;
    @Nullable
    private BuildResult buildResult;
    @Nullable
    private Map<String, Object> extraParameter;

    public SubmitRequest(
                         SparkVersion sparkVersion,
                         SparkDeployMode deployMode,
                         String sparkYaml,
                         SparkJobType jobType,
                         long id,
                         String appName,
                         String mainClass,
                         String appConf,
                         Map<String, String> appProperties,
                         List<String> appArgs,
                         ApplicationType applicationType,
                         String hadoopUser,
                         BuildResult buildResult,
                         Map<String, Object> extraParameter) {
        this.sparkVersion = sparkVersion;
        this.deployMode = deployMode;
        this.sparkYaml = sparkYaml;
        this.jobType = jobType;
        this.id = id;
        this.appName = appName;
        this.mainClass = mainClass;
        this.appConf = appConf;
        this.appProperties = appProperties;
        this.appArgs = appArgs;
        this.applicationType = applicationType;
        this.hadoopUser = hadoopUser;
        this.buildResult = buildResult;
        this.extraParameter = extraParameter;
    }

    public Map<String, String> getSparkParameterMap() {
        return getParameterMap(ConfigKeys.KEY_SPARK_PROPERTY_PREFIX);
    }

    public String getAppMain() {
        switch (jobType) {
            case SPARK_SQL:
                return Constants.STREAMPARK_SPARKSQL_CLIENT_CLASS;
            case SPARK_JAR:
            case PYSPARK:
                return mainClass;
            default:
                throw new IllegalArgumentException("Unknown deployment Mode");
        }
    }

    public String getUserJarPath() {
        checkBuildResult();
        return ((ShadedBuildResponse) buildResult).shadedJarPath();
    }

    public boolean hasExtra(String key) {
        return MapUtils.isNotEmpty(extraParameter) && extraParameter.containsKey(key);
    }

    public Object getExtra(String key) {
        return extraParameter.get(key);
    }

    private Map<String, String> getParameterMap(String prefix) {
        if (appConf == null) {
            return Collections.emptyMap();
        }
        if (!appConf.startsWith("json://") && appConf.length() < 7) {
            throw new IllegalArgumentException("[StreamPark] application config format error.");
        }
        String format = appConf.substring(0, 7);
        Map<String, String> map;
        if ("json://".equals(format)) {
            String json = appConf.substring(7);
            try {
                map = JsonUtils.read(json, Map.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("[StreamPark] application config format error.", e);
            }
        } else {
            String content = DeflaterUtils.unzipString(appConf.trim().substring(7));
            switch (format) {
                case "yaml://":
                    map = PropertiesUtils.fromYamlText(content);
                    break;
                case "conf://":
                    map = PropertiesUtils.fromHoconText(content);
                    break;
                case "prop://":
                    map = PropertiesUtils.fromPropertiesText(content);
                    break;
                case "hdfs://":
                    try {
                        String text = HdfsUtils.read(appConf);
                        int dotIndex = appConf.lastIndexOf('.');
                        String extension =
                            dotIndex >= 0 ? appConf.substring(dotIndex + 1).toLowerCase() : "";
                        switch (extension) {
                            case "yml":
                            case "yaml":
                                map = PropertiesUtils.fromYamlText(text);
                                break;
                            case "conf":
                                map = PropertiesUtils.fromHoconText(text);
                                break;
                            case "properties":
                                map = PropertiesUtils.fromPropertiesText(text);
                                break;
                            default:
                                throw new IllegalArgumentException(
                                    "[StreamPark] Usage: application config format error,must be [yaml|conf|properties]");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("[StreamPark] application config format error.");
            }
        }
        return map.entrySet().stream()
            .filter(e -> e.getKey().startsWith(prefix))
            .filter(e -> StringUtils.isNotEmpty(e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public HdfsWorkspace getHdfsWorkspace() throws IOException {
        Workspace workspace = Workspace.remote();
        String sparkHome = sparkVersion.getSparkHome();
        File sparkHomeDir = new File(sparkHome);
        String sparkName =
            Files.isSymbolicLink(sparkHomeDir.toPath())
                ? sparkHomeDir.getCanonicalFile().getName()
                : sparkHomeDir.getName();
        String sparkHdfsHome = workspace.getAppSpark() + "/" + sparkName;
        return new HdfsWorkspace(
            sparkName,
            sparkHome,
            sparkHdfsHome + "/jars",
            sparkHdfsHome + "/plugins",
            workspace.getAppJars());
    }

    private void checkBuildResult() {
        if (buildResult == null) {
            throw new RuntimeException(
                "[spark-submit] current job: " + appName + " was not yet built, buildResult is empty");
        }
        if (!buildResult.pass()) {
            throw new RuntimeException("[spark-submit] current job " + appName + " build failed, please check");
        }
    }

    public Map<String, String> getDefaultSubmitParam() {
        return DEFAULT_SUBMIT_PARAM;
    }
}
