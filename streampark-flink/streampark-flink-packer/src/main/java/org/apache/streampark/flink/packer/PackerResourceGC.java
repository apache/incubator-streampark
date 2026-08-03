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
import org.apache.streampark.common.util.LoggerSupport;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Garbage resource collector during packing. */
public final class PackerResourceGC extends LoggerSupport {

    private static final PackerResourceGC INSTANCE = new PackerResourceGC();

    private static final String APP_WORKSPACE_PATH = Workspace.local().APP_WORKSPACE();

    private PackerResourceGC() {
    }

    /** Start a building legacy resources collection process. */
    public static void startGc(Integer expiredHours) {
        INSTANCE.doStartGc(expiredHours);
    }

    private void doStartGc(Integer expiredHours) {
        File appWorkspace = new File(APP_WORKSPACE_PATH);
        if (!appWorkspace.exists()) {
            return;
        }
        long evictedBarrier = System.currentTimeMillis() - expiredHours * 3600L * 1000L;

        File[] dirs = appWorkspace.listFiles(File::isDirectory);
        if (dirs == null) {
            return;
        }
        List<File> evictedFiles = new ArrayList<>();
        for (File dir : dirs) {
            if (!dir.getName().contains("@")) {
                continue;
            }
            for (FileWithTime entry : findLastModifiedOfSubFile(dir)) {
                if (entry.lastModified < evictedBarrier) {
                    evictedFiles.add(entry.file);
                }
            }
        }

        if (evictedFiles.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (File path : evictedFiles) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(path.getAbsolutePath());
        }
        logInfo("Delete expired building resources, " + sb);
        for (File path : evictedFiles) {
            try {
                FileUtils.deleteDirectory(path);
            } catch (Exception ignored) {
                // ignore delete failures
            }
        }
    }

    private static List<FileWithTime> findLastModifiedOfSubFile(File file) {
        File[] children = file.listFiles();
        if (children == null) {
            return new ArrayList<>();
        }
        boolean isApplicationMode =
            Arrays.stream(children).anyMatch(f -> f.getName().contains(Constants.JAR_SUFFIX));
        List<FileWithTime> result = new ArrayList<>();
        if (isApplicationMode) {
            long max = Arrays.stream(children).mapToLong(File::lastModified).max().orElse(0L);
            result.add(new FileWithTime(file, max));
        } else {
            for (File subFile : children) {
                if (!subFile.isDirectory()) {
                    continue;
                }
                File[] subChildren = subFile.listFiles();
                if (subChildren == null) {
                    continue;
                }
                long max = Arrays.stream(subChildren).mapToLong(File::lastModified).max().orElse(0L);
                result.add(new FileWithTime(subFile, max));
            }
        }
        return result;
    }

    private static final class FileWithTime {

        private final File file;
        private final long lastModified;

        private FileWithTime(File file, long lastModified) {
            this.file = file;
            this.lastModified = lastModified;
        }
    }
}
