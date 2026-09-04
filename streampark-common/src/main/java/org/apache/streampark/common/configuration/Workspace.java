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

package org.apache.streampark.common.configuration;

import org.apache.streampark.common.configuration.option.WorkspaceOptions;
import org.apache.streampark.common.enums.StorageType;
import org.apache.streampark.common.util.HdfsUtils;

import java.net.URI;
import java.util.Objects;

/**
 * Immutable path layout for StreamPark's local and remote workspaces.
 *
 * <p>{@link #LOCAL} and {@link #REMOTE} provide constant-style access for normal application code.
 * Both instances are derived from the same global configuration snapshot when this class is first
 * initialized. Configuration bootstrap code must therefore call {@link
 * #verifyInitializedFrom(Configuration)} after all configuration sources have been composed.
 * Operations that intentionally use another immutable snapshot can create an independent layout
 * through {@link #of(StorageType, Configuration)}.
 */
public final class Workspace {

    private static final Configuration INITIAL_CONFIGURATION = GlobalConfiguration.current();

    /** Local workspace layout derived from the process configuration. */
    public static final Workspace LOCAL = new Workspace(StorageType.LFS, INITIAL_CONFIGURATION);

    /** Remote workspace layout derived from the process configuration. */
    public static final Workspace REMOTE = new Workspace(StorageType.HDFS, INITIAL_CONFIGURATION);

    /** Local directory containing assembled application distributions. */
    public static final String APP_LOCAL_DIST = child(LOCAL.root, "dist");

    /** Local Maven repository used by the built-in compilation pipeline. */
    public static final String MAVEN_LOCAL_PATH = child(LOCAL.root, "mvnrepo");

    /** Local directory containing checked-out project sources. */
    public static final String PROJECT_LOCAL_PATH = child(LOCAL.root, "project");

    /** Local directory containing StreamPark logs. */
    public static final String LOG_LOCAL_PATH = child(LOCAL.root, "logs");

    /** Local directory containing project build logs. */
    public static final String PROJECT_BUILD_LOG_PATH = child(LOG_LOCAL_PATH, "build_logs");

    /** Remote directory consumed by the Flink history server. */
    public static final String ARCHIVES_FILE_PATH = child(REMOTE.root, "historyserver/archive");

    /** Configured root from which every path in this layout is derived. */
    public final String root;

    /** Directory containing StreamPark plugins. */
    public final String plugins;

    /** Directory containing framework client artifacts. */
    public final String client;

    /** Directory containing version-specific Flink shim artifacts. */
    public final String shims;

    /** Directory containing user-uploaded artifacts. */
    public final String uploads;

    /** Directory containing Python runtime artifacts. */
    public final String python;

    /** Python virtual-environment archive distributed to PyFlink jobs. */
    public final String pythonVenv;

    /** Directory containing isolated per-application workspaces. */
    public final String workspace;

    /** Directory containing managed Flink distributions. */
    public final String flink;

    /** Directory containing managed Spark distributions. */
    public final String spark;

    /** Directory containing application backups. */
    public final String backups;

    /** Default directory containing Flink savepoints. */
    public final String savepoints;

    /** Directory containing StreamPark application JARs. */
    public final String jars;

    private Workspace(StorageType storageType, Configuration configuration) {
        Objects.requireNonNull(storageType, "storageType must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");
        root = resolveRoot(storageType, configuration);
        plugins = child(root, "plugins");
        client = child(root, "client");
        shims = child(root, "shims");
        uploads = child(root, "uploads");
        python = child(root, "python");
        pythonVenv = child(python, "venv.zip");
        workspace = child(root, "workspace");
        flink = child(root, "flink");
        spark = child(root, "spark");
        backups = child(root, "backups");
        savepoints = child(root, "savepoints");
        jars = child(root, "jars");
    }

    /**
     * Creates an independent workspace layout from a stable configuration snapshot.
     *
     * @param storageType local or remote storage type
     * @param configuration stable configuration snapshot
     * @return immutable workspace layout
     */
    public static Workspace of(StorageType storageType, Configuration configuration) {
        return new Workspace(storageType, configuration);
    }

