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

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.util.JsonUtils;

import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.history.FsJobArchivist;
import org.apache.flink.runtime.webmonitor.history.ArchivedJson;

import java.util.List;
import java.util.Objects;

public class FetchArchives {

    private static final String FAILED_STATE = "FAILED";

    public static String getJobStateFromArchiveFile(String jobId) {
        try {
            Objects.requireNonNull(jobId, "JobId cannot be empty.");
            Path archiveFilePath = new Path(String.format("%s/%s", Workspace.ARCHIVES_FILE_PATH(), jobId));
            for (ArchivedJson archive : FsJobArchivist.getArchivedJsons(archiveFilePath)) {
                String path = archive.getPath();
                if (path.equals("/jobs/overview")) {
                    String json = archive.getJson();
                    Jobs jobs = JsonUtils.read(json, Jobs.class);
                    List<Overview> overviews = jobs.getJobs();
                    return overviews.stream().filter(x -> x.getJid().equals(jobId)).map(Overview::getState).findFirst().orElse(FAILED_STATE);
                }
            }
            return FAILED_STATE;
        } catch (Exception e) {
            return FAILED_STATE;
        }
    }
}
