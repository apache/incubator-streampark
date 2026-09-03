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
import org.apache.streampark.common.util.HadoopUtils;

import org.apache.hadoop.fs.FileSystem;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceTest {

    @Test
    void bindWorkspaceToSnapshot() {
        Configuration first =
            Configuration.builder()
                .set(WorkspaceOptions.LOCAL_ROOT, "/tmp/first/", "test")
                .build();
        Configuration second =
            Configuration.builder()
                .set(WorkspaceOptions.LOCAL_ROOT, "/tmp/second", "test")
                .build();

        Workspace firstWorkspace = Workspace.of(StorageType.LFS, first);
        Workspace secondWorkspace = Workspace.of(StorageType.LFS, second);

        assertThat(firstWorkspace.root).isEqualTo("/tmp/first");
        assertThat(firstWorkspace.workspace).isEqualTo("/tmp/first/workspace");
        assertThat(firstWorkspace.jars).isEqualTo("/tmp/first/jars");
        assertThat(secondWorkspace.root).isEqualTo("/tmp/second");
    }

    @Test
    void avoidDuplicateRootSeparators() {
        Configuration configuration =
            Configuration.builder()
                .set(WorkspaceOptions.LOCAL_ROOT, "/", "test")
                .build();

        Workspace workspace = Workspace.of(StorageType.LFS, configuration);

        assertThat(workspace.root).isEqualTo("/");
        assertThat(workspace.workspace).isEqualTo("/workspace");
        assertThat(workspace.plugins).isEqualTo("/plugins");
    }

    @Test
    void rejectUnsupportedRemoteScheme() {
        Configuration configuration =
            Configuration.builder()
                .set(WorkspaceOptions.REMOTE_ROOT, "s3://bucket/streampark", "test")
                .build();

        assertThatThrownBy(() -> Workspace.of(StorageType.HDFS, configuration))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining(WorkspaceOptions.REMOTE_ROOT.key())
            .hasMessageContaining("hdfs scheme");
    }

    @Test
    void reportMalformedRemoteRoot() {
        Configuration configuration =
            Configuration.builder()
                .set(WorkspaceOptions.REMOTE_ROOT, "/invalid path", "test")
                .build();

        assertThatThrownBy(() -> Workspace.of(StorageType.HDFS, configuration))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining(WorkspaceOptions.REMOTE_ROOT.key())
            .hasMessageContaining("valid URI");
    }

    @Test
    void useDefaultHdfsAuthority() {
        org.apache.hadoop.conf.Configuration hadoopConfiguration = HadoopUtils.hadoopConf();
        String previousDefault = hadoopConfiguration.get(FileSystem.FS_DEFAULT_NAME_KEY);
        hadoopConfiguration.set(FileSystem.FS_DEFAULT_NAME_KEY, "hdfs://primary:8020");
        try {
            Configuration authorityless =
                Configuration.builder()
                    .set(WorkspaceOptions.REMOTE_ROOT, "hdfs:///streampark/", "test")
                    .build();
            Configuration anotherFileSystem =
                Configuration.builder()
                    .set(
                        WorkspaceOptions.REMOTE_ROOT,
                        "hdfs://secondary:8020/streampark",
                        "test")
                    .build();

            assertThat(Workspace.of(StorageType.HDFS, authorityless).root)
                .isEqualTo("hdfs://primary:8020/streampark");
            assertThatThrownBy(() -> Workspace.of(StorageType.HDFS, anotherFileSystem))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("does not match fs.defaultFS");
        } finally {
            if (previousDefault == null) {
                hadoopConfiguration.unset(FileSystem.FS_DEFAULT_NAME_KEY);
            } else {
                hadoopConfiguration.set(FileSystem.FS_DEFAULT_NAME_KEY, previousDefault);
            }
        }
    }

    @Test
    void rejectEarlyWorkspaceSnapshot() {
        Configuration changed =
            Configuration.builder(GlobalConfiguration.current())
                .set(WorkspaceOptions.LOCAL_ROOT, "/different-bootstrap-root", "test")
                .build();

        assertThatThrownBy(() -> Workspace.verifyInitializedFrom(changed))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining("before configuration bootstrap completed");
    }
}
