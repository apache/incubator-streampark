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

import com.streamxhub.streamx.common.fs.CosOperator;

import com.amazonaws.SdkClientException;
import com.qcloud.cos.model.COSObjectInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public class CosStorage implements StorageService {

    private static final Logger LOG = LoggerFactory.getLogger(CosStorage.class);

    private final CosOperator operator;

    public CosStorage(Properties properties) {
        this.operator = CosOperator.apply(properties);
    }

    @Override
    public Optional<byte[]> getData(String objectPath) {
        Objects.requireNonNull(objectPath);
        try (COSObjectInputStream s3is = (COSObjectInputStream) operator.getObject(objectPath)) {
            return Optional.of(IOUtils.toByteArray(s3is));
        } catch (IOException e) {
            LOG.error("Failed to get data from path: {}", objectPath);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean putData(String objectPath, byte[] data) {
        Objects.requireNonNull(objectPath);
        Objects.requireNonNull(data);
        try {
            operator.putObject(objectPath, data);
        } catch (SdkClientException e) {
            LOG.error("Failed to put data to path: {}", objectPath, e);
            return false;
        } finally {
            if (operator != null) {
                operator.close();
            }
        }
        return true;
    }
}
