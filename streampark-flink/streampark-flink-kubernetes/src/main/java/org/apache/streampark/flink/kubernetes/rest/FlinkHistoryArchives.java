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

package org.apache.streampark.flink.kubernetes.rest;

import org.apache.streampark.common.util.JsonUtils;
import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.flink.kubernetes.helper.KubernetesDeploymentHelper;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.history.FsJobArchivist;
import org.apache.flink.runtime.webmonitor.history.ArchivedJson;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/** Utility for parsing Flink job history archive JSON. */
public final class FlinkHistoryArchives extends LoggerSupport {

    private static final FlinkHistoryArchives INSTANCE = new FlinkHistoryArchives();

    private static final String FAILED_STATE = "FAILED";

    private FlinkHistoryArchives() {
    }

    public static String getJobStateFromArchiveFile(TrackId trackId) {
        try {
            if (trackId.jobId() == null) {
                throw new IllegalArgumentException(
                    "[StreamPark] getJobStateFromArchiveFile: JobId cannot be null.");
            }
            String archiveDir = trackId.properties().getProperty(JobManagerOptions.ARCHIVE_DIR.key());
            if (archiveDir == null) {
                return FAILED_STATE;
            }
            Path archivePath = new Path(archiveDir, trackId.jobId());
            Collection<ArchivedJson> archivedJsons =
                FsJobArchivist.getArchivedJsons(archivePath);
            if (archivedJsons == null || archivedJsons.isEmpty()) {
                return FAILED_STATE;
            }
            for (ArchivedJson archivedJson : archivedJsons) {
                String path = archivedJson.getPath();
                if (path.equals("/jobs/" + trackId.jobId() + "/exceptions")) {
                    writeExceptionLog(archivedJson.getJson(), trackId.jobId());
                } else if (path.equals("/jobs/overview")) {
                    String state = extractJobState(archivedJson.getJson(), trackId.jobId());
                    if (state != null) {
                        return state;
                    }
                }
            }
            return FAILED_STATE;
        } catch (Exception e) {
            return FAILED_STATE;
        }
    }

    private static void writeExceptionLog(String json, String jobId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = JsonUtils.read(json, Map.class);
            Object log = root.get("root-exception");
            if (log != null) {
                String path = KubernetesDeploymentHelper.getJobErrorLog(jobId);
                File file = new File(path);
                Files.asCharSink(file, Charsets.UTF_8).write(log.toString());
                INSTANCE.logInfo(" error path: " + path);
            }
        } catch (Exception ignored) {
            // ignore parse/write failures
        }
    }

    private static String extractJobState(String json, String jobId) {
        try {
            JobsOverview overview = FlinkRestJsonMapper.MAPPER.readValue(json, JobsOverview.class);
            if (overview.jobs() == null) {
                return null;
            }
            for (JobDetail detail : overview.jobs()) {
                if (jobId.equals(detail.jid())) {
                    return detail.state();
                }
            }
        } catch (Exception ignored) {
            // ignore parse failures
        }
        return null;
    }
}
