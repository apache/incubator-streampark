/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.console.base.util;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * @author benjobs
 */
public class GZipUtils {
    /**
     * @param tarZipSource 源文件
     * @param targetDir    目标目录
     */
    public static File decompress(String tarZipSource, String targetDir) {
        FileInputStream inputStream = null;
        BufferedInputStream bufInput = null;
        ArchiveInputStream archiveInput = null;
        File unFile = null;
        try {
            // 文件流
            inputStream = new FileInputStream(tarZipSource);
            // 缓冲流
            bufInput = new BufferedInputStream(inputStream);
            // GZIP压缩流
            GZIPInputStream gzipInput = new GZIPInputStream(bufInput);
            // tar压缩格式（tar类型）
            ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
            archiveInput = archiveStreamFactory.createArchiveInputStream("tar", gzipInput);
            // tar压缩文件条目
            TarArchiveEntry entry = (TarArchiveEntry) archiveInput.getNextEntry();

            while (entry != null) {
                // 条目名称
                String entryName = entry.getName();

                if (entry.isDirectory()) {
                    // 如果当前条目是目录
                    createDir(targetDir, entryName, 1);
                    if (unFile == null) {
                        unFile = new File(targetDir + entryName.replaceAll("/.*$", ""));
                    }
                } else if (entry.isFile()) {
                    // 如果当前条目是文件
                    String fullFileName = createDir(targetDir, entryName, 2);
                    // 输出文件
                    FileOutputStream outputStream = new FileOutputStream(fullFileName);
                    BufferedOutputStream bufOutput = new BufferedOutputStream(outputStream);
                    int b = -1;
                    while ((b = archiveInput.read()) != -1) {
                        bufOutput.write(b);
                    }
                    bufOutput.flush();
                    bufOutput.close();
                }
                // 下一个条目
                entry = (TarArchiveEntry) archiveInput.getNextEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(archiveInput);
            close(bufInput);
            close(inputStream);
        }

        return unFile;
    }

    /**
     * @param input
     */
    private static void close(InputStream input) {
        // 静默关闭处理
        if (null != input) {
            try {
                input.close();
            } catch (IOException ignored) {

            }
        }
    }

    /**
     * @param baseDir 根目录
     * @param entry   压缩包条目
     * @param type    类型：1、目录；2、文件
     * @return
     */
    private static String createDir(String baseDir, String entry, int type) {
        // 拆分名称
        String[] items = entry.split("/");
        String fullFilePath = baseDir;
        for (int i = 0; i < items.length; i++) {
            String item = items[i];
            fullFilePath = fullFilePath + File.separator + item;
            if (type == 2) {
                if (i != items.length - 1) {
                    // 如果目录不存在，就创建
                    File tmpFile = new File(fullFilePath);
                    if (!tmpFile.exists()) {
                        tmpFile.mkdir();
                    }
                }
            } else {
                // 如果目录不存在，就创建
                File tmpFile = new File(fullFilePath);
                if (!tmpFile.exists()) {
                    tmpFile.mkdir();
                }
            }
        }
        // 返回目录全路径
        File fullFile = new File(fullFilePath);
        return fullFile.getAbsolutePath();
    }
}
