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

package org.apache.streampark.flink.kubernetes;

import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.conf.K8sFlinkConfig;

public class TrackConfig {

  public static class FlinkTrackConfig {
    public final JobStatusWatcherConfig jobStatusWatcherConf;
    public final MetricWatcherConfig metricWatcherConf;

    public FlinkTrackConfig(
        JobStatusWatcherConfig jobStatusWatcherConf, MetricWatcherConfig metricWatcherConf) {
      this.jobStatusWatcherConf = jobStatusWatcherConf;
      this.metricWatcherConf = metricWatcherConf;
    }

    public static FlinkTrackConfig defaultConf() {
      return new FlinkTrackConfig(
          JobStatusWatcherConfig.defaultConf(), MetricWatcherConfig.defaultConf());
    }

    public static FlinkTrackConfig debugConf() {
      return new FlinkTrackConfig(
          JobStatusWatcherConfig.debugConf(), MetricWatcherConfig.debugConf());
    }

    public static FlinkTrackConfig fromConfigHub() {
      return new FlinkTrackConfig(
          new JobStatusWatcherConfig(
              InternalConfigHolder.get(K8sFlinkConfig.jobStatusTrackTaskTimeoutSec()),
              InternalConfigHolder.get(K8sFlinkConfig.jobStatueTrackTaskIntervalSec()),
              InternalConfigHolder.get(K8sFlinkConfig.silentStateJobKeepTrackingSec())),
          new MetricWatcherConfig(
              InternalConfigHolder.get(K8sFlinkConfig.metricTrackTaskTimeoutSec()),
              InternalConfigHolder.get(K8sFlinkConfig.metricTrackTaskIntervalSec())));
    }
  }

  public static class MetricWatcherConfig {
    public final long requestTimeoutSec;
    public final long requestIntervalSec;

    public MetricWatcherConfig(long requestTimeoutSec, long requestIntervalSec) {
      this.requestTimeoutSec = requestTimeoutSec;
      this.requestIntervalSec = requestIntervalSec;
    }

    public static MetricWatcherConfig defaultConf() {
      return new MetricWatcherConfig(120, 10);
    }

    public static MetricWatcherConfig debugConf() {
      return new MetricWatcherConfig(120, 2);
    }
  }

  public static class JobStatusWatcherConfig {
    public final long requestTimeoutSec;
    public final long requestIntervalSec;
    public final int silentStateJobKeepTrackingSec;

    public JobStatusWatcherConfig(
        long requestTimeoutSec, long requestIntervalSec, int silentStateJobKeepTrackingSec) {
      this.requestTimeoutSec = requestTimeoutSec;
      this.requestIntervalSec = requestIntervalSec;
      this.silentStateJobKeepTrackingSec = silentStateJobKeepTrackingSec;
    }

    public static JobStatusWatcherConfig defaultConf() {
      return new JobStatusWatcherConfig(120, 5, 60);
    }

    public static JobStatusWatcherConfig debugConf() {
      return new JobStatusWatcherConfig(120, 2, 5);
    }
  }
}
