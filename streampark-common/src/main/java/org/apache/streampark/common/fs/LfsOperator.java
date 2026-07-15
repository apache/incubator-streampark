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

/** Local File System (aka LFS) Operator */
public final class LfsOperator extends FsOperator {

    private static final LfsOperator INSTANCE = new LfsOperator();

    private LfsOperator() {
    }

    public static LfsOperator getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean exists(String path) {
        return StringUtils.isNotBlank(path) && new File(path).exists();
    }

    @Override
    public void mkdirs(String path) {
        if (!Utils.isAnyBank(path)) {
            try {
                FileUtils.forceMkdir(new File(path));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void delete(String path) {
        if (Utils.isNotEmpty(path)) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    FileUtils.forceDelete(file);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void move(String srcPath, String dstPath) {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void upload(String srcPath, String dstPath, boolean delSrc, boolean overwrite) {
        if (new File(srcPath).isDirectory()) {
            copyDir(srcPath, dstPath, delSrc, overwrite);
        } else {
            copy(srcPath, dstPath, delSrc, overwrite);
        }
    }

    @Override
    public void copy(String srcPath, String dstPath, boolean delSrc, boolean overwrite) {
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
        File dstFile;
        File dstCandidate = new File(dstPath);
        if (dstCandidate.exists()) {
            dstFile = dstCandidate.isDirectory() ? new File(dstCandidate, srcFile.getName()) : dstCandidate;
        } else {
            if (!dstCandidate.getParentFile().exists()) {
                throw new IllegalArgumentException(
                    "[StreamPark] dstPath is invalid and does not exist. Please check");
            }
            dstFile = dstCandidate;
        }
        try {
            if (srcFile.getCanonicalPath().equals(dstFile.getCanonicalPath())) {
                return;
            }
            boolean shouldCopy =
                overwrite || !dstFile.exists() || !dstFile.getName().equals(srcFile.getName());
            if (shouldCopy) {
                FileUtils.copyFile(srcFile, dstFile);
                if (delSrc) {
                    FileUtils.forceDelete(srcFile);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void copyDir(String srcPath, String dstPath, boolean delSrc, boolean overwrite) {
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
        try {
            boolean shouldCopy =
                overwrite
                    || !dstFile.exists()
                    || !srcFile.getCanonicalPath().equals(dstFile.getCanonicalPath());
            if (shouldCopy) {
                FileUtils.copyDirectory(srcFile, dstFile);
                if (delSrc) {
                    FileUtils.deleteDirectory(srcFile);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String fileMd5(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("[StreamPark] LFsOperator.fileMd5: file must not be null.");
        }
        File file = new File(path);
        if (!file.exists()) {
            throw new IllegalArgumentException("[StreamPark] LFsOperator.fileMd5: file must exists.");
        }
        try {
            // MD5 is used for non-cryptographic file integrity checks only.
            @SuppressWarnings("java:S4790")
            String digest = DigestUtils.md5Hex(IOUtils.toByteArray(new FileInputStream(path)));
            return digest;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void mkCleanDirs(String path) {
        delete(path);
        mkdirs(path);
    }

    /** list file under directory, one level of traversal only */
    public File[] listDir(String path) {
        if (path == null || path.trim().isEmpty()) {
            return new File[0];
        }
        File f = new File(path);
        if (!f.exists()) {
            return new File[0];
        }
        if (f.isFile()) {
            return new File[]{f};
        }
        File[] files = f.listFiles();
        return files != null ? files : new File[0];
    }
}
