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

package org.apache.streampark.console.base.config;

import org.apache.streampark.common.configuration.ConfigOption;
import org.apache.streampark.common.configuration.ConfigSource;
import org.apache.streampark.common.configuration.Configuration;
import org.apache.streampark.common.configuration.GlobalConfiguration;
import org.apache.streampark.common.configuration.Workspace;
import org.apache.streampark.common.configuration.option.CoreOptions;
import org.apache.streampark.common.configuration.option.DockerOptions;
import org.apache.streampark.common.configuration.option.HadoopOptions;
import org.apache.streampark.common.configuration.option.MavenOptions;
import org.apache.streampark.common.configuration.option.WorkspaceOptions;
import org.apache.streampark.common.configuration.option.YarnOptions;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.flink.kubernetes.configuration.KubernetesOptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bridges Spring's resolved environment into StreamPark's typed server configuration.
 *
 * <p>Option declarations remain with the modules that consume them. This Console-side composition
 * root owns only the list of options whose values may be overridden by Spring property sources,
 * such as command-line arguments, environment variables, and profile-specific configuration.
 * Keeping that list here prevents framework integration concerns from leaking into common or
 * engine-specific option catalogs.
 */
@Slf4j
@Component
public final class SpringConfigurationInitializer {

    private static final String SPRING_ENVIRONMENT_ORIGIN = "resolved Spring environment";

    /**
     * Options read by server components after the Spring context has completed property resolution.
     *
     * <p>This is intentionally an explicit integration boundary. Adding an option to its owning
     * catalog does not implicitly expose it to Spring or create a global registration side effect.
     */
    private static final List<ConfigOption<?>> SPRING_BOUND_OPTIONS = List.of(
        CoreOptions.APP_HOME,
        WorkspaceOptions.LOCAL_ROOT,
        WorkspaceOptions.REMOTE_ROOT,
        HadoopOptions.USER_NAME,
        HadoopOptions.KERBEROS_DEBUG,
        HadoopOptions.KERBEROS_ENABLED,
        HadoopOptions.KERBEROS_PRINCIPAL,
        HadoopOptions.KERBEROS_KEYTAB,
        HadoopOptions.KERBEROS_KRB5,
        HadoopOptions.KERBEROS_TICKET_LIFETIME,
        YarnOptions.PROXY_URL,
        YarnOptions.HTTP_AUTHENTICATION,
        DockerOptions.HOST,
        DockerOptions.MAX_CONNECTIONS,
        DockerOptions.CONNECTION_TIMEOUT,
        DockerOptions.RESPONSE_TIMEOUT,
        MavenOptions.SETTINGS_PATH,
        MavenOptions.REPOSITORY_URL,
        MavenOptions.USER_NAME,
        MavenOptions.PASSWORD,
        ConsoleOptions.BUILD_LOG_READ_MAX_SIZE,
        ConsoleOptions.DATABASE_DIALECT,
        KubernetesOptions.JOB_STATUS_REQUEST_TIMEOUT,
        KubernetesOptions.JOB_STATUS_CACHE_TIMEOUT,
        KubernetesOptions.METRIC_REQUEST_TIMEOUT,
        KubernetesOptions.JOB_STATUS_POLL_INTERVAL,
        KubernetesOptions.METRIC_POLL_INTERVAL,
        KubernetesOptions.SILENT_STATE_TRACKING_RETENTION,
        KubernetesOptions.INGRESS_CLASS);

    /**
     * Publishes Spring-resolved values as one validated runtime configuration layer.
     *
     * <p>Every supplied option is type-checked before the snapshot becomes globally visible.
     * Workspace verification then detects any component that accessed constant workspace paths
     * before Spring resolution completed.
     *
     * @param environment fully prepared Spring environment
     * @return the installed immutable configuration snapshot
     */
    public Configuration initialize(Environment environment) {
        Objects.requireNonNull(environment, "environment must not be null");
        Map<String, Object> resolvedValues = collect(environment);
        Configuration springOverrides = Configuration.builder()
            .add(SPRING_ENVIRONMENT_ORIGIN, ConfigSource.RUNTIME, resolvedValues)
            .build();
        Configuration candidate = Configuration.builder(GlobalConfiguration.current())
            .add(springOverrides)
            .build();

        // Conversion is normally lazy. The process bootstrap boundary validates every supplied
        // Spring override eagerly so invalid server settings fail before background services start.
        SPRING_BOUND_OPTIONS.stream()
            .filter(option -> resolvedValues.containsKey(option.key()))
            .forEach(candidate::get);

        GlobalConfiguration.update(candidate);
        initializeHadoopUser(environment, candidate);
        Workspace.verifyInitializedFrom(candidate);
        log.info(
            "Applied {} Spring-resolved StreamPark configuration overrides: {}",
            springOverrides.size(),
            springOverrides.toRedactedMap());
        return candidate;
    }

    private static void initializeHadoopUser(
                                             Environment environment,
                                             Configuration configuration) {
        // Hadoop reads this legacy JVM property during its own static bootstrap. Set it before
        // Workspace resolves the remote filesystem so the configured identity is effective.
        String userName = environment.getProperty(
            HadoopOptions.HADOOP_USER_NAME_PROPERTY,
            configuration.get(HadoopOptions.USER_NAME));
        SystemPropertyUtils.set(HadoopOptions.HADOOP_USER_NAME_PROPERTY, userName);
        log.info(
            "Initialized Hadoop system property: {}={}",
            HadoopOptions.HADOOP_USER_NAME_PROPERTY,
            userName);
    }

    private static Map<String, Object> collect(Environment environment) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (ConfigOption<?> option : SPRING_BOUND_OPTIONS) {
            if (environment.containsProperty(option.key())) {
                String value = environment.getProperty(option.key());
                if (value != null) {
                    resolved.put(option.key(), value);
                }
            }
        }
        return resolved;
    }
}
