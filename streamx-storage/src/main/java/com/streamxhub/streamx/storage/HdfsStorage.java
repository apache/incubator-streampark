/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.storage;

import static org.apache.hadoop.io.IOUtils.copyBytes;

import com.streamxhub.streamx.common.util.HadoopUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class HdfsStorage implements StorageService{
    private static final Logger LOG = LoggerFactory.getLogger(HdfsStorage.class);
    private final String bucketPath;
    private final FileSystem client;

    public HdfsStorage(URI baseUri, String storageGroup) {
        Objects.requireNonNull(baseUri);
        if (!baseUri.getScheme().equals("hdfs")) {
            throw new IllegalArgumentException("Expected scheme hdfs://, but was: " + baseUri);
        } else {
            this.bucketPath = String.format("%s/%s", StringUtils.removeEnd(baseUri.toString(), "/"), storageGroup);
            this.client = HadoopUtils.hdfs();

        }
    }

    @Override
    public Optional<byte[]> getData(String fileName) {
        Objects.requireNonNull(fileName);
        Path path = new Path(StorageUtils.getFullBucketPath(bucketPath, fileName));
        try (FSDataInputStream inputStream = this.client.open(path)){
            Optional<byte[]> data;
            data = Optional.of(IOUtils.toByteArray(inputStream));
            return data;
        } catch (FileNotFoundException e) {
            LOG.debug("No file at path: {}", path);
            return Optional.empty();
        } catch (IOException e) {
            LOG.error("Failed to get data from path: {}", path, e);
            return Optional.empty();
        }
    }

    @Override
    public Boolean putData(String fileName, byte[] data) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(fileName);
        StorageUtils.validateName(fileName);
        Path path = new Path(StorageUtils.getFullBucketPath(bucketPath, fileName));
        try (FSDataOutputStream fsDataOutputStream = this.client.create(path)){
            copyBytes(new ByteArrayInputStream(data), fsDataOutputStream, this.client.getConf());
        } catch (IOException e) {
            LOG.error("Failed to put data to path: {}", path, e);
            return false;
        }
        return true;
    }
}
