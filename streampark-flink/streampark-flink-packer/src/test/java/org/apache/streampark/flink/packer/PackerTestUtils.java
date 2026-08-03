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

import org.codehaus.plexus.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/** Test utilities for packer module. */
public final class PackerTestUtils {

    private PackerTestUtils() {
    }

    public static String path(String resourcePath) {
        java.net.URL resource = PackerTestUtils.class.getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Missing test resource: " + resourcePath);
        }
        return resource.getFile();
    }

    public static Path createJar(Path jarPath, String entryName, byte[] content) throws IOException {
        Files.createDirectories(jarPath.getParent());
        try (
            OutputStream out = Files.newOutputStream(jarPath);
            JarOutputStream jarOut = new JarOutputStream(out)) {
            jarOut.putNextEntry(new JarEntry(entryName));
            jarOut.write(content);
            jarOut.closeEntry();
        }
        return jarPath;
    }

    public static boolean jarEquals(JarFile jar1, JarFile jar2, String entry) {
        InputStream s1 = null;
        InputStream s2 = null;
        try {
            s1 = jar1.getInputStream(jar1.getJarEntry(entry));
            s2 = jar2.getInputStream(jar2.getJarEntry(entry));
            return Arrays.equals(IOUtil.toByteArray(s1), IOUtil.toByteArray(s2));
        } catch (IOException e) {
            return false;
        } finally {
            if (s1 != null) {
                try {
                    s1.close();
                } catch (IOException ignored) {
                }
            }
            if (s2 != null) {
                try {
                    s2.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
