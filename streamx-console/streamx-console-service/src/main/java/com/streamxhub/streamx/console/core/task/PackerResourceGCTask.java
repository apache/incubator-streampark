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

package com.streamxhub.streamx.console.core.task;

import com.streamxhub.streamx.flink.packer.PackerResourceGC;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * author: Al-assad
 */
@Slf4j
@Component
public class PackerResourceGCTask {

    @Value("${streamx.packer-gc.max-resource-expired-hours:120}")
    public Integer maxResourceIntervalHours;

    @Scheduled(cron = "${streamx.packer-gc.exec-cron:0 0 0/6 * * ?}")
    public void collectGarbage() {
        log.info("[streamx-packer] Starting Packer Resource GC Task.");
        PackerResourceGC.startGc(maxResourceIntervalHours);
    }

}
