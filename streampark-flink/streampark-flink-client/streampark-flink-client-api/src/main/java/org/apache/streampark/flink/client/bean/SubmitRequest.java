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

package org.apache.streampark.flink.client.bean;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.FlinkVersion;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.enums.FlinkK8sRestExposedType;
import org.apache.streampark.common.enums.FlinkRestoreMode;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.HdfsUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.streampark.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.streampark.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.collections.MapUtils;
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;

import javax.annotation.Nullable;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubmitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final FlinkVersion flinkVersion;
    private final FlinkDeployMode deployMode;
    private final Map<String, Serializable> properties;
    private final SubmitApplicationSpec application;
    @Nullable
    private final SubmitClusterSpec cluster;
    @Nullable
    private final BuildResult buildResult;
    @Nullable
    private final Map<String, Serializable> extraParameter;

    private transient Map<String, String> appProperties;
    private transient Map<String, String> appOption;
    private transient String appMain;
    private transient String effectiveAppName;
    private transient List<URL> libs;
    private transient List<URL> classPaths;
    private transient String flinkSQL;
    private transient Boolean allowNonRestoredState;
    private transient SavepointRestoreSettings savepointRestoreSettings;
    private transient File userJarFile;
    private transient Boolean safePackageProgram;
    private transient HdfsWorkspace hdfsWorkspace;

    public SubmitRequest(
                         FlinkVersion flinkVersion,
                         FlinkDeployMode deployMode,
                         Map<String, Object> properties,
                         SubmitApplicationSpec application,
                         @Nullable SubmitClusterSpec cluster,
                         @Nullable BuildResult buildResult,
                         @Nullable Map<String, Object> extraParameter) {
        this.flinkVersion = flinkVersion;
        this.deployMode = deployMode;
        this.properties = ClientBeanUtils.toSerializableMap(properties);
        this.application = application;
        this.cluster = cluster;
        this.buildResult = buildResult;
        this.extraParameter = ClientBeanUtils.toSerializableMap(extraParameter);
    }

    public FlinkVersion flinkVersion() {
        return flinkVersion;
    }

    public FlinkDeployMode deployMode() {
        return deployMode;
    }

    public Map<String, Object> properties() {
        return ClientBeanUtils.copyPropertiesMap(properties);
    }

    public String flinkYaml() {
        return application.flinkYaml();
    }

    public FlinkJobType jobType() {
        return application.jobType();
    }

    public long id() {
        return application.id();
    }

    public String jobId() {
        return application.jobId();
    }

    public String appName() {
        return application.appName();
    }

    public String appConf() {
        return application.appConf();
    }

    public ApplicationType applicationType() {
        return application.applicationType();
    }

    public String savePoint() {
        return application.savePoint();
    }

    public FlinkRestoreMode restoreMode() {
        return application.restoreMode();
    }

    public String args() {
        return application.args();
    }

    @Nullable
    public String clusterId() {
        return cluster != null ? cluster.clusterId() : null;
    }

    @Nullable
    public String hadoopUser() {
        return cluster != null ? cluster.hadoopUser() : null;
    }

    @Nullable
    public BuildResult buildResult() {
        return buildResult;
    }

    @Nullable
    public Map<String, Object> extraParameter() {
        return ClientBeanUtils.copyPropertiesMap(extraParameter);
    }

    @Nullable
    public String kubernetesNamespace() {
        return cluster != null ? cluster.kubernetesNamespace() : null;
    }

    @Nullable
    public FlinkK8sRestExposedType flinkRestExposedType() {
        return cluster != null ? cluster.flinkRestExposedType() : null;
    }

    public Map<String, String> appProperties() {
        if (appProperties == null) {
            appProperties = getParameterMap(ConfigKeys.KEY_FLINK_PROPERTY_PREFIX());
        }
        return appProperties;
    }

    public Map<String, String> appOption() {
        if (appOption == null) {
            appOption = getParameterMap(ConfigKeys.KEY_FLINK_OPTION_PREFIX());
        }
        return appOption;
    }

    public String appMain() {
        if (appMain == null) {
            if (jobType() == FlinkJobType.FLINK_SQL) {
                appMain = Constants.STREAMPARK_FLINKSQL_CLIENT_CLASS;
            } else if (jobType() == FlinkJobType.PYFLINK) {
                appMain = Constants.PYTHON_FLINK_DRIVER_CLASS_NAME;
            } else {
                appMain = appProperties().get(ConfigKeys.KEY_FLINK_APPLICATION_MAIN_CLASS());
            }
        }
        return appMain;
    }

    public String effectiveAppName() {
        if (effectiveAppName == null) {
            effectiveAppName =
                appName() == null ? appProperties().get(ConfigKeys.KEY_FLINK_APP_NAME()) : appName();
        }
        return effectiveAppName;
    }

    public List<URL> libs() {
        if (libs == null) {
            File libDir = new File(new File(Workspace.local().APP_WORKSPACE(), String.valueOf(id())), "lib");
            File[] files = libDir.listFiles();
            if (files == null) {
                libs = Collections.emptyList();
            } else {
                List<URL> urls = new ArrayList<>();
                for (File file : files) {
                    try {
                        urls.add(file.toURI().toURL());
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("Invalid lib file URL: " + file, e);
                    }
                }
                libs = urls;
            }
        }
        return libs;
    }

    public List<URL> classPaths() {
        if (classPaths == null) {
            try {
                List<URL> combined = new ArrayList<>(flinkVersion.getFlinkLibs());
                combined.addAll(libs());
                classPaths = combined;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to build classpath", e);
            }
        }
        return classPaths;
    }

    public String flinkSQL() {
        if (flinkSQL == null) {
            if (extraParameter == null) {
                return null;
            }
            Object sql = extraParameter.get(ConfigKeys.KEY_FLINK_SQL());
            flinkSQL = sql != null ? sql.toString() : null;
        }
        return flinkSQL;
    }

    public boolean allowNonRestoredState() {
        if (allowNonRestoredState == null) {
            Object value =
                properties.get(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.key());
            if (value == null) {
                allowNonRestoredState = false;
            } else {
                try {
                    allowNonRestoredState = Boolean.parseBoolean(value.toString());
                } catch (Exception e) {
                    allowNonRestoredState = false;
                }
            }
        }
        return allowNonRestoredState;
    }

    public SavepointRestoreSettings savepointRestoreSettings() {
        if (savepointRestoreSettings == null) {
            if (savePoint() == null || savePoint().isEmpty()) {
                savepointRestoreSettings = SavepointRestoreSettings.none();
            } else {
                savepointRestoreSettings =
                    SavepointRestoreSettings.forPath(savePoint(), allowNonRestoredState());
            }
        }
        return savepointRestoreSettings;
    }

    public File userJarFile() {
        if (userJarFile == null) {
            if (deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION) {
                userJarFile = null;
            } else {
                checkBuildResult();
                ShadedBuildResponse shadedBuildResult = buildResult.as(ShadedBuildResponse.class);
                userJarFile = new File(shadedBuildResult.shadedJarPath());
            }
        }
        return userJarFile;
    }

    public boolean safePackageProgram() {
        if (safePackageProgram == null) {
            String version = flinkVersion.version();
            String[] parts = version.split("\\.");
            if (parts.length >= 3) {
                try {
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    int c = Integer.parseInt(parts[2].trim());
                    safePackageProgram = a >= 1 && (b > 12 || (b == 12 && c >= 2));
                } catch (NumberFormatException e) {
                    safePackageProgram = false;
                }
            } else {
                safePackageProgram = false;
            }
        }
        return safePackageProgram;
    }

    public boolean hasProp(String key) {
        return MapUtils.isNotEmpty(properties) && properties.containsKey(key);
    }

    public Object getProp(String key) {
        return properties.get(key);
    }

    public void putProperty(String key, Serializable value) {
        if (properties != null) {
            properties.put(key, value);
        }
    }

    public boolean hasExtra(String key) {
        return MapUtils.isNotEmpty(extraParameter) && extraParameter.containsKey(key);
    }

    public Object getExtra(String key) {
        return extraParameter != null ? extraParameter.get(key) : null;
    }

    public HdfsWorkspace hdfsWorkspace() {
        if (hdfsWorkspace == null) {
            hdfsWorkspace = ClientBeanUtils.createHdfsWorkspace(flinkVersion);
        }
        return hdfsWorkspace;
    }

    public void checkBuildResult() {
        if (deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_SESSION) {
            AssertUtils.required(
                buildResult != null,
                "[flink-submit] current job: "
                    + effectiveAppName()
                    + " was not yet built, buildResult is empty"
                    + ",clusterId="
                    + clusterId()
                    + ","
                    + ",namespace="
                    + kubernetesNamespace());
            AssertUtils.required(
                buildResult.pass(),
                "[flink-submit] current job "
                    + effectiveAppName()
                    + " build failed, clusterId"
                    + ",clusterId="
                    + clusterId()
                    + ","
                    + ",namespace="
                    + kubernetesNamespace());
        } else {
            AssertUtils.required(
                buildResult != null,
                "[flink-submit] current job: "
                    + effectiveAppName()
                    + " was not yet built, buildResult is empty");
            AssertUtils.required(
                buildResult.pass(),
                "[flink-submit] current job " + effectiveAppName() + " build failed, please check");
        }
    }

    private Map<String, String> getParameterMap(String prefix) {
        if (appConf() == null) {
            return Collections.emptyMap();
        }
        String format = appConf().substring(0, Math.min(appConf().length(), 7));
        Map<String, String> map = loadAppConfMap(format);
        return filterByPrefix(map, prefix);
    }

    private Map<String, String> loadAppConfMap(String format) {
        if ("json://".equals(format)) {
            return parseJsonAppConf();
        }
        String content = DeflaterUtils.unzipString(appConf().trim().substring(7));
        switch (format) {
            case "yaml://":
                return PropertiesUtils.fromYamlTextAsJava(content);
            case "conf://":
                return PropertiesUtils.fromHoconTextAsJava(content);
            case "prop://":
                return PropertiesUtils.fromPropertiesTextAsJava(content);
            case "hdfs://":
                return parseHdfsAppConf();
            default:
                throw new IllegalArgumentException("[StreamPark] application config format error.");
        }
    }

    private Map<String, String> parseJsonAppConf() {
        String json = appConf().substring(7);
        try {
            Map<String, String> map =
                new ObjectMapper()
                    .readValue(json, new TypeReference<Map<String, String>>() {
                    });
            return map.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON app config", e);
        }
    }

    private Map<String, String> parseHdfsAppConf() {
        try {
            String text = HdfsUtils.read(appConf());
            String[] parts = appConf().split("\\.");
            String extension = parts[parts.length - 1].toLowerCase();
            switch (extension) {
                case "yml":
                case "yaml":
                    return PropertiesUtils.fromYamlTextAsJava(text);
                case "conf":
                    return PropertiesUtils.fromHoconTextAsJava(text);
                case "properties":
                    return PropertiesUtils.fromPropertiesTextAsJava(text);
                default:
                    throw new IllegalArgumentException(
                        "[StreamPark] Usage: application config format error,must be [yaml|conf|properties]");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse HDFS app config: " + appConf(), e);
        }
    }

    private static Map<String, String> filterByPrefix(Map<String, String> map, String prefix) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith(prefix) && value != null && !value.isEmpty()) {
                result.put(key.substring(prefix.length()), value);
            }
        }
        return result;
    }
}
