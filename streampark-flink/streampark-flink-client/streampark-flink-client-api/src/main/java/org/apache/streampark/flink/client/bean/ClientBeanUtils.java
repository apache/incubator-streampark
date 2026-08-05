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

import org.apache.streampark.common.conf.FlinkVersion;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.flink.util.FlinkUtils;

import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

final class ClientBeanUtils {

    private ClientBeanUtils() {
    }

    static HdfsWorkspace createHdfsWorkspace(FlinkVersion flinkVersion) {
        Workspace workspace = Workspace.remote();
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
        String flinkHdfsHome = workspace.APP_FLINK() + "/" + flinkName;
        return new HdfsWorkspace(
            flinkName,
            flinkHome,
            FlinkUtils.getFlinkDistJar(flinkHome),
            flinkHdfsHome + "/lib",
            flinkHdfsHome + "/plugins",
            workspace.APP_JARS());
    }

    @Nullable
    static Map<String, Object> copyPropertiesMap(@Nullable Map<String, Serializable> properties) {
        if (properties == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        result.putAll(properties);
        return result;
    }

    @Nullable
    static Map<String, Serializable> toSerializableMap(@Nullable Map<String, Object> properties) {
        if (properties == null) {
            return null;
        }
        Map<String, Serializable> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Serializable) {
                result.put(entry.getKey(), (Serializable) value);
            } else if (value != null) {
                result.put(entry.getKey(), value.toString());
            }
        }
        return result;
    }
}
