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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.StorageType;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.console.core.util.ApplicationEntityUtils;
import org.apache.streampark.flink.packer.maven.DependencyInfo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ApplicationEntitySupport {

    Long getProjectId();

    Long getId();

    String getModule();

    String getDependency();

    Integer getRelease();

    Integer getRestartSize();

    Integer getRestartCount();

    @JsonIgnore
    StorageType getStorageType();

    @JsonIgnore
    default String getDistHome() {
        return ApplicationEntityUtils.distHome(getProjectId(), getModule());
    }

    @JsonIgnore
    default String getLocalAppHome() {
        return ApplicationEntityUtils.localAppHome(getId());
    }

    @JsonIgnore
    default String getRemoteAppHome() {
        return ApplicationEntityUtils.remoteAppHome(getId());
    }

    @JsonIgnore
    default DependencyInfo getDependencyInfo() {
        return ApplicationEntityUtils.dependencyInfo(getDependency());
    }

    @JsonIgnore
    default boolean isNeedRollback() {
        return ApplicationEntityUtils.needRollback(getRelease());
    }

    @JsonIgnore
    default boolean isNeedRestartOnFailed() {
        return ApplicationEntityUtils.needRestartOnFailed(getRestartSize(), getRestartCount());
    }

    @JsonIgnore
    default FsOperator getFsOperator() {
        return ApplicationEntityUtils.fsOperator(getStorageType());
    }

    @JsonIgnore
    default Workspace getWorkspace() {
        return ApplicationEntityUtils.workspace(getStorageType());
    }
}
