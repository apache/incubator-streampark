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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {

    @Test
    void isJarFileTypeShouldReturnTrueForValidJarHeader() {
        byte[] jarFileContent = new byte[]{0x50, 0x4B, 0x03, 0x04};
        assertTrue(FileUtils.isJarFileType(new ByteArrayInputStream(jarFileContent)));
    }

    @Test
    void isJarFileTypeShouldReturnFalseForNonJarHeader() {
        byte[] textFileContent = new byte[]{0x54, 0x45, 0x53, 0x54};
        assertFalse(FileUtils.isJarFileType(new ByteArrayInputStream(textFileContent)));
    }

    @Test
    void existsShouldReturnTrueIfFileExists() throws Exception {
        File existingFile = File.createTempFile("existing", ".txt");
        try {
            assertTrue(FileUtils.exists(existingFile));
        } finally {
            existingFile.delete();
        }
    }

    @Test
    void existsShouldReturnFalseIfFileDoesNotExist() {
        assertFalse(FileUtils.exists(new File("non_existing_file.txt")));
    }

    @Test
    void createTempDirShouldCreateDirectory() {
        File tempDir = FileUtils.createTempDir();
        assertTrue(tempDir.exists());
        assertTrue(tempDir.isDirectory());
        tempDir.delete();
    }

    @Test
    void getPathFromEnvShouldReturnExistingPath() {
        String envName = System.getenv("TMPDIR") != null ? "TMPDIR" : "TEMP";
        if (System.getenv(envName) != null || System.getProperty(envName) != null) {
            String path = FileUtils.getPathFromEnv(envName);
            assertNotNull(path);
            assertFalse(path.isEmpty());
        }
    }

    @Test
    void getPathFromEnvShouldThrowIfVariableNotSet() {
        assertThrows(NullPointerException.class, () -> FileUtils.getPathFromEnv("NON_EXISTING_ENV_VAR"));
    }

    @Test
    void resolvePathShouldThrowIfParentDoesNotExist() {
        assertThrows(IllegalArgumentException.class,
            () -> FileUtils.resolvePath("/tmp/nonexistent-parent-114514", "child.txt"));
    }

    @Test
    void getSuffixShouldThrowIfFilenameIsNull() {
        assertThrows(NullPointerException.class, () -> FileUtils.getSuffix(null));
    }

    @Test
    void readInputStreamShouldReadBytes() throws Exception {
        String inputData = "Hello";
        byte[] byteArray = new byte[inputData.length()];
        FileUtils.readInputStream(new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8)), byteArray);
        assertEquals(inputData, new String(byteArray, StandardCharsets.UTF_8));
    }

    @Test
    void readFileShouldReadContent() throws Exception {
        File file = File.createTempFile("read-file", ".txt");
        try {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write("Hello".getBytes(StandardCharsets.UTF_8));
            }
            assertEquals("Hello", FileUtils.readFile(file));
        } finally {
            file.delete();
        }
    }

    @Test
    void readEndOfFileShouldReadTail() throws Exception {
        File file = File.createTempFile("read-end", ".txt");
        try {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write("1234567890".getBytes(StandardCharsets.UTF_8));
            }
            assertArrayEquals("67890".getBytes(StandardCharsets.UTF_8), FileUtils.readEndOfFile(file, 5));
        } finally {
            file.delete();
        }
    }

    @Test
    void readEndOfFileShouldReadEntireFileWhenLimitExceedsSize() throws Exception {
        File file = File.createTempFile("read-end-all", ".txt");
        try {
            String content = "1234567890";
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            }
            assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), FileUtils.readEndOfFile(file, 15));
        } finally {
            file.delete();
        }
    }

    @Test
    void readFileFromOffsetShouldReadPartialContent() throws Exception {
        File file = File.createTempFile("read-offset", ".txt");
        try {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write("1234567890".getBytes(StandardCharsets.UTF_8));
            }
            assertArrayEquals("67890".getBytes(StandardCharsets.UTF_8), FileUtils.readFileFromOffset(file, 5, 5));
        } finally {
            file.delete();
        }
    }

    @Test
    void readFileFromOffsetShouldThrowWhenOffsetTooLarge() throws Exception {
        File file = File.createTempFile("read-offset-bad", ".txt");
        try {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write("123".getBytes(StandardCharsets.UTF_8));
            }
            assertThrows(IllegalArgumentException.class, () -> FileUtils.readFileFromOffset(file, 15, 5));
        } finally {
            file.delete();
        }
    }

    @Test
    void tailOfShouldReturnNullIfFileDoesNotExist() throws Exception {
        assertNull(FileUtils.tailOf("/nonexistent_file.txt", 0, 5));
    }
}
