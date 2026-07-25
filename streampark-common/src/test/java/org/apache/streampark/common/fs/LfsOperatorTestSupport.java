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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Random;
import java.util.UUID;

final class LfsOperatorTestSupport {

    private LfsOperatorTestSupport() {
    }

    static File genRandomFile(String dir) {
        return genRandomFile(dir, UUID.randomUUID().toString() + ".dat", 256);
    }

    static File genRandomFile(String dir, String name) {
        return genRandomFile(dir, name, 256);
    }

    static File genRandomFile(String dir, String name, int size) {
        Random random = new Random();
        byte[] content = new byte[size];
        random.nextBytes(content);
        File file = new File(dir, name);
        try {
            FileUtils.writeByteArrayToFile(file, content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    static DirWithFiles genRandomDir(String dirPath) {
        return genRandomDir(dirPath, 5);
    }

    static DirWithFiles genRandomDir(String dirPath, int childFileCount) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File[] children = new File[childFileCount];
        for (int i = 0; i < childFileCount; i++) {
            children[i] = genRandomFile(dirPath);
        }
        return new DirWithFiles(dir, children);
    }

    static final class DirWithFiles {

        private final File dir;
        private final File[] files;

        DirWithFiles(File dir, File[] files) {
            this.dir = dir;
            this.files = files;
        }

        File dir() {
            return dir;
        }

        File[] files() {
            return files;
        }
    }
}
