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

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.ipc.RPC;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

public final class HdfsUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(HdfsUtils.class.getName());

    private HdfsUtils() {
    }

    public static String getDefaultFS() {
        return HadoopUtils.hadoopConf().get(FileSystem.FS_DEFAULT_NAME_KEY);
    }

    public static List<FileStatus> list(String src) throws IOException {
        return Arrays.asList(HadoopUtils.hdfs().listStatus(getPath(src)));
    }

    public static void move(String src, String dst) throws IOException {
        HadoopUtils.hdfs().rename(getPath(src), getPath(dst));
    }

    public static void mkdirs(String path) throws IOException {
        HadoopUtils.hdfs().mkdirs(getPath(path));
    }

    public static void copyHdfs(String src, String dst) throws IOException {
        copyHdfs(src, dst, false, true);
    }

    public static void copyHdfs(String src, String dst, boolean delSrc, boolean overwrite) throws IOException {
        Path srcPath = getPath(src);
        Path dstPath = getPath(dst);
        FileStatus dstStatus = HadoopUtils.hdfs().getFileStatus(dstPath);
        Path dstFinalPath =
            dstStatus.isFile() ? dstPath : getPath(dst + "/" + srcPath.getName());
        FileUtil.copy(
            HadoopUtils.hdfs(),
            srcPath,
            HadoopUtils.hdfs(),
            dstFinalPath,
            delSrc,
            overwrite,
            HadoopUtils.hadoopConf());
    }

    public static void copyHdfsDir(String src, String dst) throws IOException {
        copyHdfsDir(src, dst, false, true);
    }

    public static void copyHdfsDir(String src, String dst, boolean delSrc, boolean overwrite) throws IOException {
        for (FileStatus status : list(src)) {
            FileUtil.copy(
                HadoopUtils.hdfs(),
                status,
                HadoopUtils.hdfs(),
                getPath(dst),
                delSrc,
                overwrite,
                HadoopUtils.hadoopConf());
        }
    }

    public static void upload(String src, String dst) throws IOException {
        upload(src, dst, false, true);
    }

    public static void upload(String src, String dst, boolean delSrc, boolean overwrite) throws IOException {
        HadoopUtils.hdfs().copyFromLocalFile(delSrc, overwrite, getPath(src), getPath(dst));
    }

    public static void uploadMulti(String[] src, String dst) throws IOException {
        uploadMulti(src, dst, false, true);
    }

    public static void uploadMulti(String[] src, String dst, boolean delSrc, boolean overwrite) throws IOException {
        Path[] paths = Arrays.stream(src).map(HdfsUtils::getPath).toArray(Path[]::new);
        HadoopUtils.hdfs().copyFromLocalFile(delSrc, overwrite, paths, getPath(dst));
    }

    public static void download(String src, String dst) throws IOException {
        download(src, dst, false, false);
    }

    public static void download(String src, String dst, boolean delSrc,
                                boolean useRawLocalFileSystem) throws IOException {
        HadoopUtils.hdfs()
            .copyToLocalFile(delSrc, getPath(src), getPath(dst), useRawLocalFileSystem);
    }

    public static String getNameNode() throws IOException {
        return getAddressOfActive(HadoopUtils.hdfs()).getHostString();
    }

    public static void create(String fileName, String content) throws IOException {
        Path path = getPath(fileName);
        if (!HadoopUtils.hdfs().exists(path)) {
            throw new IllegalArgumentException("[StreamPark] HdfsUtils.create " + fileName + " is exists!! ");
        }
        FSDataOutputStream outputStream = HadoopUtils.hdfs().create(path);
        outputStream.writeUTF(content);
        outputStream.flush();
        outputStream.close();
    }

    public static boolean exists(String path) throws IOException {
        return HadoopUtils.hdfs().exists(getPath(path));
    }

    public static String read(String fileName) throws IOException {
        Path path = getPath(fileName);
        if (!HadoopUtils.hdfs().exists(path) || HadoopUtils.hdfs().isDirectory(path)) {
            throw new IllegalArgumentException(
                "[StreamPark] HdfsUtils.read: path(" + fileName + ") not exists or isDirectory ");
        }
        FSDataInputStream in = HadoopUtils.hdfs().open(path);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copyBytes(in, out, 4096, false);
        out.flush();
        IOUtils.closeStream(in);
        IOUtils.closeStream(out);
        return new String(out.toByteArray());
    }

    public static void delete(String src) throws IOException {
        Path path = getPath(src);
        if (HadoopUtils.hdfs().exists(path)) {
            HadoopUtils.hdfs().delete(path, true);
        } else {
            LOG.warn("[StreamPark] HDFS delete {}, but file {} is not exists!", src, src);
        }
    }

    public static String fileMd5(String fileName) throws IOException {
        Path path = getPath(fileName);
        FSDataInputStream in = HadoopUtils.hdfs().open(path);
        try {
            // MD5 is used for non-cryptographic file integrity checks only.
            @SuppressWarnings("java:S4790")
            String digest = DigestUtils.md5Hex(in);
            return digest;
        } finally {
            in.close();
        }
    }

    public static void downToLocal(String hdfsPath, String localPath) throws IOException {
        Path path = getPath(hdfsPath);
        FSDataInputStream input = HadoopUtils.hdfs().open(path);
        String content = input.readUTF();
        FileWriter fw = new FileWriter(localPath);
        fw.write(content);
        fw.close();
        input.close();
    }

    private static Path getPath(String hdfsPath) {
        return new Path(hdfsPath);
    }

    public static InetSocketAddress getAddressOfActive(FileSystem fs) throws IOException {
        if (!(fs instanceof DistributedFileSystem)) {
            throw new IllegalArgumentException("FileSystem " + fs + " is not a DFS.");
        }
        fs.exists(new Path("/"));
        DistributedFileSystem dfs = (DistributedFileSystem) fs;
        return RPC.getServerAddress(dfs.getClient().getNamenode());
    }
}
