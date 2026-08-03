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

package org.apache.streampark.console.core.util;

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.StorageType;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.console.core.bean.Dependency;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.flink.packer.maven.DependencyInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ApplicationEntityUtils {

    private ApplicationEntityUtils() {
    }

    public static String distHome(Long projectId, String module) {
        String path = String.format("%s/%s/%s", Workspace.APP_LOCAL_DIST(), projectId.toString(), module);
        log.info("local distHome:{}", path);
        return path;
    }

    public static String localAppHome(Long id) {
        String path = String.format("%s/%s", Workspace.local().APP_WORKSPACE(), id.toString());
        log.info("local appHome:{}", path);
        return path;
    }

    public static String remoteAppHome(Long id) {
        String path = String.format("%s/%s", Workspace.remote().APP_WORKSPACE(), id.toString());
        log.info("remote appHome:{}", path);
        return path;
    }

    public static DependencyInfo dependencyInfo(String dependency) {
        return Dependency.toDependency(dependency).toJarPackDeps();
    }

    public static boolean needRollback(Integer release) {
        return ReleaseStateEnum.NEED_ROLLBACK.get() == release;
    }

    public static boolean needRestartOnFailed(Integer restartSize, Integer restartCount) {
        if (restartSize != null && restartCount != null) {
            return restartSize > 0 && restartCount <= restartSize;
        }
        return false;
    }

    public static FsOperator fsOperator(StorageType storageType) {
        return FsOperator.of(storageType);
    }

    public static Workspace workspace(StorageType storageType) {
        return Workspace.of(storageType);
    }
}
