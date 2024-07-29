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

import org.apache.streampark.common.enums.FlinkExecutionMode;
import org.apache.streampark.common.enums.SparkExecutionMode;
import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.common.util.YarnUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private boolean atAll;
    private Integer allJobs;
    private Integer affectedJobs;
    private String user;
    private Integer probeJobs;
    private Integer failedJobs;
    private Integer lostJobs;
    private Integer cancelledJobs;

    public static class AlertTemplateBuilder {

        public AlertTemplateBuilder startTime(Date startTime) {
            this.startTime = DateUtils.format(startTime, DateUtils.fullFormat(), TimeZone.getDefault());
            return this;
        }

        public AlertTemplateBuilder endTime(Date endTime) {
            this.endTime = DateUtils.format(
                endTime == null ? new Date() : endTime,
                DateUtils.fullFormat(),
                TimeZone.getDefault());
            return this;
        }

        public AlertTemplateBuilder duration(Date start, Date end) {
            long duration;
            if (start == null && end == null) {
                duration = 0L;
            } else if (end == null) {
                duration = System.currentTimeMillis() - start.getTime();
            } else {
                duration = end.getTime() - start.getTime();
            }
            this.duration = DateUtils.toDuration(duration);
            return this;
        }

        public AlertTemplateBuilder link(FlinkExecutionMode mode, String appId) {
            if (FlinkExecutionMode.isYarnMode(mode)) {
                String format = "%s/proxy/%s/";
                this.link = String.format(format, YarnUtils.getRMWebAppURL(false), appId);
            } else {
                this.link = null;
            }
            return this;
        }

        public AlertTemplateBuilder link(SparkExecutionMode mode, String appId) {
            if (SparkExecutionMode.isYarnMode(mode)) {
                String format = "%s/proxy/%s/";
                this.link = String.format(format, YarnUtils.getRMWebAppURL(false), appId);
            } else {
                this.link = null;
            }
            return this;
        }

        public AlertTemplateBuilder restart(Boolean needRestartOnFailed, Integer restartCount) {
            this.restart = needRestartOnFailed && restartCount > 0;
            return this;
        }

        public AlertTemplateBuilder restartIndex(Integer restartIndex) {
            if (this.restart) {
                this.restartIndex = restartIndex;
            }
            return this;
        }

        public AlertTemplateBuilder totalRestart(Integer totalRestart) {
            if (this.restart) {
                this.totalRestart = totalRestart;
            }
            return this;
        }
    }
}
