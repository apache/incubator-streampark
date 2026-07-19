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
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.common.util.TypeCastUtils;

import java.net.URI;

public final class Workspace {

    private static volatile Workspace localInstance;
    private static volatile Workspace remoteInstance;

    private final String workspace;
    private final String appPlugins;
    private final String appClient;
    private final String appShims;
    private final String appUploads;
    private final String appPython;
    private final String appPythonVenv;
    private final String appWorkspace;
    private final String appFlink;
    private final String appSpark;
    private final String appBackups;
    private final String appSavepoints;
    private final String appJars;

    private Workspace(StorageType storageType) {
        this.workspace = computeWorkspace(storageType);
        this.appPlugins = workspace + "/plugins";
        this.appClient = workspace + "/client";
        this.appShims = workspace + "/shims";
        this.appUploads = workspace + "/uploads";
        this.appPython = workspace + "/python";
        this.appPythonVenv = appPython + "/venv.zip";
        this.appWorkspace = workspace + "/workspace";
        this.appFlink = workspace + "/flink";
        this.appSpark = workspace + "/spark";
        this.appBackups = workspace + "/backups";
        this.appSavepoints = workspace + "/savepoints";
        this.appJars = workspace + "/jars";
    }

    public static Workspace of(StorageType storageType) {
        return new Workspace(storageType);
    }

    public static Workspace local() {
        if (localInstance == null) {
            synchronized (Workspace.class) {
                if (localInstance == null) {
                    localInstance = new Workspace(StorageType.LFS);
                }
            }
        }
        return localInstance;
    }

    public static Workspace remote() {
        if (remoteInstance == null) {
            synchronized (Workspace.class) {
                if (remoteInstance == null) {
                    remoteInstance = new Workspace(StorageType.HDFS);
                }
            }
        }
        return remoteInstance;
    }

    /** local build path */
    public static String APP_LOCAL_DIST() {
        return local().WORKSPACE() + "/dist";
    }

    /** dirPath of the maven local repository with built-in compilation process */
    public static String MAVEN_LOCAL_PATH() {
        return local().WORKSPACE() + "/mvnrepo";
    }

    /** local sourceCode path.(for git...) */
    public static String PROJECT_LOCAL_PATH() {
        return local().WORKSPACE() + "/project";
    }

    /** local log path. */
    public static String LOG_LOCAL_PATH() {
        return local().WORKSPACE() + "/logs";
    }

    /** project build log path. */
    public static String PROJECT_BUILD_LOG_PATH() {
        return LOG_LOCAL_PATH() + "/build_logs";
    }

    /** project archives path */
    public static String ARCHIVES_FILE_PATH() {
        return remote().WORKSPACE() + "/historyserver/archive";
    }

    public String WORKSPACE() {
        return workspace;
    }

    public String APP_PLUGINS() {
        return appPlugins;
    }

    public String APP_CLIENT() {
        return appClient;
    }

    /** store flink multi version support jars */
    public String APP_SHIMS() {
        return appShims;
    }

    public String APP_UPLOADS() {
        return appUploads;
    }

    public String APP_PYTHON() {
        return appPython;
    }

    public String APP_PYTHON_VENV() {
        return appPythonVenv;
    }

    public String APP_WORKSPACE() {
        return appWorkspace;
    }

    public String APP_FLINK() {
        return appFlink;
    }

    public String APP_SPARK() {
        return appSpark;
    }

    public String APP_BACKUPS() {
        return appBackups;
    }

    public String APP_SAVEPOINTS() {
        return appSavepoints;
    }

    /** store global public jars */
    public String APP_JARS() {
        return appJars;
    }

    private <T> T getConfigValue(InternalOption<T> option) {
        String s = SystemPropertyUtils.get(option.getKey());
        T v = InternalConfigHolder.get(option);
        if (s != null && v.equals(option.getDefaultValue()))
            return TypeCastUtils.cast(s, option.getClassType());
        return v;
    }

    private String computeWorkspace(StorageType storageType) {
        switch (storageType) {
            case LFS: {
                String path = getConfigValue(CommonConfig.STREAMPARK_WORKSPACE_LOCAL());
                if (path == null) {
                    throw new IllegalArgumentException(
                        "[StreamPark] streampark.workspace.local must not be null");
                }
                return path;
            }
            case HDFS: {
                String path = getConfigValue(CommonConfig.STREAMPARK_WORKSPACE_REMOTE());
                if (path.isEmpty()) {
                    return HdfsUtils.getDefaultFS()
                        + CommonConfig.STREAMPARK_WORKSPACE_REMOTE().getDefaultValue();
                }
                String defaultFs = HdfsUtils.getDefaultFS();
                if (path.startsWith("hdfs://")) {
                    if (path.startsWith(defaultFs)) {
                        return path;
                    }
                    return defaultFs + URI.create(path).getPath();
                }
                return defaultFs + path;
            }
            default:
                throw new IllegalArgumentException("Unsupported storage type: " + storageType);
        }
    }
}
