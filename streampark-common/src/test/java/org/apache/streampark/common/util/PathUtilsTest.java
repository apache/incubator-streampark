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

package org.apache.streampark.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies containment guarantees for request-supplied child paths. */
class PathUtilsTest {

    @TempDir
    Path directory;

    @TempDir
    Path outsideDirectory;

    @Test
    void resolveContainedChild() throws Exception {
        Path child = Files.createDirectory(directory.resolve("module"));

        assertThat(PathUtils.resolveChild(directory, "module"))
            .isEqualTo(child.toRealPath());
    }

    @Test
    void resolveAbsoluteChild() throws Exception {
        Path child = Files.createFile(directory.resolve("application.yaml"));

        assertThat(PathUtils.resolveChild(directory, child.toString()))
            .isEqualTo(child.toRealPath());
    }

    @Test
    void rejectEscapingChild() {
        assertThatThrownBy(() -> PathUtils.resolveChild(directory, "../outside"))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("escapes trusted directory");
    }

    @Test
    void rejectAbsoluteOutsideRoot() throws Exception {
        Path outside = Files.createFile(outsideDirectory.resolve("outside.yaml"));

        assertThatThrownBy(() -> PathUtils.resolveChild(directory, outside.toString()))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("escapes trusted directory");
    }

    @Test
    void rejectSymlinkOutsideRoot() throws Exception {
        Path outside = Files.createFile(outsideDirectory.resolve("linked-target.yaml"));
        Path link = Files.createSymbolicLink(directory.resolve("linked.yaml"), outside);

        assertThatThrownBy(() -> PathUtils.resolveChild(directory, link.toString()))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("escapes trusted directory");
    }
}
