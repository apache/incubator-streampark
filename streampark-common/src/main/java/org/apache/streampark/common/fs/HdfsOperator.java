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

import org.apache.streampark.common.util.HdfsUtils;

import java.io.IOException;

/** Hadoop File System (aka HDFS) Operator */
public final class HdfsOperator extends FsOperator {

    private static final HdfsOperator INSTANCE = new HdfsOperator();

    private HdfsOperator() {}

    public static HdfsOperator getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean exists(String path) {
        try {
            return HdfsUtils.exists(toHdfsPath(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void mkdirs(String path) {
        try {
            HdfsUtils.mkdirs(toHdfsPath(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            HdfsUtils.delete(toHdfsPath(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void move(String srcPath, String dstPath) {
        try {
            HdfsUtils.move(toHdfsPath(srcPath), toHdfsPath(dstPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void upload(String srcPath, String dstPath, boolean delSrc, boolean overwrite) {
        try {
            HdfsUtils.upload(srcPath, toHdfsPath(dstPath), delSrc, overwrite);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void copy(String srcPath, String dstPath, boolean delSrc, boolean overwrite) {
        try {
            HdfsUtils.copyHdfs(toHdfsPath(srcPath), toHdfsPath(dstPath), delSrc, overwrite);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void copyDir(String srcPath, String dstPath, boolean delSrc, boolean overwrite) {
        try {
            HdfsUtils.copyHdfsDir(toHdfsPath(srcPath), toHdfsPath(dstPath), delSrc, overwrite);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void mkCleanDirs(String path) {
        delete(path);
        mkdirs(path);
    }

    @Override
    public String fileMd5(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("[StreamPark] HdfsOperator.fileMd5: file must not be null.");
        }
        try {
            return HdfsUtils.fileMd5(toHdfsPath(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String toHdfsPath(String path) {
        if (path.startsWith("hdfs://")) {
            return path;
        }
        return HdfsUtils.getDefaultFS().concat(path);
    }
}
