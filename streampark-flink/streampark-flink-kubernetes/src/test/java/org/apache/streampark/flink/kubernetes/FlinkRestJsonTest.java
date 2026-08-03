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

package org.apache.streampark.flink.kubernetes;

import org.apache.streampark.flink.kubernetes.rest.CheckpointInfo;
import org.apache.streampark.flink.kubernetes.rest.FlinkCheckpointResponse;
import org.apache.streampark.flink.kubernetes.rest.FlinkHistoryArchives;
import org.apache.streampark.flink.kubernetes.rest.FlinkJmConfigItem;
import org.apache.streampark.flink.kubernetes.rest.FlinkRestOverview;
import org.apache.streampark.flink.kubernetes.rest.JobsOverview;

import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.history.FsJobArchivist;
import org.apache.flink.runtime.webmonitor.history.ArchivedJson;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

class FlinkRestJsonTest {

    @Test
    void flinkRestOverview() {
        String json =
            "{\n"
                + "    \"taskmanagers\":2,\n"
                + "    \"slots-total\":10,\n"
                + "    \"slots-available\":2,\n"
                + "    \"jobs-running\":2,\n"
                + "    \"jobs-finished\":5,\n"
                + "    \"jobs-cancelled\":1,\n"
                + "    \"jobs-failed\":1,\n"
                + "    \"flink-version\":\"1.12.0\",\n"
                + "    \"flink-commit\":\"fc00492\"\n"
                + "}";

        FlinkRestOverview overview = FlinkRestOverview.parse(json).orElseThrow(AssertionError::new);
        Assertions.assertEquals(2, overview.slotsAvailable());
    }

    @Test
    void flinkRestJmConfigItem() {
        String json =
            "[\n"
                + "    {\n"
                + "        \"key\": \"taskmanager.memory.process.size\",\n"
                + "        \"value\": \"1024m\"\n"
                + "    },\n"
                + "    {\n"
                + "        \"key\": \"classloader.resolve-order\",\n"
                + "        \"value\": \"parent-first\"\n"
                + "    }\n"
                + "]";

        List<FlinkJmConfigItem> items = FlinkJmConfigItem.parse(json);
        Assertions.assertFalse(items.isEmpty());
        Assertions.assertEquals("taskmanager.memory.process.size", items.get(0).key());
    }

    @Test
    void testJobDetails() {
        String json =
            "{\n"
                + "    \"jobs\": [\n"
                + "        {\n"
                + "            \"jid\": \"4579b7a235f0756483da3c3618081bc2\",\n"
                + "            \"name\": \"FLink SQL\",\n"
                + "            \"state\": \"RUNNING\",\n"
                + "            \"start-time\": 1647616038354,\n"
                + "            \"end-time\": -1,\n"
                + "            \"duration\": 43912,\n"
                + "            \"last-modification\": 1647616039219,\n"
                + "            \"tasks\": {\n"
                + "                \"total\": 1,\n"
                + "                \"created\": 0,\n"
                + "                \"scheduled\": 0,\n"
                + "                \"deploying\": 0,\n"
                + "                \"running\": 1,\n"
                + "                \"finished\": 0,\n"
                + "                \"canceling\": 0,\n"
                + "                \"canceled\": 0,\n"
                + "                \"failed\": 0,\n"
                + "                \"reconciling\": 0\n"
                + "            }\n"
                + "        }\n"
                + "    ]\n"
                + "}";

        Optional<JobsOverview> jobDetails = JobsOverview.parse(json);
        Assertions.assertTrue(jobDetails.isPresent());
        Assertions.assertEquals(1, jobDetails.get().jobs().size());
    }

    @Test
    void testCheckpoint() {
        String json =
            "{\n"
                + "    \"latest\":{\n"
                + "        \"completed\":{\n"
                + "            \"id\":1914,\n"
                + "            \"status\":\"COMPLETED\",\n"
                + "            \"is_savepoint\":false,\n"
                + "            \"trigger_timestamp\":1658138497283,\n"
                + "            \"checkpoint_type\":\"CHECKPOINT\",\n"
                + "            \"external_path\":\"oss:///streampark/prod/checkpoints/chk-1914\"\n"
                + "        }\n"
                + "    }\n"
                + "}";

        Optional<CheckpointInfo> checkpoint = FlinkCheckpointResponse.parseCompleted(json);
        Assertions.assertTrue(checkpoint.isPresent());
        Assertions.assertEquals(1914L, checkpoint.get().id());
    }

    @Test
    void testHistoryArchives() throws Exception {
        Path archivePath =
            new Path("src/test/resources/d933fa6c785f0db6dccc6cc05dd43bab.json");
        String jobId = "d933fa6c785f0db6dccc6cc05dd43bab";
        Collection<ArchivedJson> archivedJson = FsJobArchivist.getArchivedJsons(archivePath);
        Assertions.assertNotNull(archivedJson);

        org.apache.streampark.flink.kubernetes.model.TrackId trackId =
            org.apache.streampark.flink.kubernetes.model.TrackId.onApplication(
                "default", "test", 1L, jobId, "1", new java.util.Properties());

        String state = FlinkHistoryArchives.getJobStateFromArchiveFile(trackId);
        Assertions.assertNotNull(state);
    }
}
