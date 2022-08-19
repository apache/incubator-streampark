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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class LocalFsStorage implements StorageService{
    private static final Logger LOG = LoggerFactory.getLogger(LocalFsStorage.class);
    private final URI baseUri;
    private final Path basePath;

    public LocalFsStorage(URI baseUri, String storageGroup) {
        this.baseUri = Objects.requireNonNull(baseUri);
        if (!baseUri.getScheme().equals("file")) {
            throw new IllegalArgumentException("Expected scheme file://");
        } else {
            this.basePath = StorageUtils.getBasePath(baseUri, storageGroup).resolve(storageGroup);
        }
    }

    @Override
    public Optional<byte[]> getData(String fileName) {
        Path filePath = basePath.resolve(fileName);
        if (!Files.isRegularFile(filePath)) {
            return Optional.empty();
        } else {
            try {
                return Optional.of(Files.readAllBytes(filePath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    public Boolean putData(String fileName, byte[] data) {
        Objects.requireNonNull(fileName);
        Objects.requireNonNull(data);
        Path filePath = StorageUtils.getFileNamePath(this.basePath, fileName);
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, data);
        } catch (IOException e) {
            LOG.error("Failed to create storage directory or failed to write file", e);
            return false;
        }
        return true;
    }
}
