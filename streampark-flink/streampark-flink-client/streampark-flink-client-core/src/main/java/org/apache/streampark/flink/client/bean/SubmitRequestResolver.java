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

import org.apache.streampark.common.configuration.ConfigurationFormat;
import org.apache.streampark.common.configuration.ConfigurationParser;
import org.apache.streampark.common.configuration.Constants;
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.Workspace;
import org.apache.streampark.common.configuration.option.ApplicationOptions;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.HdfsUtils;
import org.apache.streampark.flink.client.configuration.FlinkSavepointOptions;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.streampark.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.streampark.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;

import javax.annotation.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Resolves runtime submission values from the serialized client request. */
public final class SubmitRequestResolver {

    private static final int CONFIG_SCHEME_LENGTH = 7;

    private SubmitRequestResolver() {
    }

    /** Returns application properties without their transport prefix. */
    public static Map<String, String> applicationProperties(SubmitRequest request) {
        return getParameters(request, FlinkOptions.PROPERTY_PREFIX.value());
    }

    /** Returns application options without their transport prefix. */
    public static Map<String, String> applicationOptions(SubmitRequest request) {
        return getParameters(request, FlinkOptions.OPTION_PREFIX.value());
    }

    /** Resolves the application entry class for the submitted job type. */
    @Nullable
    public static String applicationMain(SubmitRequest request) {
        if (request.jobType() == FlinkJobType.FLINK_SQL) {
            return Constants.STREAMPARK_FLINKSQL_CLIENT_CLASS;
        }
        if (request.jobType() == FlinkJobType.PYFLINK) {
            return Constants.PYTHON_FLINK_DRIVER_CLASS_NAME;
        }
        String mainClass =
            applicationProperties(request).get(FlinkOptions.APPLICATION_MAIN_CLASS.key());
        if (mainClass == null && request.appConf() != null) {
            mainClass =
                loadApplicationConfig(request.appConf())
                    .get(FlinkOptions.APPLICATION_MAIN_CLASS.key());
        }
        return mainClass;
    }

    /** Resolves the explicit or configured application name. */
    @Nullable
    public static String effectiveApplicationName(SubmitRequest request) {
        return request.appName() == null
            ? applicationProperties(request).get(FlinkOptions.PIPELINE_NAME.key())
            : request.appName();
    }

