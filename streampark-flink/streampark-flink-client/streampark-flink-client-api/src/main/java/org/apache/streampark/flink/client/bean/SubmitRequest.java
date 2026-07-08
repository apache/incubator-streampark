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
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.HdfsUtils;
import org.apache.streampark.common.util.JsonUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;
import org.apache.streampark.flink.util.FlinkUtils;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class SubmitRequest {

    private FlinkVersion flinkVersion;
    private FlinkDeployMode deployMode;
    private Map<String, Object> properties;
    private String flinkYaml;
    private FlinkJobType jobType;
    private long id;
    private String jobId;
    private String appName;
    private String appConf;
    private ApplicationType applicationType;
    private String savePoint;
    private org.apache.streampark.common.enums.FlinkRestoreMode restoreMode;
    private String args;
    @Nullable
    private String clusterId;
    @Nullable
    private String hadoopUser;
    @Nullable
    private BuildResult buildResult;
    @Nullable
    private Map<String, Object> extraParameter;
    @Nullable
    private String kubernetesNamespace;
    @Nullable
    private FlinkK8sRestExposedType flinkRestExposedType;

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
                         String flinkYaml,
                         FlinkJobType jobType,
                         long id,
                         String jobId,
                         String appName,
                         String appConf,
                         ApplicationType applicationType,
                         String savePoint,
                         org.apache.streampark.common.enums.FlinkRestoreMode restoreMode,
                         String args,
                         String clusterId,
                         String hadoopUser,
                         BuildResult buildResult,
                         Map<String, Object> extraParameter,
                         String kubernetesNamespace,
                         FlinkK8sRestExposedType flinkRestExposedType) {
        this.flinkVersion = flinkVersion;
        this.deployMode = deployMode;
        this.properties = properties;
        this.flinkYaml = flinkYaml;
        this.jobType = jobType;
        this.id = id;
        this.jobId = jobId;
        this.appName = appName;
        this.appConf = appConf;
        this.applicationType = applicationType;
        this.savePoint = savePoint;
        this.restoreMode = restoreMode;
        this.args = args;
        this.clusterId = clusterId;
        this.hadoopUser = hadoopUser;
        this.buildResult = buildResult;
        this.extraParameter = extraParameter;
        this.kubernetesNamespace = kubernetesNamespace;
        this.flinkRestExposedType = flinkRestExposedType;
    }

    public Map<String, String> getAppProperties() {
        if (appProperties == null) {
            appProperties = getParameterMap(ConfigKeys.KEY_FLINK_PROPERTY_PREFIX());
        }
        return appProperties;
    }

    public Map<String, String> getAppOption() {
        if (appOption == null) {
            appOption = getParameterMap(ConfigKeys.KEY_FLINK_OPTION_PREFIX());
        }
        return appOption;
    }

    public String getAppMain() {
        if (appMain == null) {
            if (jobType == FlinkJobType.FLINK_SQL) {
                appMain = Constants.STREAMPARK_FLINKSQL_CLIENT_CLASS;
            } else if (jobType == FlinkJobType.PYFLINK) {
                appMain = Constants.PYTHON_FLINK_DRIVER_CLASS_NAME;
            } else {
                appMain = getAppProperties().get(ConfigKeys.KEY_FLINK_APPLICATION_MAIN_CLASS());
            }
        }
        return appMain;
    }

    public String getEffectiveAppName() {
        if (effectiveAppName == null) {
            effectiveAppName =
                appName == null
                    ? getAppProperties().get(ConfigKeys.KEY_FLINK_APP_NAME())
                    : appName;
        }
        return effectiveAppName;
    }

    public List<URL> getLibs() {
        if (libs == null) {
            String path = Workspace.local().APP_WORKSPACE() + "/" + id + "/lib";
            File libDir = new File(path);
            File[] files = libDir.listFiles();
            if (files == null) {
                libs = Collections.emptyList();
            } else {
                List<URL> urls = new ArrayList<>();
                for (File file : files) {
                    try {
                        urls.add(file.toURI().toURL());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                libs = urls;
            }
        }
        return libs;
    }

    public List<URL> getClassPaths() {
        if (classPaths == null) {
            try {
                classPaths = new ArrayList<>(flinkVersion.flinkLibs());
                classPaths.addAll(getLibs());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return classPaths;
    }

    public String getFlinkSQL() {
        if (flinkSQL == null) {
            Object value = extraParameter.get(ConfigKeys.KEY_FLINK_SQL());
            flinkSQL = value == null ? null : value.toString();
        }
        return flinkSQL;
    }

    public boolean isAllowNonRestoredState() {
        if (allowNonRestoredState == null) {
            Object value = properties.get(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.key());
            if (value == null) {
                allowNonRestoredState = false;
            } else {
                allowNonRestoredState = Boolean.parseBoolean(value.toString());
            }
        }
        return allowNonRestoredState;
    }

    public SavepointRestoreSettings getSavepointRestoreSettings() {
        if (savepointRestoreSettings == null) {
            if (savePoint == null || savePoint.isEmpty()) {
                savepointRestoreSettings = SavepointRestoreSettings.none();
            } else {
                savepointRestoreSettings =
                    SavepointRestoreSettings.forPath(savePoint, isAllowNonRestoredState());
            }
        }
        return savepointRestoreSettings;
    }

    public File getUserJarFile() {
        if (userJarFile == null && deployMode != FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION) {
            checkBuildResult();
            userJarFile = new File(((ShadedBuildResponse) buildResult).shadedJarPath());
        }
        return userJarFile;
    }

    public boolean isSafePackageProgram() {
        if (safePackageProgram == null) {
            String[] parts = flinkVersion.version().split("\\.");
            if (parts.length >= 3) {
                try {
                    int major = Integer.parseInt(parts[0].trim());
                    int minor = Integer.parseInt(parts[1].trim());
                    int patch = Integer.parseInt(parts[2].trim());
                    safePackageProgram = major >= 1 && (minor > 12 || (minor == 12 && patch >= 2));
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

    public boolean hasExtra(String key) {
        return MapUtils.isNotEmpty(extraParameter) && extraParameter.containsKey(key);
    }

    public Object getExtra(String key) {
        return extraParameter.get(key);
    }

    public HdfsWorkspace getHdfsWorkspace() {
        if (hdfsWorkspace == null) {
            Workspace workspace = Workspace.remote();
            String flinkHome = flinkVersion.flinkHome;
            File flinkHomeDir = new File(flinkHome);
            String flinkName;
            try {
                flinkName =
                    FileUtils.isSymlink(flinkHomeDir)
                        ? flinkHomeDir.getCanonicalFile().getName()
                        : flinkHomeDir.getName();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String flinkHdfsHome = workspace.APP_FLINK() + "/" + flinkName;
            hdfsWorkspace =
                HdfsWorkspace.builder()
                    .flinkName(flinkName)
                    .flinkHome(flinkHome)
                    .flinkLib(flinkHdfsHome + "/lib")
                    .flinkPlugins(flinkHdfsHome + "/plugins")
                    .flinkDistJar(FlinkUtils.getFlinkDistJar(flinkHome))
                    .appJars(workspace.APP_JARS())
                    .build();
        }
        return hdfsWorkspace;
    }

    public void checkBuildResult() {
        if (deployMode == FlinkDeployMode.KUBERNETES_NATIVE_SESSION) {
            AssertUtils.required(
                buildResult != null,
                "[flink-submit] current job: "
                    + getEffectiveAppName()
                    + " was not yet built, buildResult is empty"
                    + ",clusterId="
                    + clusterId
                    + ","
                    + "namespace="
                    + kubernetesNamespace);
            AssertUtils.required(
                buildResult.pass(),
                "[flink-submit] current job "
                    + getEffectiveAppName()
                    + " build failed, clusterId"
                    + ",clusterId="
                    + clusterId
                    + ","
                    + "namespace="
                    + kubernetesNamespace);
        } else {
            AssertUtils.required(
                buildResult != null,
                "[flink-submit] current job: "
                    + getEffectiveAppName()
                    + " was not yet built, buildResult is empty");
            AssertUtils.required(
                buildResult.pass(),
                "[flink-submit] current job "
                    + getEffectiveAppName()
                    + " build failed, please check");
        }
    }

    private Map<String, String> getParameterMap(String prefix) {
        if (appConf == null) {
            return Collections.emptyMap();
        }
        String format = appConf.substring(0, Math.min(7, appConf.length()));
        if ("json://".equals(format)) {
            String json = appConf.substring(7);
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> map = JsonUtils.read(json, Map.class);
                return map.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            } catch (Exception e) {
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
                try {
                    String text = HdfsUtils.read(appConf);
                    String extension =
                        appConf.split("\\.")[appConf.split("\\.").length - 1].toLowerCase();
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
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new IllegalArgumentException("[StreamPark] application config format error.");
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey().startsWith(prefix) && StringUtils.isNotEmpty(entry.getValue())) {
                result.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }
        return result;
    }
}
