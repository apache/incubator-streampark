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

package org.apache.streampark.console.core.managed.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled entrypoint for database-leased managed Flink job synchronization. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "streampark.managed-flink.watcher", name = "enabled", havingValue = "true")
public class ManagedFlinkJobWatcher {

    private final ManagedFlinkFeatureGate featureGate;
    private final ManagedFlinkJobSyncService syncService;

    @Scheduled(fixedDelayString = "${streampark.managed-flink.watcher.scheduler-delay-ms:1000}", initialDelayString = "${streampark.managed-flink.watcher.scheduler-initial-delay-ms:5000}")
    public void synchronize() {
        if (!featureGate.isEnabled()) {
            return;
        }
        try {
            syncService.synchronizeDue();
        } catch (Exception exception) {
            log.warn(
                "[StreamPark][ManagedFlinkJobWatcher] synchronization batch failed, type={}",
                exception.getClass().getSimpleName());
        }
    }
}
