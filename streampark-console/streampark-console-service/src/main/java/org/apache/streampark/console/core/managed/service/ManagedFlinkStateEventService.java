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

import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.entity.ManagedFlinkStateEvent;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.mapper.FlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkApplicationMapper;
import org.apache.streampark.console.core.mapper.ManagedFlinkStateEventMapper;
import org.apache.streampark.console.core.service.alert.AlertService;
import org.apache.streampark.console.core.util.AlertTemplateUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Persists deduplicated state transitions and dispatches alerts outside watcher transactions. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkStateEventService {

    private static final Set<String> PROBLEM_STATES =
        new HashSet<>(Arrays.asList("FAILED", "LOST", "SUSPENDED"));

    private final ManagedFlinkStateEventMapper eventMapper;
    private final FlinkApplicationMapper applicationMapper;
    private final ManagedFlinkApplicationMapper managedApplicationMapper;
    private final AlertService alertService;

    Long create(
                Long appId,
                String fromState,
                String toState,
                String externalInstanceId,
                Date now) {
        if (fromState == null || toState == null || fromState.equals(toState)) {
            return null;
        }
        String eventKey =
            hash(
                appId
                    + "|"
                    + fromState
                    + "->"
                    + toState
                    + "|"
                    + defaultValue(externalInstanceId, "-"));
        ManagedFlinkStateEvent existing =
            eventMapper.selectOne(
                new LambdaQueryWrapper<ManagedFlinkStateEvent>()
                    .eq(ManagedFlinkStateEvent::getEventKey, eventKey));
        if (existing != null) {
            return existing.getId();
        }
        ManagedFlinkStateEvent event = new ManagedFlinkStateEvent();
        event.setAppId(appId);
        event.setEventKey(eventKey);
        event.setFromState(fromState);
        event.setToState(toState);
        event.setExternalInstanceId(externalInstanceId);
        event.setAlertState(
            shouldAlert(fromState, toState) ? "PENDING" : "NOT_REQUIRED");
        event.setAlertAttempts(0);
        event.setCreateTime(now);
        event.setModifyTime(now);
        eventMapper.insert(event);
        return event.getId();
    }

    void dispatch(Long eventId) {
        if (eventId == null) {
            return;
        }
        ManagedFlinkStateEvent event = eventMapper.selectById(eventId);
        if (event == null
            || !("PENDING".equals(event.getAlertState())
                || "FAILED".equals(event.getAlertState()))) {
            return;
        }
        int claimed =
            eventMapper.update(
                null,
                new LambdaUpdateWrapper<ManagedFlinkStateEvent>()
                    .eq(ManagedFlinkStateEvent::getId, eventId)
                    .in(ManagedFlinkStateEvent::getAlertState, "PENDING", "FAILED")
                    .lt(ManagedFlinkStateEvent::getAlertAttempts, 3)
                    .set(ManagedFlinkStateEvent::getAlertState, "SENDING")
                    .setSql("alert_attempts = alert_attempts + 1")
                    .set(ManagedFlinkStateEvent::getModifyTime, new Date()));
        if (claimed != 1) {
            return;
        }
        FlinkApplication application = applicationMapper.selectById(event.getAppId());
        ManagedFlinkApplication managed =
            managedApplicationMapper.selectById(event.getAppId());
        if (application == null || managed == null || application.getAlertId() == null) {
            finish(eventId, "SKIPPED", null);
            return;
        }
        FlinkAppStateEnum state = FlinkAppStateEnum.getState(event.getToState());
        org.apache.streampark.console.core.bean.AlertTemplate template =
            AlertTemplateUtils.createAlertTemplate(application, state);
        template.setLink(managed.getConsoleUrl());
        boolean sent = alertService.alert(application.getAlertId(), template);
        finish(eventId, sent ? "SENT" : "FAILED", sent ? null : "ALERT:SendFailed");
    }

    void dispatchPending() {
        Date stale = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5));
        eventMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkStateEvent>()
                .eq(ManagedFlinkStateEvent::getAlertState, "SENDING")
                .lt(ManagedFlinkStateEvent::getModifyTime, stale)
                .lt(ManagedFlinkStateEvent::getAlertAttempts, 3)
                .set(ManagedFlinkStateEvent::getAlertState, "FAILED")
                .set(ManagedFlinkStateEvent::getLastErrorCode, "ALERT:DispatchInterrupted")
                .set(ManagedFlinkStateEvent::getModifyTime, new Date()));
        List<ManagedFlinkStateEvent> pending =
            eventMapper.selectList(
                new LambdaQueryWrapper<ManagedFlinkStateEvent>()
                    .in(ManagedFlinkStateEvent::getAlertState, "PENDING", "FAILED")
                    .lt(ManagedFlinkStateEvent::getAlertAttempts, 3)
                    .orderByAsc(ManagedFlinkStateEvent::getModifyTime)
                    .last("limit 100"));
        for (ManagedFlinkStateEvent event : pending) {
            dispatch(event.getId());
        }
    }

    private void finish(Long eventId, String state, String errorCode) {
        eventMapper.update(
            null,
            new LambdaUpdateWrapper<ManagedFlinkStateEvent>()
                .eq(ManagedFlinkStateEvent::getId, eventId)
                .eq(ManagedFlinkStateEvent::getAlertState, "SENDING")
                .set(ManagedFlinkStateEvent::getAlertState, state)
                .set(ManagedFlinkStateEvent::getLastErrorCode, errorCode)
                .set(ManagedFlinkStateEvent::getModifyTime, new Date()));
    }

    private static boolean shouldAlert(String fromState, String toState) {
        return PROBLEM_STATES.contains(toState)
            || PROBLEM_STATES.contains(fromState) && !PROBLEM_STATES.contains(toState);
    }

    private static String hash(String value) {
        try {
            byte[] digest =
                MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Managed Flink state event cannot be hashed.");
        }
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
