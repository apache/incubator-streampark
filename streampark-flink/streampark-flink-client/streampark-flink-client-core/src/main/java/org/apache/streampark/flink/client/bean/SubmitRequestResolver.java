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
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.Workspace;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.HdfsUtils;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.streampark.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.streampark.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Parses transport-oriented submission fields into an immutable request snapshot.
 *
 * <p>Job configuration is decoded exactly once. The returned snapshot owns all derived
 * values used by configuration assembly and deployment clients, preventing repeated HDFS reads or
 * inconsistent results within one submission.
 */
public final class SubmitRequestResolver {

    private static final int CONFIG_SCHEME_LENGTH = 7;

    private SubmitRequestResolver() {
    }

    /**
     * Resolves a request from one job-configuration read.
     *
     * @param request serialized submission request
     * @return immutable values derived from the request
     */
    public static ResolvedSubmitRequest resolve(SubmitRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Map<String, String> jobConfig =
            request.appConf() == null
                ? Collections.emptyMap()
                : loadJobConfig(request.appConf());
        String jobName = resolveJobName(request, jobConfig);
        AssertUtils.hasText(jobName, "Flink job name must not be blank");
        validateBuildResult(request, jobName);
        return new ResolvedSubmitRequest(
            request,
            jobConfig,
            jobName,
            resolveUserJar(request),
            resolveClassPath(request));
    }

    /** Resolves the explicit job name or the configured pipeline-name fallback. */
    private static String resolveJobName(SubmitRequest request, Map<String, String> jobConfig) {
        if (request.appName() != null) {
            return request.appName();
        }
        String propertyKey = FlinkOptions.PROPERTY_PREFIX.value() + FlinkOptions.PIPELINE_NAME.key();
        return jobConfig.get(propertyKey);
    }

    /** Verifies build state once before any configuration or deployment work begins. */
    private static void validateBuildResult(SubmitRequest request, String jobName) {
        BuildResult buildResult = request.buildResult();
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
                + jobName
                + " was not yet built; build result is empty"
                + target);
        AssertUtils.required(
            buildResult.pass(),
            "[flink-submit] current job " + jobName + " build failed" + target);
    }

    /** Captures the built user JAR for modes that upload a local artifact. */
    private static File resolveUserJar(SubmitRequest request) {
        if (request.deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION) {
            return null;
        }
        ShadedBuildResponse buildResult = request.buildResult().as(ShadedBuildResponse.class);
        String shadedJarPath = buildResult.shadedJarPath();
        AssertUtils.hasText(shadedJarPath, "Built user JAR path must not be blank");
        return new File(shadedJarPath);
    }

    /** Captures the deterministic user-code classpath only for JobGraph deployment modes. */
    private static List<URL> resolveClassPath(SubmitRequest request) {
        if (!usesJobGraph(request.deployMode())) {
            return Collections.emptyList();
        }
        try {
            List<URL> classPath = new ArrayList<>(request.flinkVersion().getFlinkLibs());
            classPath.sort(Comparator.comparing(URL::toExternalForm));
            classPath.addAll(listJobLibraries(request));
            return Collections.unmodifiableList(classPath);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve the Flink submission classpath", e);
        }
    }

    /** Returns whether the deployment builds and submits a JobGraph in this client. */
    private static boolean usesJobGraph(FlinkDeployMode deployMode) {
        return deployMode == FlinkDeployMode.LOCAL
            || deployMode == FlinkDeployMode.REMOTE
            || deployMode == FlinkDeployMode.YARN_SESSION
            || deployMode == FlinkDeployMode.YARN_PER_JOB;
    }

    /** Lists job-specific libraries from the local workspace in deterministic order. */
    private static List<URL> listJobLibraries(SubmitRequest request) {
        File libDir =
            new File(new File(Workspace.LOCAL.workspace, String.valueOf(request.id())), "lib");
        File[] files = libDir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        List<URL> urls = new ArrayList<>(files.length);
        for (File file : files) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid job library path: " + file, e);
            }
        }
        return urls;
    }

    /** Dispatches job configuration parsing from its explicit transport scheme. */
    private static Map<String, String> loadJobConfig(String appConf) {
        if (appConf.length() < CONFIG_SCHEME_LENGTH) {
            throw createInvalidConfigException(appConf);
        }
        String scheme = appConf.substring(0, CONFIG_SCHEME_LENGTH);
        switch (scheme) {
            case "json://":
                return parseJsonConfig(appConf.substring(CONFIG_SCHEME_LENGTH));
            case "yaml://":
                return parseConfigContent(
                    decompressConfig(appConf), ConfigurationFormat.YAML, "inline YAML");
            case "conf://":
                return parseConfigContent(
                    decompressConfig(appConf), ConfigurationFormat.HOCON, "inline HOCON");
            case "prop://":
                return parseConfigContent(
                    decompressConfig(appConf), ConfigurationFormat.PROPERTIES, "inline properties");
            case "hdfs://":
                return parseHdfsConfig(appConf);
            default:
                throw createInvalidConfigException(appConf);
        }
    }

    /** Expands a compressed inline configuration after removing its transport scheme. */
    private static String decompressConfig(String appConf) {
        return DeflaterUtils.unzipString(appConf.trim().substring(CONFIG_SCHEME_LENGTH));
    }

    /** Parses the legacy JSON transport form and discards null entries. */
    private static Map<String, String> parseJsonConfig(String json) {
        try {
            Map<String, String> values =
                new ObjectMapper()
                    .readValue(json, new TypeReference<>() {
                    });
            return values.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON job configuration", e);
        }
    }

    /** Reads an HDFS configuration and selects its parser from the file extension. */
    private static Map<String, String> parseHdfsConfig(String appConf) {
        try {
            String text = HdfsUtils.read(appConf);
            String extension = appConf.substring(appConf.lastIndexOf('.') + 1).toLowerCase();
            switch (extension) {
                case "yml":
                case "yaml":
                    return parseConfigContent(text, ConfigurationFormat.YAML, appConf);
                case "conf":
                    return parseConfigContent(text, ConfigurationFormat.HOCON, appConf);
                case "properties":
                    return parseConfigContent(text, ConfigurationFormat.PROPERTIES, appConf);
                default:
                    throw new IllegalArgumentException(
                        "HDFS job configuration must be YAML, HOCON, or properties");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to read HDFS job configuration: " + appConf, e);
        }
    }

    /** Parses a configuration document through the common immutable configuration pipeline. */
    private static Map<String, String> parseConfigContent(
                                                          String content,
                                                          ConfigurationFormat format,
                                                          String origin) {
        return ConfigurationParser.parse(content, format, origin).toMap();
    }

    /** Creates a consistent failure for unsupported or truncated transport schemes. */
    private static IllegalArgumentException createInvalidConfigException(String appConf) {
        return new IllegalArgumentException(
            "Unsupported job configuration format: " + appConf);
    }
}
