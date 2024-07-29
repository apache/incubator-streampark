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
package org.apache.streampark.console.core.task;

import org.apache.streampark.flink.packer.PackerResourceGC;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PackerResourceGCTask {

    @Value("${streampark.packer-gc.max-resource-expired-hours:120}")
    public Integer maxResourceIntervalHours;

    @Scheduled(cron = "${streampark.packer-gc.exec-cron:0 0 0/6 * * ?}")
    public void collectGarbage() {
        log.info("[streampark-packer] Starting Packer Resource GC Task.");
        PackerResourceGC.startGc(maxResourceIntervalHours);
    }
}
