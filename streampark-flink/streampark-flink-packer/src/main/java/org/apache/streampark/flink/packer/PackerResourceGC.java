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

package org.apache.streampark.flink.packer;

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.constants.Constants;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public final class PackerResourceGC {

    private static final String APP_WORKSPACE_PATH = Workspace.local().getAppWorkspace();
    private PackerResourceGC() {
    }

    public static void startGc(Integer expiredHours) {
        File appWorkspace = new File(APP_WORKSPACE_PATH);
        if (!appWorkspace.exists())
            return;
        long evictedBarrier = System.currentTimeMillis() - expiredHours * 3600L * 1000L;
        File[] evictedFiles = java.util.Arrays.stream(appWorkspace.listFiles())
            .filter(File::isDirectory)
            .filter(f -> f.getName().contains("@"))
            .flatMap(f -> findLastModifiedOfSubFile(f).stream())
            .filter(e -> e.getValue() < evictedBarrier)
            .map(java.util.Map.Entry::getKey)
            .toArray(File[]::new);
        if (evictedFiles.length == 0)
            return;
        StringBuilder sb = new StringBuilder();
        for (File f : evictedFiles)
            sb.append(f.getAbsolutePath()).append(", ");
        log.info("Delete expired building resources, {}", sb);
        for (File path : evictedFiles) {
            try {
                FileUtils.deleteDirectory(path);
            } catch (Exception ignored) {
            }
        }
    }

    private static java.util.List<java.util.Map.Entry<File, Long>> findLastModifiedOfSubFile(File file) {
        boolean isApplicationMode = java.util.Arrays.stream(file.listFiles())
            .anyMatch(f -> f.getName().contains(Constants.JAR_SUFFIX));
        if (isApplicationMode) {
            long max = java.util.Arrays.stream(file.listFiles()).mapToLong(File::lastModified).max().orElse(0L);
            return java.util.Collections.singletonList(java.util.Map.entry(file, max));
        }
        java.util.List<java.util.Map.Entry<File, Long>> result = new java.util.ArrayList<>();
        for (File subFile : file.listFiles()) {
            if (subFile.isDirectory()) {
                long max = java.util.Arrays.stream(subFile.listFiles()).mapToLong(File::lastModified).max().orElse(0L);
                result.add(java.util.Map.entry(subFile, max));
            }
        }
        return result;
    }
}
