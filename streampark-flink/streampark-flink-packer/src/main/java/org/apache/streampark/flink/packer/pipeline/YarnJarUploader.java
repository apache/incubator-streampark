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

package org.apache.streampark.flink.packer.pipeline;

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.AutoCloseUtils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/** Uploads application jars to local or HDFS Yarn-provided paths. */
public final class YarnJarUploader {

    private YarnJarUploader() {
    }

    public static void uploadJarToHdfsOrLfs(FsOperator fsOperator, String origin, String target) throws IOException {
        File originFile = new File(origin);
        if (!fsOperator.exists(target)) {
            fsOperator.mkdirs(target);
        }
        if (originFile.isFile()) {
            uploadFile(fsOperator, originFile, target);
        } else if (fsOperator == FsOperator.hdfs()) {
            fsOperator.upload(originFile.getAbsolutePath(), target);
        }
    }

    private static void uploadFile(FsOperator fsOperator, File originFile, String target) throws IOException {
        if (fsOperator == FsOperator.lfs()) {
            fsOperator.copy(originFile.getAbsolutePath(), target);
            return;
        }
        String uploadFile = Workspace.remote().APP_UPLOADS() + "/" + originFile.getName();
        if (fsOperator.exists(uploadFile)) {
            AutoCloseUtils.using(
                new FileInputStream(originFile),
                inputStream -> {
                    try {
                        if (!DigestUtils.md5Hex(inputStream).equals(fsOperator.fileMd5(uploadFile))) {
                            fsOperator.upload(originFile.getAbsolutePath(), uploadFile);
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                    return null;
                });
        } else {
            fsOperator.upload(originFile.getAbsolutePath(), uploadFile);
        }
        fsOperator.copy(uploadFile, target);
    }
}
