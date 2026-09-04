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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Resolves untrusted paths within an explicitly trusted directory. */
public final class PathUtils {

    private PathUtils() {
    }

    /**
     * Resolves an existing path without allowing it to escape {@code directory}. Absolute paths
     * are accepted only when they remain inside the trusted directory. Both paths are resolved
     * through symbolic links before the final containment check.
     *
     * @param directory trusted parent directory
     * @param child untrusted relative or absolute path
     * @return normalized path contained by the trusted directory
     * @throws IOException when canonical path resolution fails or the child escapes the directory
     */
    public static Path resolveChild(Path directory, String child) throws IOException {
        Objects.requireNonNull(directory, "directory must not be null");
        if (child == null || child.trim().isEmpty()) {
            throw new IOException("child path must not be blank");
        }

        Path lexicalRoot = directory.toAbsolutePath().normalize();
        Path requested = Path.of(child);
        Path candidate = requested.isAbsolute()
            ? requested.normalize()
            : lexicalRoot.resolve(requested).normalize();
        if (!candidate.startsWith(lexicalRoot)) {
            throw new IOException("Path escapes trusted directory: " + child);
        }
        Path root = lexicalRoot.toRealPath();
        Path resolved = candidate.toRealPath();
        if (!resolved.startsWith(root)) {
            throw new IOException("Path escapes trusted directory: " + child);
        }
        return resolved;
    }
}
