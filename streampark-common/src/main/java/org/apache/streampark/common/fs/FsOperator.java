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

import org.apache.streampark.common.enums.StorageType;

public abstract class FsOperator {

    private static volatile FsOperator lfsInstance;
    private static volatile FsOperator hdfsInstance;

    public static FsOperator lfs() {
        if (lfsInstance == null) {
            synchronized (FsOperator.class) {
                if (lfsInstance == null) {
                    lfsInstance = of(StorageType.LFS);
                }
            }
        }
        return lfsInstance;
    }

    public static FsOperator hdfs() {
        if (hdfsInstance == null) {
            synchronized (FsOperator.class) {
                if (hdfsInstance == null) {
                    hdfsInstance = of(StorageType.HDFS);
                }
            }
        }
        return hdfsInstance;
    }

    public static FsOperator of(StorageType storageType) {
        switch (storageType) {
            case HDFS:
                return HdfsOperator.getInstance();
            case LFS:
                return LfsOperator.getInstance();
            default:
                throw new UnsupportedOperationException("Unsupported storageType:" + storageType);
        }
    }

    public abstract boolean exists(String path);

    public abstract void mkdirs(String path);

    public void mkdirsIfNotExists(String path) {
        if (!exists(path)) {
            mkdirs(path);
        }
    }

    public abstract void delete(String path);

    public abstract void mkCleanDirs(String path);

    public void upload(String srcPath, String dstPath) {
        upload(srcPath, dstPath, false, true);
    }

    public void copy(String srcPath, String dstPath) {
        copy(srcPath, dstPath, false, true);
    }

    public void copyDir(String srcPath, String dstPath) {
        copyDir(srcPath, dstPath, false, true);
    }

    public abstract void upload(String srcPath, String dstPath, boolean delSrc, boolean overwrite);

    public abstract void copy(String srcPath, String dstPath, boolean delSrc, boolean overwrite);

    public abstract void copyDir(String srcPath, String dstPath, boolean delSrc, boolean overwrite);

    public abstract void move(String srcPath, String dstPath);

    public abstract String fileMd5(String path);
}