    /** Returns the application-specific libraries available in the local workspace. */
    public static List<URL> libraries(SubmitRequest request) {
        File libDir =
            new File(
                new File(Workspace.LOCAL.workspace, String.valueOf(request.id())), "lib");
        File[] files = libDir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<URL> urls = new ArrayList<>(files.length);
        for (File file : files) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid library path: " + file, e);
            }
        }
        return urls;
    }

    /** Builds the complete user-code classpath for submission. */
    public static List<URL> classPaths(SubmitRequest request) {
        try {
            List<URL> classPaths = new ArrayList<>(request.flinkVersion().getFlinkLibs());
            classPaths.addAll(libraries(request));
            return classPaths;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve the Flink submission classpath", e);
        }
    }

    /** Returns the SQL payload carried by a Flink SQL request. */
    @Nullable
    public static String flinkSql(SubmitRequest request) {
        Map<String, Object> extraParameter = request.extraParameter();
        Object sql = extraParameter.get(ApplicationOptions.SQL.key());
        return sql == null ? null : sql.toString();
    }

    /** Returns whether state not represented in a savepoint may be skipped. */
    public static boolean allowNonRestoredState(SubmitRequest request) {
        Object value =
            request.properties().get(FlinkSavepointOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.key());
        return value != null && Boolean.parseBoolean(value.toString());
    }

    /** Creates Flink restore settings from the request savepoint options. */
    public static SavepointRestoreSettings savepointRestoreSettings(SubmitRequest request) {
        if (request.savePoint() == null || request.savePoint().isEmpty()) {
            return SavepointRestoreSettings.none();
        }
        return SavepointRestoreSettings.forPath(
            request.savePoint(), allowNonRestoredState(request));
    }

    /** Resolves the built user JAR for deployment modes that upload a local artifact. */
    @Nullable
    public static File userJarFile(SubmitRequest request) {
        if (request.deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION) {
            return null;
        }
        validateBuildResult(request);
        ShadedBuildResponse buildResult = request.buildResult().as(ShadedBuildResponse.class);
        String shadedJarPath = buildResult.shadedJarPath();
        return shadedJarPath == null ? null : new File(shadedJarPath);
    }

    /** Returns whether the target Flink version safely supports closing packaged programs. */
    public static boolean canSafelyClosePackagedProgram(SubmitRequest request) {
        String[] parts = request.flinkVersion().version().split("\\.");
        if (parts.length < 3) {
            return false;
        }
        try {
            int major = Integer.parseInt(parts[0].trim());
            int minor = Integer.parseInt(parts[1].trim());
            int patch = Integer.parseInt(parts[2].trim());
            return major >= 1 && (minor > 12 || (minor == 12 && patch >= 2));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** Verifies that the application artifact was built successfully before submission. */
    public static void validateBuildResult(SubmitRequest request) {
        BuildResult buildResult = request.buildResult();
        String applicationName = effectiveApplicationName(request);
        String target =
            FlinkDeployMode.isKubernetesMode(request.deployMode())
                ? ", clusterId="
                    + request.clusterId()
                    + ", namespace="
                    + request.kubernetesNamespace()
                : "";
        AssertUtils.required(
            buildResult != null,
            "[flink-submit] current job "
                + applicationName
                + " was not yet built; build result is empty"
                + target);
        AssertUtils.required(
            buildResult.pass(),
            "[flink-submit] current job " + applicationName + " build failed" + target);
    }

    private static Map<String, String> getParameters(SubmitRequest request, String prefix) {
        if (request.appConf() == null) {
            return Collections.emptyMap();
        }
        return filterByPrefix(loadApplicationConfig(request.appConf()), prefix);
    }

    private static Map<String, String> loadApplicationConfig(String appConf) {
        if (appConf.length() < CONFIG_SCHEME_LENGTH) {
            throw invalidConfigFormat(appConf);
        }
        String scheme = appConf.substring(0, CONFIG_SCHEME_LENGTH);
        switch (scheme) {
            case "json://":
                return parseJson(appConf.substring(CONFIG_SCHEME_LENGTH));
            case "yaml://":
                return parseConfig(decompress(appConf), ConfigurationFormat.YAML, "inline YAML");
            case "conf://":
                return parseConfig(decompress(appConf), ConfigurationFormat.HOCON, "inline HOCON");
            case "prop://":
                return parseConfig(
                    decompress(appConf), ConfigurationFormat.PROPERTIES, "inline properties");
            case "hdfs://":
                return parseHdfs(appConf);
            default:
                throw invalidConfigFormat(appConf);
        }
    }

    private static String decompress(String appConf) {
        return DeflaterUtils.unzipString(appConf.trim().substring(CONFIG_SCHEME_LENGTH));
    }

    private static Map<String, String> parseJson(String json) {
        try {
            Map<String, String> values =
                new ObjectMapper()
                    .readValue(json, new TypeReference<>() {
                    });
            return values.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON application configuration", e);
        }
    }

    private static Map<String, String> parseHdfs(String appConf) {
        try {
            String text = HdfsUtils.read(appConf);
            String extension = appConf.substring(appConf.lastIndexOf('.') + 1).toLowerCase();
            switch (extension) {
                case "yml":
                case "yaml":
                    return parseConfig(text, ConfigurationFormat.YAML, appConf);
                case "conf":
                    return parseConfig(text, ConfigurationFormat.HOCON, appConf);
                case "properties":
                    return parseConfig(text, ConfigurationFormat.PROPERTIES, appConf);
                default:
                    throw new IllegalArgumentException(
                        "HDFS application configuration must be YAML, HOCON, or properties");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to read HDFS application configuration: " + appConf, e);
        }
    }

    private static Map<String, String> parseConfig(
                                                   String content,
                                                   ConfigurationFormat format,
                                                   String origin) {
        return ConfigurationParser.parse(content, format, origin).toMap();
    }

    private static Map<String, String> filterByPrefix(
                                                      Map<String, String> values, String prefix) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getValue();
            if (entry.getKey().startsWith(prefix) && value != null && !value.isEmpty()) {
                result.put(entry.getKey().substring(prefix.length()), value);
            }
        }
        return result;
    }

    private static IllegalArgumentException invalidConfigFormat(String appConf) {
        return new IllegalArgumentException(
            "Unsupported application configuration format: " + appConf);
    }
}
