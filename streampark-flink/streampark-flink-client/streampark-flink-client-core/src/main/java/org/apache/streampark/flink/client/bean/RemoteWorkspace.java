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

import org.apache.streampark.common.configuration.Workspace;
import org.apache.streampark.common.core.FlinkVersion;
import org.apache.streampark.flink.util.FlinkUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Resolved HDFS paths used while submitting a Flink job through YARN. */
public class RemoteWorkspace {

    private final String flinkDistJar;
    private final String flinkLib;
    private final String flinkPlugins;
    private final String appJars;

    public static RemoteWorkspace resolve(FlinkVersion flinkVersion) {
        Workspace workspace = Workspace.REMOTE;
        String flinkHome = flinkVersion.flinkHome;
        File flinkHomeDir = new File(flinkHome);
        String flinkName;
        try {
            flinkName =
                FileUtils.isSymlink(flinkHomeDir)
                    ? flinkHomeDir.getCanonicalFile().getName()
                    : flinkHomeDir.getName();
        } catch (IOException e) {
            throw new UncheckedIOException(
                "Failed to resolve Flink home directory name: " + flinkHome, e);
        }
        String flinkHdfsHome = workspace.flink + "/" + flinkName;
        return new RemoteWorkspace(
            FlinkUtils.getFlinkDistJar(flinkHome),
            flinkHdfsHome + "/lib",
            flinkHdfsHome + "/plugins",
            workspace.jars);
    }

    private RemoteWorkspace(
                            String flinkDistJar,
                            String flinkLib,
                            String flinkPlugins,
                            String appJars) {
        this.flinkDistJar = flinkDistJar;
        this.flinkLib = flinkLib;
        this.flinkPlugins = flinkPlugins;
        this.appJars = appJars;
    }

    public String flinkDistJar() {
        return flinkDistJar;
    }

    public String flinkLib() {
        return flinkLib;
    }

    public String flinkPlugins() {
        return flinkPlugins;
    }

    public String appJars() {
        return appJars;
    }
}
