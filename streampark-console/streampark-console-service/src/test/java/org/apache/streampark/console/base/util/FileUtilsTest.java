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
package org.apache.streampark.console.base.util;

import org.apache.streampark.common.util.FileUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Random;

/** Test for {@link FileUtils} */
class FileUtilsTest {

    @Test
    void testReadEndOfFile(@TempDir Path tempDir) throws IOException {
        Path filePath = tempDir.resolve("tmp_file");
        File file = filePath.toFile();
        FileOutputStream outputStream = new FileOutputStream(file);
        Random random = new Random();
        int fileSize = 1000000;
        byte[] fileBytes = new byte[fileSize];
        random.nextBytes(fileBytes);
        outputStream.write(fileBytes);
        outputStream.flush();
        outputStream.close();

        // The read size is larger than the file size
        byte[] readBytes = FileUtils.readEndOfFile(file, fileSize + 1);
        Assertions.assertArrayEquals(fileBytes, readBytes);

        // The read size is equals the file size
        readBytes = FileUtils.readEndOfFile(file, fileSize);
        Assertions.assertArrayEquals(fileBytes, readBytes);

        // The read size is less than the file size
        int readSize = 50000;
        readBytes = FileUtils.readEndOfFile(file, readSize);
        byte[] expectedBytes = new byte[readSize];
        System.arraycopy(fileBytes, fileSize - readSize, expectedBytes, 0, expectedBytes.length);
        Assertions.assertArrayEquals(expectedBytes, readBytes);
    }

    @Test
    void testReadEndOfFileWithChinese(@TempDir Path tempDir) throws IOException {
        Path filePath = tempDir.resolve("tmp_file");
        File file = filePath.toFile();
        PrintWriter writer = new PrintWriter(file);
        String logWithChinese = "Hello world! 你好啊，hello xxxx";
        writer.write(logWithChinese);
        writer.close();

        byte[] bytes = FileUtils.readEndOfFile(file, 1000000);
        String readString = new String(bytes);
        Assertions.assertEquals(logWithChinese, readString);
    }

    @Test
    void testReadFileFromOffset(@TempDir Path tempDir) throws IOException {
        Path filePath = tempDir.resolve("tmp_file");
        File file = filePath.toFile();
        FileOutputStream outputStream = new FileOutputStream(file);
        Random random = new Random();
        int fileSize = 1000000;
        byte[] fileBytes = new byte[fileSize];
        random.nextBytes(fileBytes);
        outputStream.write(fileBytes);
        outputStream.flush();
        outputStream.close();

        // The read size is larger than the file size
        byte[] readBytes = FileUtils.readFileFromOffset(file, 0, fileSize + 1);
        Assertions.assertArrayEquals(fileBytes, readBytes);

        // The read size is equals the file size
        readBytes = FileUtils.readFileFromOffset(file, 0, fileSize);
        Assertions.assertArrayEquals(fileBytes, readBytes);

        // The read size is less than the file size
        int readSize = 3456;
        readBytes = new byte[fileSize];
        byte[] tmpReadBytes;
        for (int i = 0; i < fileSize; i += tmpReadBytes.length) {
            tmpReadBytes = FileUtils.readFileFromOffset(file, i, readSize);
            Assertions.assertTrue(tmpReadBytes.length <= readSize);
            System.arraycopy(tmpReadBytes, 0, readBytes, i, tmpReadBytes.length);
        }
        Assertions.assertArrayEquals(fileBytes, readBytes);
    }
}
