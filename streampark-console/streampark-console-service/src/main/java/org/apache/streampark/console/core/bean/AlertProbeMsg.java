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

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class AlertProbeMsg {

  private List<Long> alertId;

  private String user;

  private Integer probeJobs;

  private Integer failedJobs;

  private Integer lostJobs;

  private Integer cancelledJobs;

  public void incrementProbeJobs() {
    this.probeJobs++;
  }

  public void incrementFailedJobs() {
    this.failedJobs++;
  }

  public void incrementLostJobs() {
    this.lostJobs++;
  }

  public void incrementCancelledJobs() {
    this.cancelledJobs++;
  }
}
