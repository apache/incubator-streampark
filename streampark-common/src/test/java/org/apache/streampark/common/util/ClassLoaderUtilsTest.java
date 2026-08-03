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

import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassLoaderUtilsTest {

    @Test
    void loadJarShouldAppendJarToSystemClassloader() throws Exception {
        File jarFile = File.createTempFile("streampark-classloader-test", ".jar");
        jarFile.deleteOnExit();
        try {
            try (JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(jarFile))) {
                jarOut.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
                jarOut.write("Manifest-Version: 1.0\n".getBytes("UTF-8"));
                jarOut.closeEntry();
            }
            ClassLoaderUtils.loadJar(jarFile.getAbsolutePath());
        } finally {
            jarFile.delete();
        }
    }

    @Test
    void loadResourceShouldAppendDirectoryToSystemClassloader() throws Exception {
        File dir = FileUtils.createTempDir();
        try {
            ClassLoaderUtils.loadResource(dir.getAbsolutePath());
        } finally {
            assertTrue(dir.delete());
        }
    }
}
