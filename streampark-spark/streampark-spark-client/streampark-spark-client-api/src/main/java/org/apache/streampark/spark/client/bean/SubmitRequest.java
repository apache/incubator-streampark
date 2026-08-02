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
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.streampark.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.streampark.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.collections.MapUtils;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Request to submit a Spark application. */
public class SubmitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Map<String, String> DEFAULT_SUBMIT_PARAM;

    static {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("spark.driver.cores", "1");
        defaults.put("spark.driver.memory", "1g");
        defaults.put("spark.executor.cores", "1");
        defaults.put("spark.executor.memory", "1g");
        defaults.put("spark.executor.instances", "2");
        DEFAULT_SUBMIT_PARAM = Collections.unmodifiableMap(defaults);
    }

    private final SparkVersion sparkVersion;
    private final SparkDeployMode deployMode;
    private final String sparkYaml;
    private final SparkJobType jobType;
    private final long id;
    private final String appName;
    private final String mainClass;
    private final String appConf;
    private final Map<String, String> appProperties;
    private final List<String> appArgs;
    private final ApplicationType applicationType;
    @Nullable
    private final String hadoopUser;
    @Nullable
    private final BuildResult buildResult;
    @Nullable
    private final Map<String, Object> extraParameter;

    private transient Map<String, String> sparkParameterMap;
    private transient String appMain;
    private transient HdfsWorkspace hdfsWorkspace;

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
                         @Nullable String hadoopUser,
                         @Nullable BuildResult buildResult,
                         @Nullable Map<String, Object> extraParameter) {
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

    public SparkVersion sparkVersion() {
        return sparkVersion;
    }

    public SparkVersion getSparkVersion() {
        return sparkVersion;
    }

    public SparkDeployMode deployMode() {
        return deployMode;
    }

    public SparkDeployMode getDeployMode() {
        return deployMode;
    }

    public String sparkYaml() {
        return sparkYaml;
    }

    public String getSparkYaml() {
        return sparkYaml;
    }

    public SparkJobType jobType() {
        return jobType;
    }

    public SparkJobType getJobType() {
        return jobType;
    }

    public long id() {
        return id;
    }

    public long getId() {
        return id;
    }

    public String appName() {
        return appName;
    }

    public String getAppName() {
        return appName;
    }

    public String mainClass() {
        return mainClass;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String appConf() {
        return appConf;
    }

    public String getAppConf() {
        return appConf;
    }

    public Map<String, String> appProperties() {
        return appProperties;
    }

    public Map<String, String> getAppProperties() {
        return appProperties;
    }

    public List<String> appArgs() {
        return appArgs;
    }

    public List<String> getAppArgs() {
        return appArgs;
    }

    public ApplicationType applicationType() {
        return applicationType;
    }

    public ApplicationType getApplicationType() {
        return applicationType;
    }

    @Nullable
    public String hadoopUser() {
        return hadoopUser;
    }

    @Nullable
    public String getHadoopUser() {
        return hadoopUser;
    }

    @Nullable
    public BuildResult buildResult() {
        return buildResult;
    }

    @Nullable
    public BuildResult getBuildResult() {
        return buildResult;
    }

    @Nullable
    public Map<String, Object> extraParameter() {
        return extraParameter;
    }

    @Nullable
    public Map<String, Object> getExtraParameter() {
        return extraParameter;
    }

    public Map<String, String> sparkParameterMap() {
        if (sparkParameterMap == null) {
            sparkParameterMap = getParameterMap(ConfigKeys.KEY_SPARK_PROPERTY_PREFIX());
        }
        return sparkParameterMap;
    }

    public String appMain() {
        if (appMain == null) {
            switch (jobType) {
                case SPARK_SQL:
                    appMain = Constants.STREAMPARK_SPARKSQL_CLIENT_CLASS;
                    break;
                case SPARK_JAR:
                case PYSPARK:
                    appMain = mainClass;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown deployment Mode");
            }
        }
        return appMain;
    }

    public String userJarPath() {
        checkBuildResult();
        return ((ShadedBuildResponse) buildResult).shadedJarPath();
    }

    public boolean hasExtra(String key) {
        return MapUtils.isNotEmpty(extraParameter) && extraParameter.containsKey(key);
    }

    public Object getExtra(String key) {
        return extraParameter.get(key);
    }

    public HdfsWorkspace hdfsWorkspace() {
        if (hdfsWorkspace == null) {
            Workspace workspace = Workspace.remote;
            String sparkHome = sparkVersion.getSparkHome();
            File sparkHomeDir = new File(sparkHome);
            String sparkName;
            try {
                sparkName = isSymlink(sparkHomeDir)
                    ? sparkHomeDir.getCanonicalFile().getName()
                    : sparkHomeDir.getName();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String sparkHdfsHome = workspace.APP_SPARK() + "/" + sparkName;
            hdfsWorkspace = new HdfsWorkspace(
                sparkName,
                sparkHome,
                sparkHdfsHome + "/jars",
                sparkHdfsHome + "/plugins",
                workspace.APP_JARS());
        }
        return hdfsWorkspace;
    }

    private Map<String, String> getParameterMap(String prefix) {
        if (appConf == null) {
            return Collections.emptyMap();
        }
        String format = appConf.substring(0, Math.min(appConf.length(), 7));
        if ("json://".equals(format)) {
            String json = appConf.substring(7);
            try {
                Map<String, String> map =
                    new ObjectMapper().readValue(json, new TypeReference<Map<String, String>>() {
                    });
                return map.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String content = DeflaterUtils.unzipString(appConf.trim().substring(7));
        Map<String, String> map;
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
                String text;
                try {
                    text = HdfsUtils.read(appConf);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String extension = appConf.split("\\.")[appConf.split("\\.").length - 1].toLowerCase();
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
                            "[StreamPark] Usage: application config format error,"
                                + "must be [yaml|conf|properties]");
                }
                break;
            default:
                throw new IllegalArgumentException("[StreamPark] application config format error.");
        }
        return map.entrySet().stream()
            .filter(e -> e.getKey().startsWith(prefix))
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean isSymlink(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        return Files.isSymbolicLink(file.toPath());
    }

    private void checkBuildResult() {
        if (buildResult == null) {
            throw new RuntimeException(
                "[spark-submit] current job: " + appName + " was not yet built, buildResult is empty");
        }
        if (!buildResult.pass()) {
            throw new RuntimeException(
                "[spark-submit] current job " + appName + " build failed, please check");
        }
    }
}
