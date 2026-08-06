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

package org.apache.streampark.console.core.assembler;

import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkApplicationBackup;
import org.apache.streampark.console.core.request.app.AppBackupDeleteRequest;
import org.apache.streampark.console.core.request.app.AppBackupQueryRequest;
import org.apache.streampark.console.core.request.app.AppOptLogDeleteRequest;
import org.apache.streampark.console.core.request.app.AppOptLogQueryRequest;
import org.apache.streampark.console.core.response.app.AppBackupResponse;
import org.apache.streampark.console.core.response.app.AppOptLogResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

public final class AppLogAssembler {

    private AppLogAssembler() {
    }

    public static FlinkApplicationBackup toEntity(AppBackupQueryRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplicationBackup backup = new FlinkApplicationBackup();
        backup.setAppId(request.getAppId());
        backup.setTeamId(request.getTeamId());
        return backup;
    }

    public static FlinkApplicationBackup toEntity(AppBackupDeleteRequest request) {
        if (request == null) {
            return null;
        }
        FlinkApplicationBackup backup = new FlinkApplicationBackup();
        backup.setId(request.getId());
        backup.setAppId(request.getAppId());
        return backup;
    }

    public static ApplicationLog toEntity(AppOptLogQueryRequest request) {
        if (request == null) {
            return null;
        }
        ApplicationLog log = new ApplicationLog();
        log.setAppId(request.getAppId());
        log.setTeamId(request.getTeamId());
        return log;
    }

    public static ApplicationLog toEntity(AppOptLogDeleteRequest request) {
        if (request == null) {
            return null;
        }
        ApplicationLog log = new ApplicationLog();
        log.setId(request.getId());
        log.setAppId(request.getAppId());
        log.setTeamId(request.getTeamId());
        return log;
    }

    public static AppBackupResponse toResponse(FlinkApplicationBackup backup) {
        return DtoAssembler.toDto(backup, AppBackupResponse.class);
    }

    public static AppOptLogResponse toResponse(ApplicationLog log) {
        return DtoAssembler.toDto(log, AppOptLogResponse.class);
    }

    public static IPage<AppBackupResponse> toBackupPage(IPage<FlinkApplicationBackup> page) {
        return DtoAssembler.toPage(page, AppLogAssembler::toResponse);
    }

    public static IPage<AppOptLogResponse> toOptLogPage(IPage<ApplicationLog> page) {
        return DtoAssembler.toPage(page, AppLogAssembler::toResponse);
    }
}
