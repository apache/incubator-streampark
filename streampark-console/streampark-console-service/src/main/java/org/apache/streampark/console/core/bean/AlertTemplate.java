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

import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.enums.CheckPointStatus;
import org.apache.streampark.console.core.enums.FlinkAppState;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

@Data
@Accessors(chain = true)
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
  private Integer affectedJobs;

  public static AlertTemplate of(Application application, FlinkAppState appState) {
    return new AlertTemplateBuilder()
        .setDuration(application.getStartTime(), application.getEndTime())
        .setJobName(application.getJobName())
        .setLink(application.getExecutionModeEnum(), application.getAppId())
        .setStartTime(application.getStartTime())
        .setEndTime(application.getEndTime())
        .setRestart(application.isNeedRestartOnFailed(), application.getRestartCount())
        .setRestartIndex(application.getRestartCount())
        .setTotalRestart(application.getRestartSize())
        .setType(1)
        .setTitle(String.format("Notify: %s %s", application.getJobName(), appState.name()))
        .setSubject(String.format("StreamPark Alert: %s %s", application.getJobName(), appState))
        .setStatus(appState.name())
        .build();
  }

  public static AlertTemplate of(Application application, CheckPointStatus checkPointStatus) {
    return new AlertTemplateBuilder()
        .setDuration(application.getStartTime(), application.getEndTime())
        .setJobName(application.getJobName())
        .setLink(application.getExecutionModeEnum(), application.getAppId())
        .setStartTime(application.getStartTime())
        .setType(2)
        .setCpFailureRateInterval(
            DateUtils.toDuration(application.getCpFailureRateInterval() * 1000 * 60))
        .setCpMaxFailureInterval(application.getCpMaxFailureInterval())
        .setTitle(String.format("Notify: %s checkpoint FAILED", application.getJobName()))
        .setSubject(
            String.format("StreamPark Alert: %s, checkPoint is Failed", application.getJobName()))
        .build();
  }

  public static AlertTemplate of(FlinkCluster cluster, ClusterState clusterState) {
    return new AlertTemplateBuilder()
        .setDuration(cluster.getStartTime(), cluster.getEndTime())
        .setJobName(cluster.getClusterName())
        .setLink(ExecutionMode.YARN_SESSION, cluster.getClusterId())
        .setStartTime(cluster.getStartTime())
        .setEndTime(cluster.getEndTime())
        .setType(3)
        .setTitle(String.format("Notify: %s %s", cluster.getClusterName(), clusterState.name()))
        .setSubject(
            String.format("StreamPark Alert: %s %s", cluster.getClusterName(), clusterState))
        .setStatus(clusterState.name())
        .setAffectedJobs(cluster.getJobs())
        .build();
  }

  private static class AlertTemplateBuilder {
    private AlertTemplate alertTemplate = new AlertTemplate();

    public AlertTemplateBuilder setTitle(String title) {
      alertTemplate.setTitle(title);
      return this;
    }

    public AlertTemplateBuilder setSubject(String subject) {
      alertTemplate.setSubject(subject);
      return this;
    }

    public AlertTemplateBuilder setJobName(String jobName) {
      alertTemplate.setJobName(jobName);
      return this;
    }

    public AlertTemplateBuilder setType(Integer type) {
      alertTemplate.setType(type);
      return this;
    }

    public AlertTemplateBuilder setStatus(String status) {
      alertTemplate.setStatus(status);
      return this;
    }

    public AlertTemplateBuilder setStartTime(Date startTime) {
      alertTemplate.setStartTime(
          DateUtils.format(startTime, DateUtils.fullFormat(), TimeZone.getDefault()));
      return this;
    }

    public AlertTemplateBuilder setEndTime(Date endTime) {
      alertTemplate.setEndTime(
          DateUtils.format(
              endTime == null ? new Date() : endTime,
              DateUtils.fullFormat(),
              TimeZone.getDefault()));
      return this;
    }

    public AlertTemplateBuilder setDuration(String duration) {
      alertTemplate.setDuration(duration);
      return this;
    }

    public AlertTemplateBuilder setDuration(Date start, Date end) {
      long duration;
      if (start == null && end == null) {
        duration = 0L;
      } else if (end == null) {
        duration = System.currentTimeMillis() - start.getTime();
      } else {
        duration = end.getTime() - start.getTime();
      }
      alertTemplate.setDuration(DateUtils.toDuration(duration));
      return this;
    }

    public AlertTemplateBuilder setLink(String link) {
      alertTemplate.setLink(link);
      return this;
    }

    public AlertTemplateBuilder setLink(ExecutionMode mode, String appId) {
      if (ExecutionMode.isYarnMode(mode)) {
        String format = "%s/proxy/%s/";
        String url = String.format(format, YarnUtils.getRMWebAppURL(false), appId);
        alertTemplate.setLink(url);
      } else {
        alertTemplate.setLink(null);
      }
      return this;
    }

    public AlertTemplateBuilder setCpFailureRateInterval(String cpFailureRateInterval) {
      alertTemplate.setCpFailureRateInterval(cpFailureRateInterval);
      return this;
    }

    public AlertTemplateBuilder setCpMaxFailureInterval(Integer cpMaxFailureInterval) {
      alertTemplate.setCpMaxFailureInterval(cpMaxFailureInterval);
      return this;
    }

    public AlertTemplateBuilder setRestart(Boolean restart) {
      alertTemplate.setRestart(restart);
      return this;
    }

    public AlertTemplateBuilder setRestart(Boolean needRestartOnFailed, Integer restartCount) {
      boolean needRestart = needRestartOnFailed && restartCount > 0;
      alertTemplate.setRestart(needRestart);
      return this;
    }

    public AlertTemplateBuilder setRestartIndex(Integer restartIndex) {
      if (alertTemplate.getRestart()) {
        alertTemplate.setRestartIndex(restartIndex);
      }
      return this;
    }

    public AlertTemplateBuilder setTotalRestart(Integer totalRestart) {
      if (alertTemplate.getRestart()) {
        alertTemplate.setTotalRestart(totalRestart);
      }
      return this;
    }

    public AlertTemplateBuilder setAtAll(Boolean atAll) {
      alertTemplate.setAtAll(atAll);
      return this;
    }

    public AlertTemplateBuilder setAffectedJobs(Integer jobs) {
      alertTemplate.setAffectedJobs(jobs);
      return this;
    }

    public AlertTemplate build() {
      return this.alertTemplate;
    }
  }
}
