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

import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.flink.client.request.SubmitRequest;

import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.PackagedProgramUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a {@link JobGraph} with the classloader of the registered Flink version.
 *
 * <p>The builder temporarily installs the target-version parent classloader, constructs the
 * packaged program, and restores the original thread context classloader in every outcome.
 * Partially created resources are closed without replacing the original construction failure.
 */
public final class FlinkJobGraphBuilder {

    private FlinkJobGraphBuilder() {
    }

    /** Creates the packaged program and its executable job graph. */
    public static Result build(
                               Configuration configuration,
                               ResolvedSubmitRequest resolved,
                               File jarFile) throws Exception {
        SubmitRequest request = resolved.request();
        PackagedProgram.Builder builder =
            PackagedProgram.newBuilder()
                .setSavepointRestoreSettings(resolved.savepointRestoreSettings())
                .setEntryPointClassName(
                    configuration
                        .getOptional(ApplicationConfiguration.APPLICATION_MAIN_CLASS)
                        .orElseThrow(
                            () -> new IllegalStateException(
                                "Job main class is not configured")))
                .setArguments(
                    configuration
                        .getOptional(ApplicationConfiguration.APPLICATION_ARGS)
                        .orElse(Collections.emptyList())
                        .toArray(new String[0]));

        if (request.jobType() != FlinkJobType.PYFLINK) {
            builder.setJarFile(jarFile);
            boolean flinkSqlJob = request.jobType() == FlinkJobType.FLINK_SQL;
            builder.setUserClassPaths(userClassPaths(resolved, !flinkSqlJob));
            builder.setConfiguration(parentFirstConfiguration());
        }

        ClassLoader shimsClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader programParent =
            request.jobType() == FlinkJobType.PYFLINK
                ? shimsClassLoader
                : programParentClassLoader(shimsClassLoader);

        Thread.currentThread().setContextClassLoader(programParent);

        try {
            PackagedProgram program = builder.build();
            try {
                Configuration jobGraphConfiguration = new Configuration(configuration);
                if (request.jobType() != FlinkJobType.PYFLINK) {
                    jobGraphConfiguration.addAll(parentFirstConfiguration());
                }
                JobGraph jobGraph =
                    PackagedProgramUtils.createJobGraph(
                        program,
                        jobGraphConfiguration,
                        configuration.get(
                            CoreOptions.DEFAULT_PARALLELISM,
                            CoreOptions.DEFAULT_PARALLELISM.defaultValue()),
                        null,
                        false);
                return new Result(program, jobGraph);
            } catch (Exception failure) {
                closeAfterBuildFailure(program, failure);
                throw failure;
            }
        } finally {
            Thread.currentThread().setContextClassLoader(shimsClassLoader);
        }
    }

    /** Closes a partially built program without hiding the original construction failure. */
    private static void closeAfterBuildFailure(
                                               PackagedProgram program, Exception failure) {
        try {
            program.close();
        } catch (Exception closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    /** Declares packages that must retain one identity across the user-code classloader boundary. */
    private static Configuration parentFirstConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setString(
            "classloader.parent-first-patterns.additional",
            "org.apache.streampark.;org.apache.flink.;org.yaml.");
        return configuration;
    }

    /**
     * Creates the parent used by Flink's user-code classloader.
     *
     * <p>StreamPark and Flink classes come from the version-isolated shims classloader. SnakeYAML
     * comes from the console classloader to avoid conflicts with SQL job fat jars.
     */
    private static ClassLoader programParentClassLoader(ClassLoader shimsClassLoader) {
        ClassLoader consoleClassLoader = FlinkJobGraphBuilder.class.getClassLoader();
        return new ClassLoader(shimsClassLoader) {

            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        if (name.startsWith("org.apache.streampark.")
                            || name.startsWith("org.apache.flink.")) {
                            loaded = getParent().loadClass(name);
                        } else if (name.startsWith("org.yaml.")) {
                            loaded = consoleClassLoader.loadClass(name);
                        }
                    }
                    if (loaded == null) {
                        loaded = super.loadClass(name, resolve);
                    } else if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
        };
    }

    /**
     * Resolves the classpath used while building the JobGraph.
     *
     * <p>Thin uploaded jars need {@code flink-dist} on Flink 1.20 and later. SQL fat jars omit it
     * and use parent-first delegation to prevent dependency conflicts.
     */
    private static List<URL> userClassPaths(
                                            ResolvedSubmitRequest resolved,
                                            boolean includeFlinkDist) {
        List<URL> classPaths = new ArrayList<>(resolved.getSubmissionClassPath());
        classPaths.removeIf(
            url -> {
                String path = url.getPath();
                return path.contains("flink-shaded-force-shading")
                    || (!includeFlinkDist && path.contains("flink-dist"));
            });
        return classPaths;
    }

    /** Result containing resources created while building a JobGraph. */
    public static final class Result {

        private final PackagedProgram program;
        private final JobGraph jobGraph;

        private Result(PackagedProgram program, JobGraph jobGraph) {
            this.program = program;
            this.jobGraph = jobGraph;
        }

        public PackagedProgram program() {
            return program;
        }

        public JobGraph jobGraph() {
            return jobGraph;
        }
    }
}
