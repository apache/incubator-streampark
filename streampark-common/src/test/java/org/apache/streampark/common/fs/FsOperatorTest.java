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

package org.apache.streampark.common.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FsOperatorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldReadSizeAndSha256ThroughFileSystemAbstraction() throws Exception {
        Path artifact = tempDirectory.resolve("artifact.jar");
        Files.writeString(artifact, "streampark-artifact", StandardCharsets.UTF_8);

        assertEquals(19, FsOperator.lfs().fileSize(artifact.toString()));
        assertEquals(
            "090bb96c6b9e8c2b35af8060c74216970f16e83063c914cb360449c62a44472f",
            FsOperator.lfs().fileSha256(artifact.toString()));
        try (java.io.InputStream input = FsOperator.lfs().open(artifact.toString())) {
            assertArrayEquals(
                "streampark-artifact".getBytes(StandardCharsets.UTF_8),
                input.readAllBytes());
        }
    }
}
