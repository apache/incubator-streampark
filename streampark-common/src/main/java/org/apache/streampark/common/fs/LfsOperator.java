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

import org.apache.streampark.common.util.Utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/** Local File System (aka LFS) Operator */
public final class LfsOperator {

    private LfsOperator() {
    }

    public static boolean exists(String path) {
        return StringUtils.isNotBlank(path) && new File(path).exists();
    }

    public static void mkdirs(String path) {
        if (!Utils.isAnyBank(path)) {
            try {
                FileUtils.forceMkdir(new File(path));
            } catch (IOException e) {
                throw new IllegalStateException("[StreamPark] Failed to mkdirs: " + path, e);
            }
        }
    }

    public static void delete(String path) {
        if (Utils.isNotEmpty(path)) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    FileUtils.forceDelete(file);
                } catch (IOException e) {
                    throw new IllegalStateException("[StreamPark] Failed to delete: " + path, e);
                }
            }
        }
    }

    public static void move(String srcPath, String dstPath) {
        if (Utils.isAnyBank(srcPath, dstPath)) {
            return;
        }
        File srcFile = new File(srcPath);
        File dstFile = new File(dstPath);
        if (!srcFile.exists()) {
            return;
        }
        try {
            if (srcFile.getCanonicalPath().equals(dstFile.getCanonicalPath())) {
                return;
            }
            FileUtils.moveToDirectory(srcFile, dstFile, true);
        } catch (IOException e) {
            throw new IllegalStateException(
                "[StreamPark] Failed to move " + srcPath + " to " + dstPath, e);
        }
    }

    public static void upload(String srcPath, String dstPath) {
        upload(srcPath, dstPath, false, true);
    }

    public static void upload(String srcPath, String dstPath, boolean delSrc) {
        upload(srcPath, dstPath, delSrc, true);
    }

    public static void upload(String srcPath, String dstPath, boolean delSrc, boolean overwrite) {
        if (new File(srcPath).isDirectory()) {
            copyDir(srcPath, dstPath, delSrc, overwrite);
        } else {
            copy(srcPath, dstPath, delSrc, overwrite);
        }
    }

    /**
     * When the suffixes of srcPath and dstPath are the same, or the file names are the same, copy to
     * the file, otherwise copy to the directory.
     */
    public static void copy(String srcPath, String dstPath) {
        copy(srcPath, dstPath, false, true);
    }

    public static void copy(String srcPath, String dstPath, boolean delSrc) {
        copy(srcPath, dstPath, delSrc, true);
    }

    public static void copy(String srcPath, String dstPath, boolean delSrc, boolean overwrite) {
        if (Utils.isAnyBank(srcPath, dstPath)) {
            return;
        }
        File srcFile = new File(srcPath);
        if (!srcFile.exists()) {
            return;
        }
        if (!srcFile.isFile()) {
            throw new IllegalArgumentException("[StreamPark] " + srcPath + " must be a file.");
        }
        File dstFile = resolveCopyDestination(srcFile, dstPath);
        try {
            if (srcFile.getCanonicalPath().equals(dstFile.getCanonicalPath())) {
                throw new IllegalArgumentException(
                    "[StreamPark] src and dst must not be the same path: " + srcPath);
            }
        } catch (IOException e) {
            throw new IllegalStateException("[StreamPark] Failed to resolve copy paths", e);
        }
        boolean shouldCopy =
            overwrite || !dstFile.exists() || !dstFile.getName().equals(srcFile.getName());
        if (shouldCopy) {
            try {
                FileUtils.copyFile(srcFile, dstFile);
                if (delSrc) {
                    FileUtils.forceDelete(srcFile);
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                    "[StreamPark] Failed to copy " + srcPath + " to " + dstPath, e);
            }
        }
    }

    public static void copyDir(String srcPath, String dstPath) {
        copyDir(srcPath, dstPath, false, true);
    }

    public static void copyDir(String srcPath, String dstPath, boolean delSrc) {
        copyDir(srcPath, dstPath, delSrc, true);
    }

    public static void copyDir(String srcPath, String dstPath, boolean delSrc, boolean overwrite) {
        if (Utils.isAnyBank(srcPath, dstPath)) {
            return;
        }
        File srcFile = new File(srcPath);
        if (!srcFile.exists()) {
            return;
        }
        if (!srcFile.isDirectory()) {
            throw new IllegalArgumentException("[StreamPark] " + srcPath + " must be a directory.");
        }
        File dstFile = new File(dstPath);
        boolean shouldCopy;
        try {
            shouldCopy = overwrite || !dstFile.exists()
                || !srcFile.getCanonicalPath().equals(dstFile.getCanonicalPath());
        } catch (IOException e) {
            throw new IllegalStateException("[StreamPark] Failed to resolve copyDir paths", e);
        }
        if (shouldCopy) {
            try {
                FileUtils.copyDirectory(srcFile, dstFile);
                if (delSrc) {
                    FileUtils.deleteDirectory(srcFile);
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                    "[StreamPark] Failed to copyDir " + srcPath + " to " + dstPath, e);
            }
        }
    }

    public static String fileMd5(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("[StreamPark] LFsOperator.fileMd5: file must not be null.");
        }
        File file = new File(path);
        if (!file.exists()) {
            throw new IllegalArgumentException("[StreamPark] LFsOperator.fileMd5: file must exists.");
        }
        try (FileInputStream inputStream = new FileInputStream(path)) {
            return DigestUtils.md5Hex(IOUtils.toByteArray(inputStream));
        } catch (IOException e) {
            throw new IllegalStateException("[StreamPark] Failed to compute md5 for: " + path, e);
        }
    }

    /** Force delete directory and recreate it. */
    public static void mkCleanDirs(String path) {
        delete(path);
        mkdirs(path);
    }

    /** list file under directory, one level of traversal only */
    public static File[] listDir(String path) {
        if (path == null || path.trim().isEmpty()) {
            return new File[0];
        }
        File file = new File(path);
        if (!file.exists()) {
            return new File[0];
        }
        if (file.isFile()) {
            return new File[]{file};
        }
        File[] files = file.listFiles();
        return files != null ? files : new File[0];
    }

    private static File resolveCopyDestination(File srcFile, String dstPath) {
        File dstFile = new File(dstPath);
        if (dstFile.exists()) {
            if (dstFile.isDirectory()) {
                return new File(dstFile, srcFile.getName());
            }
            return dstFile;
        }
        if (!dstFile.getParentFile().exists()) {
            throw new IllegalArgumentException(
                "[StreamPark] dstPath is invalid and does not exist. Please check");
        }
        return dstFile;
    }
}