    /**
     * Verifies that the constant layouts were initialized from the completed bootstrap snapshot.
     *
     * <p>The check converts accidental early class initialization into an actionable startup
     * failure instead of allowing the process to operate on stale workspace paths.
     *
     * @param configuration completed bootstrap configuration
     * @throws ConfigException if either constant was initialized from another workspace root
     */
    public static void verifyInitializedFrom(Configuration configuration) {
        Workspace expectedLocal = of(StorageType.LFS, configuration);
        Workspace expectedRemote = of(StorageType.HDFS, configuration);
        if (!LOCAL.root.equals(expectedLocal.root) || !REMOTE.root.equals(expectedRemote.root)) {
            throw new ConfigException(
                "Workspace constants were initialized before configuration bootstrap completed");
        }
    }

    private static String resolveRoot(StorageType storageType, Configuration configuration) {
        if (storageType == StorageType.LFS) {
            return normalizeRoot(configuration.get(WorkspaceOptions.LOCAL_ROOT));
        }

        String configured = normalizeRoot(configuration.get(WorkspaceOptions.REMOTE_ROOT));
        URI configuredUri = parseUri(configured, WorkspaceOptions.REMOTE_ROOT.key());
        rejectUnsupportedUriComponents(configuredUri, WorkspaceOptions.REMOTE_ROOT.key());
        if (configuredUri.getScheme() == null) {
            return qualifyWithDefaultFileSystem(configured);
        }
        if (!"hdfs".equalsIgnoreCase(configuredUri.getScheme())) {
            throw new ConfigException(
                "Option '"
                    + WorkspaceOptions.REMOTE_ROOT.key()
                    + "' must use the hdfs scheme: "
                    + configured);
        }
        return validateAndQualifyHdfsUri(configured, configuredUri);
    }

    private static String validateAndQualifyHdfsUri(String configured, URI configuredUri) {
        String defaultFileSystem = defaultFileSystem();
        URI defaultUri = parseUri(defaultFileSystem, "fs.defaultFS");
        if (!"hdfs".equalsIgnoreCase(defaultUri.getScheme())) {
            throw new ConfigException(
                "Remote workspace uses HDFS but fs.defaultFS is '" + defaultFileSystem + "'");
        }

        String configuredAuthority = configuredUri.getAuthority();
        if (configuredAuthority == null) {
            return qualify(defaultFileSystem, configuredUri.getPath());
        }
        String defaultAuthority = defaultUri.getAuthority();
        if (defaultAuthority == null || !configuredAuthority.equalsIgnoreCase(defaultAuthority)) {
            throw new ConfigException(
                "Remote workspace authority '"
                    + configuredAuthority
                    + "' does not match fs.defaultFS '"
                    + defaultFileSystem
                    + "'");
        }
        return configured;
    }

    private static String qualifyWithDefaultFileSystem(String configured) {
        return qualify(defaultFileSystem(), configured);
    }

    private static String qualify(String defaultFileSystem, String path) {
        String relativePath = path;
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return relativePath.isEmpty() ? defaultFileSystem : child(defaultFileSystem, relativePath);
    }

    private static String defaultFileSystem() {
        String value = HdfsUtils.getDefaultFS();
        if (value == null || value.trim().isEmpty()) {
            throw new ConfigException("Hadoop property 'fs.defaultFS' must not be blank");
        }
        String normalized = normalizeRoot(value);
        URI uri = parseUri(normalized, "fs.defaultFS");
        if (uri.getScheme() == null) {
            throw new ConfigException("Hadoop property 'fs.defaultFS' must contain a URI scheme");
        }
        return normalized;
    }

    private static void rejectUnsupportedUriComponents(URI uri, String key) {
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new ConfigException("Option '" + key + "' must not contain a query or fragment");
        }
    }

    private static URI parseUri(String value, String key) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("Option '" + key + "' is not a valid URI: " + value, exception);
        }
    }

    private static String child(String parent, String name) {
        return parent.endsWith("/") ? parent + name : parent + "/" + name;
    }

    private static String normalizeRoot(String value) {
        String normalized = value.trim();
        if (isSchemeRoot(normalized)) {
            return normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean isSchemeRoot(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            return false;
        }
        for (int index = separator + 1; index < value.length(); index++) {
            if (value.charAt(index) != '/') {
                return false;
            }
        }
        return true;
    }
}
