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

package org.apache.streampark.archives;

import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.history.FsJobArchivist;
import org.apache.flink.runtime.webmonitor.history.ArchivedJson;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class FetchArchives {

    private static String FAILED = "FAILED";

    public static String getJobStateFromArchiveFile(String jobId, String archivePath) {
        try {
            Objects.requireNonNull(jobId, "JobId cannot be empty.");
            Objects.requireNonNull(archivePath, "archivePath cannot be empty.");
            Path refreshPath = new Path(archivePath);
            FileSystem refreshFS = refreshPath.getFileSystem();
            // contents of /:refreshDir
            FileStatus[] jobArchives = listArchives(refreshFS, refreshPath);
            Path jobArchivePath = Arrays.stream(jobArchives)
                .map(FileStatus::getPath)
                .filter(path -> path.getName().equals(jobId)).findFirst().orElse(null);

            Objects.requireNonNull(jobArchivePath, "Archive Log Path Exception");
            for (ArchivedJson archive : FsJobArchivist.getArchivedJsons(jobArchivePath)) {
                String path = archive.getPath();
                if (path.equals("/jobs/overview")) {
                    String json = archive.getJson();
                    ObjectMapper objectMapper = new ObjectMapper();
                    Jobs jobs = objectMapper.readValue(json, Jobs.class);
                    List<Overview> overviews = jobs.getJobs();
                    return overviews.get(0).getState();
                }
            }
            return FAILED;
        } catch (Exception e) {
            return FAILED;
        }
    }

    private static FileStatus[] listArchives(FileSystem refreshFS, Path refreshDir)
        throws IOException {
        // contents of /:refreshDir
        FileStatus[] jobArchives = refreshFS.listStatus(refreshDir);
        if (jobArchives == null) {
            // the entire refreshDirectory was removed
            return new FileStatus[0];
        }
        Arrays.sort(
            jobArchives, Comparator.comparingLong(FileStatus::getModificationTime).reversed());
        return jobArchives;
    }
}
