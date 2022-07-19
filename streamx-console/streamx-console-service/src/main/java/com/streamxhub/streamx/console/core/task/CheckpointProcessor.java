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

import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.SavePoint;
import com.streamxhub.streamx.console.core.enums.CheckPointStatus;
import com.streamxhub.streamx.console.core.metrics.flink.CheckPoints;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.console.core.service.SavePointService;
import com.streamxhub.streamx.console.core.service.alert.AlertService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CheckpointProcessor {

    private final Map<Long, Long> checkPointCache = new ConcurrentHashMap<>(0);

    private final Map<Long, Counter> checkPointFailedCache = new ConcurrentHashMap<>(0);

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private SavePointService savePointService;

    public void process(Long appId, CheckPoints checkPoints) {
        CheckPoints.Latest latest = checkPoints.getLatest();
        if (latest != null) {
            Application application = applicationService.getById(appId);
            CheckPoints.CheckPoint checkPoint = latest.getCompleted();
            if (checkPoint != null) {
                CheckPointStatus status = checkPoint.getCheckPointStatus();
                if (CheckPointStatus.COMPLETED.equals(status)) {
                    Long latestId = checkPointCache.get(appId);
                    if (latestId == null) {
                        SavePoint savePoint = savePointService.getLatest(appId);
                        if (savePoint != null) {
                            latestId = savePoint.getId();
                        }
                    }
                    if (latestId == null || latestId < checkPoint.getId()) {
                        SavePoint savePoint = new SavePoint();
                        savePoint.setAppId(application.getId());
                        savePoint.setLatest(true);
                        savePoint.setType(checkPoint.getCheckPointType().get());
                        savePoint.setPath(checkPoint.getExternalPath());
                        savePoint.setTriggerTime(new Date(checkPoint.getTriggerTimestamp()));
                        savePoint.setCreateTime(new Date());
                        savePointService.save(savePoint);
                        checkPointCache.put(application.getId(), checkPoint.getId());
                    }
                } else if (CheckPointStatus.FAILED.equals(status) && application.cpFailedTrigger()) {
                    Counter counter = checkPointFailedCache.get(appId);
                    if (counter == null) {
                        checkPointFailedCache.put(appId, new Counter(checkPoint.getTriggerTimestamp()));
                    } else {
                        //x分钟之内超过Y次CheckPoint失败触发动作
                        long minute = counter.getDuration(checkPoint.getTriggerTimestamp());
                        if (minute <= application.getCpFailureRateInterval()
                            && counter.count >= application.getCpMaxFailureInterval()) {
                            checkPointFailedCache.remove(appId);
                            if (application.getCpFailureAction() == 1) {
                                alertService.alert(application, CheckPointStatus.FAILED);
                            } else {
                                try {
                                    applicationService.restart(application);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        } else {
                            counter.add();
                        }
                    }
                }
            }
        }
    }

    public static class Counter {
        private Long timestamp;
        private Integer count;

        public Counter(Long timestamp) {
            this.timestamp = timestamp;
            this.count = 1;
        }

        public void add() {
            this.count += 1;
        }

        public Integer getCount() {
            return count;
        }

        public long getDuration(Long currentTimestamp) {
            return (currentTimestamp - this.timestamp) / 1000 / 60;
        }
    }

}


