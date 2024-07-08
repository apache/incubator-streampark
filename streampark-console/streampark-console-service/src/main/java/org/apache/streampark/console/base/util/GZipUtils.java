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

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;

@Slf4j
public final class GZipUtils {

    private GZipUtils() {
    }

    /**
     * @param tarZipSource source dir
     * @param targetDir target dir
     */
    public static void deCompress(String tarZipSource, String targetDir) {
        File unFile = null;
        // tar compress format
        ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
        try (
                FileInputStream inputStream = new FileInputStream(tarZipSource);
                BufferedInputStream bufInput = new BufferedInputStream(inputStream);
                GZIPInputStream gzipInput = new GZIPInputStream(bufInput);
                ArchiveInputStream archiveInput = archiveStreamFactory.createArchiveInputStream("tar", gzipInput)) {

            TarArchiveEntry entry = (TarArchiveEntry) archiveInput.getNextEntry();

            while (entry != null) {
                String entryName = entry.getName();

                if (entry.isDirectory()) {
                    createDir(targetDir, entryName, 1);
                    if (unFile == null) {
                        unFile = new File(targetDir + entryName.replaceAll("/.*$", ""));
                    }
                } else if (entry.isFile()) {
                    String fullFileName = createDir(targetDir, entryName, 2);
                    try (
                            FileOutputStream outputStream = new FileOutputStream(fullFileName);
                            BufferedOutputStream bufOutput = new BufferedOutputStream(outputStream)) {
                        int b = -1;
                        while ((b = archiveInput.read()) != -1) {
                            bufOutput.write(b);
                        }
                    }
                }
                entry = (TarArchiveEntry) archiveInput.getNextEntry();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @param baseDir baseDir
     * @param entry archive entry
     * @param type type: 1, dir; 2, file
     * @return
     */
    private static String createDir(String baseDir, String entry, int type) {
        String[] items = entry.split("/");
        String fullFilePath = baseDir;
        for (int i = 0; i < items.length; i++) {
            String item = items[i];
            fullFilePath = fullFilePath + File.separator + item;
            if (type == 2) {
                if (i != items.length - 1) {
                    File tmpFile = new File(fullFilePath);
                    if (!tmpFile.exists()) {
                        tmpFile.mkdir();
                    }
                }
            } else {
                File tmpFile = new File(fullFilePath);
                if (!tmpFile.exists()) {
                    tmpFile.mkdir();
                }
            }
        }
        File fullFile = new File(fullFilePath);
        return fullFile.getAbsolutePath();
    }
}
