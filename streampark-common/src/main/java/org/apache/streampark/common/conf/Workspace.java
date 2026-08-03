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

package org.apache.streampark.common.conf;

import org.apache.streampark.common.enums.StorageType;
import org.apache.streampark.common.util.HdfsUtils;
import org.apache.streampark.common.util.StringCastUtils;
import org.apache.streampark.common.util.SystemPropertyUtils;

import java.net.URI;

/** Workspace path configuration for local and remote storage. */
public final class Workspace {

    private static final Workspace LOCAL = of(StorageType.LFS);
    private static final Workspace REMOTE = of(StorageType.HDFS);

    /** Scala-friendly local workspace singleton. */
    public static final Workspace local = LOCAL;

    /** Scala-friendly remote workspace singleton. */
    public static final Workspace remote = REMOTE;

    static {
        LOCAL.initPaths();
    }

    private final StorageType storageType;

    /**
     * Path fields are private so Scala field-style access (e.g. {@code Workspace.local.APP_JARS})
     * resolves to accessor methods and triggers lazy initialization. Public fields would bypass
     * {@link #ensurePathsInitialized()} and stay null when remote paths are deferred.
     */
    private String WORKSPACE;
    private String APP_PLUGINS;
    private String APP_CLIENT;
    private String APP_SHIMS;
    private String APP_UPLOADS;
    private String APP_PYTHON;
    private String APP_PYTHON_VENV;
    private String APP_WORKSPACE;
    private String APP_FLINK;
    private String APP_SPARK;
    private String APP_BACKUPS;
    private String APP_SAVEPOINTS;
    private String APP_JARS;

    private volatile boolean pathsInitialized;

    private Workspace(StorageType storageType) {
        this.storageType = storageType;
    }

    public static Workspace of(StorageType storageType) {
        return new Workspace(storageType);
    }

    public static Workspace local() {
        return LOCAL;
    }

    public static Workspace remote() {
        return REMOTE;
    }

    /** local build path */
    public static String APP_LOCAL_DIST() {
        return local.WORKSPACE() + "/dist";
    }

    /** dirPath of the maven local repository with built-in compilation process */
    public static String MAVEN_LOCAL_PATH() {
        return local.WORKSPACE() + "/mvnrepo";
    }

    /** local sourceCode path.(for git...) */
    public static String PROJECT_LOCAL_PATH() {
        return local.WORKSPACE() + "/project";
    }

    /** local log path. */
    public static String LOG_LOCAL_PATH() {
        return local.WORKSPACE() + "/logs";
    }

    /** project build log path. */
    public static String PROJECT_BUILD_LOG_PATH() {
        return LOG_LOCAL_PATH() + "/build_logs";
    }

    /** project archives path */
    public static String ARCHIVES_FILE_PATH() {
        return remote.WORKSPACE() + "/historyserver/archive";
    }

    public String WORKSPACE() {
        ensurePathsInitialized();
        return WORKSPACE;
    }

    public String APP_PLUGINS() {
        ensurePathsInitialized();
        return APP_PLUGINS;
    }

    public String APP_CLIENT() {
        ensurePathsInitialized();
        return APP_CLIENT;
    }

    public String APP_SHIMS() {
        ensurePathsInitialized();
        return APP_SHIMS;
    }

    public String APP_UPLOADS() {
        ensurePathsInitialized();
        return APP_UPLOADS;
    }

    public String APP_PYTHON() {
        ensurePathsInitialized();
        return APP_PYTHON;
    }

    public String APP_PYTHON_VENV() {
        ensurePathsInitialized();
        return APP_PYTHON_VENV;
    }

    public String APP_WORKSPACE() {
        ensurePathsInitialized();
        return APP_WORKSPACE;
    }

    public String APP_FLINK() {
        ensurePathsInitialized();
        return APP_FLINK;
    }

    public String APP_SPARK() {
        ensurePathsInitialized();
        return APP_SPARK;
    }

    public String APP_BACKUPS() {
        ensurePathsInitialized();
        return APP_BACKUPS;
    }

    public String APP_SAVEPOINTS() {
        ensurePathsInitialized();
        return APP_SAVEPOINTS;
    }

    public String APP_JARS() {
        ensurePathsInitialized();
        return APP_JARS;
    }

    public String getAppJars() {
        return APP_JARS();
    }

    private void ensurePathsInitialized() {
        if (!pathsInitialized) {
            synchronized (this) {
                if (!pathsInitialized) {
                    initPaths();
                    pathsInitialized = true;
                }
            }
        }
    }

    private void initPaths() {
        if (pathsInitialized) {
            return;
        }
        WORKSPACE = computeWorkspace();
        APP_PLUGINS = WORKSPACE + "/plugins";
        APP_CLIENT = WORKSPACE + "/client";
        APP_SHIMS = WORKSPACE + "/shims";
        APP_UPLOADS = WORKSPACE + "/uploads";
        APP_PYTHON = WORKSPACE + "/python";
        APP_PYTHON_VENV = APP_PYTHON + "/venv.zip";
        APP_WORKSPACE = WORKSPACE + "/workspace";
        APP_FLINK = WORKSPACE + "/flink";
        APP_SPARK = WORKSPACE + "/spark";
        APP_BACKUPS = WORKSPACE + "/backups";
        APP_SAVEPOINTS = WORKSPACE + "/savepoints";
        APP_JARS = WORKSPACE + "/jars";
        pathsInitialized = true;
    }

    private String computeWorkspace() {
        if (storageType == StorageType.LFS) {
            String path = getConfigValue(CommonConfig.STREAMPARK_WORKSPACE_LOCAL());
            if (path == null) {
                throw new IllegalArgumentException("[StreamPark] streampark.workspace.local must not be null");
            }
            return path;
        }
        String path = getConfigValue(CommonConfig.STREAMPARK_WORKSPACE_REMOTE());
        if (path == null || path.isEmpty()) {
            return HdfsUtils.getDefaultFS() + CommonConfig.STREAMPARK_WORKSPACE_REMOTE().getDefaultValue();
        }
        String defaultFs = HdfsUtils.getDefaultFS();
        if (path.startsWith("hdfs://")) {
            if (path.startsWith(defaultFs)) {
                return path;
            }
            String hdfsPath = URI.create(path).getPath();
            return defaultFs + hdfsPath;
        }
        return defaultFs + path;
    }

    @SuppressWarnings("unchecked")
    private <T> T getConfigValue(InternalOption option) {
        String systemValue = SystemPropertyUtils.get(option.getKey());
        Object configValue = InternalConfigHolder.get(option);
        Object defaultValue = option.getDefaultValue();
        if (systemValue == null && configValue == null) {
            return (T) defaultValue;
        }
        if (systemValue == null) {
            return (T) configValue;
        }
        if (configValue == null) {
            return StringCastUtils.cast(systemValue, option.getClassType());
        }
        if (configValue.equals(defaultValue)) {
            return StringCastUtils.cast(systemValue, option.getClassType());
        }
        return (T) configValue;
    }
}
