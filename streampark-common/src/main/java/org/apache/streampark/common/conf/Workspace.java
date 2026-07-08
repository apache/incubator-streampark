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

/** Workspace path resolver for local and remote storage. */
public class Workspace {

    private static final Workspace LOCAL = new Workspace(StorageType.LFS);
    private static final Workspace REMOTE = new Workspace(StorageType.HDFS);

    private final StorageType storageType;

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

    public static String appLocalDist() {
        return local().getWorkspace() + "/dist";
    }

    public static String mavenLocalPath() {
        return local().getWorkspace() + "/mvnrepo";
    }

    public static String projectLocalPath() {
        return local().getWorkspace() + "/project";
    }

    public static String logLocalPath() {
        return local().getWorkspace() + "/logs";
    }

    public static String projectBuildLogPath() {
        return logLocalPath() + "/build_logs";
    }

    public static String archivesFilePath() {
        return remote().getWorkspace() + "/historyserver/archive";
    }

    private <T> T getConfigValue(InternalOption option) {
        String s = SystemPropertyUtils.get(option.getKey());
        T v = InternalConfigHolder.get(option);
        T d = StringCastUtils.cast(option.getDefaultValue().toString(), (Class<T>) option.getClassType());
        if (s == null && v == null) {
            return d;
        }
        if (s == null) {
            return v;
        }
        if (v == null) {
            return StringCastUtils.cast(s, (Class<T>) option.getClassType());
        }
        if (v.equals(d)) {
            return StringCastUtils.cast(s, (Class<T>) option.getClassType());
        }
        return v;
    }

    public String getWorkspace() {
        switch (storageType) {
            case LFS:
                String path = getConfigValue(CommonConfig.STREAMPARK_WORKSPACE_LOCAL);
                if (path == null) {
                    throw new IllegalArgumentException("[StreamPark] streampark.workspace.local must not be null");
                }
                return path;
            case HDFS:
                String remotePath = getConfigValue(CommonConfig.STREAMPARK_WORKSPACE_REMOTE);
                if (remotePath == null || remotePath.isEmpty()) {
                    return HdfsUtils.getDefaultFS()
                        + CommonConfig.STREAMPARK_WORKSPACE_REMOTE.getDefaultValue();
                }
                String defaultFs = HdfsUtils.getDefaultFS();
                if (remotePath.startsWith("hdfs://")) {
                    if (remotePath.startsWith(defaultFs)) {
                        return remotePath;
                    }
                    String hdfsPath = URI.create(remotePath).getPath();
                    return defaultFs + hdfsPath;
                }
                return defaultFs + remotePath;
            default:
                throw new IllegalStateException("Unsupported storage type: " + storageType);
        }
    }

    public String getAppPlugins() {
        return getWorkspace() + "/plugins";
    }

    public String getAppClient() {
        return getWorkspace() + "/client";
    }

    public String getAppShims() {
        return getWorkspace() + "/shims";
    }

    public String getAppUploads() {
        return getWorkspace() + "/uploads";
    }

    public String getAppPython() {
        return getWorkspace() + "/python";
    }

    public String getAppPythonVenv() {
        return getAppPython() + "/venv.zip";
    }

    public String getAppWorkspace() {
        return getWorkspace() + "/workspace";
    }

    public String getAppFlink() {
        return getWorkspace() + "/flink";
    }

    public String getAppSpark() {
        return getWorkspace() + "/spark";
    }

    public String getAppBackups() {
        return getWorkspace() + "/backups";
    }

    public String getAppSavepoints() {
        return getWorkspace() + "/savepoints";
    }

    public String getAppJars() {
        return getWorkspace() + "/jars";
    }
}
