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

package com.streamxhub.streamx.console.core.service.impl;

import static com.streamxhub.streamx.common.enums.StorageType.LFS;

import com.streamxhub.streamx.common.conf.Workspace;
import com.streamxhub.streamx.common.enums.StorageType;
import com.streamxhub.streamx.common.fs.LfsOperator;
import com.streamxhub.streamx.console.core.service.ApplicationHistoryService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Al-assad
 */
@Slf4j
@Service
public class ApplicationHistoryServiceImpl implements ApplicationHistoryService {

    @Override
    public List<String> listUploadJars(StorageType storageType, int limit) {
        switch (storageType) {
            case LFS:
                return Arrays.stream(LfsOperator.listDir(Workspace.of(LFS).APP_UPLOADS()))
                    .filter(File::isFile)
                    .sorted(Comparator.comparingLong(File::lastModified).reversed())
                    .map(File::getName)
                    .filter(fn -> fn.endsWith(".jar"))
                    .limit(limit)
                    .collect(Collectors.toList());
            case HDFS:
                // temporarily does not provide support for hdfs.
            default:
                return new ArrayList<>(0);
        }
    }

}
