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

package org.apache.streampark.console.core.bean;

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.CheckPointStatus;
import org.apache.streampark.console.core.enums.FlinkAppState;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

@Data
public class AlertTemplate implements Serializable {
    private String title;
    private String subject;
    private String jobName;
    private String status;
    private Integer type;
    private String startTime;
    private String endTime;
    private String duration;
    private String link;
    private String cpFailureRateInterval;
    private Integer cpMaxFailureInterval;
    private Boolean restart;
    private Integer restartIndex;
    private Integer totalRestart;
    private boolean atAll = false;

    private static AlertTemplate of(Application application) {
        long duration;
        if (application.getEndTime() == null) {
            duration = System.currentTimeMillis() - application.getStartTime().getTime();
        } else {
            duration = application.getEndTime().getTime() - application.getStartTime().getTime();
        }
        AlertTemplate template = new AlertTemplate();
        template.setJobName(application.getJobName());

        if (ExecutionMode.isYarnMode(application.getExecutionMode())) {
            String format = "%s/proxy/%s/";
            String url = String.format(format, YarnUtils.getRMWebAppURL(), application.getAppId());
            template.setLink(url);
        } else {
            template.setLink(null);
        }

        template.setStartTime(DateUtils.format(application.getStartTime(), DateUtils.fullFormat(), TimeZone.getDefault()));
        template.setEndTime(DateUtils.format(application.getEndTime() == null ? new Date() : application.getEndTime(), DateUtils.fullFormat(), TimeZone.getDefault()));
        template.setDuration(DateUtils.toDuration(duration));
        boolean needRestart = application.isNeedRestartOnFailed() && application.getRestartCount() > 0;
        template.setRestart(needRestart);
        if (needRestart) {
            template.setRestartIndex(application.getRestartCount());
            template.setTotalRestart(application.getRestartSize());
        }
        return template;
    }

    public static AlertTemplate of(Application application, FlinkAppState appState) {
        AlertTemplate template = of(application);
        template.setType(1);
        template.setTitle(String.format("Notify: %s %s", application.getJobName(), appState.name()));
        template.setSubject(String.format("StreamPark Alert: %s %s", template.getJobName(), appState));
        template.setStatus(appState.name());
        return template;
    }

    public static AlertTemplate of(Application application, CheckPointStatus checkPointStatus) {
        AlertTemplate template = of(application);
        template.setType(2);
        template.setCpFailureRateInterval(DateUtils.toDuration(application.getCpFailureRateInterval() * 1000 * 60));
        template.setCpMaxFailureInterval(application.getCpMaxFailureInterval());
        template.setTitle(String.format("Notify: %s checkpoint FAILED", application.getJobName()));
        template.setSubject(String.format("StreamPark Alert: %s, checkPoint is Failed", template.getJobName()));
        return template;
    }
}
