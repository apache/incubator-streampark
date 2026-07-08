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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Path resolution helpers for user-supplied file locations. */
public final class SafePathUtils {

    private SafePathUtils() {}

    public static Path resolveConfigPath(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("filename must not be blank");
        }
        if (filename.contains("..")) {
            throw new IllegalArgumentException("invalid file path: " + filename);
        }
        Path base = Paths.get("").toAbsolutePath().normalize();
        Path resolved = base.resolve(filename).normalize();
        if (!Paths.get(filename).isAbsolute() && !resolved.startsWith(base)) {
            throw new IllegalArgumentException("invalid file path: " + filename);
        }
        return resolved;
    }

    public static InputStream openConfigFile(String filename) throws IOException {
        return Files.newInputStream(resolveConfigPath(filename));
    }

    public static Path resolveJarPath(java.net.URL jar) throws IOException {
        try {
            String location = jar.toString();
            if (location.contains("..")) {
                throw new IOException("JAR file path is invalid " + jar);
            }
            Path base = Paths.get("").toAbsolutePath().normalize();
            return base.resolve(Paths.get(jar.toURI())).normalize();
        } catch (Exception e) {
            throw new IOException("JAR file path is invalid " + jar, e);
        }
    }
}
