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

package org.apache.streampark.flink.kubernetes.enums;

import java.util.ArrayList;
import java.util.List;

/** flink job status on kubernetes */
public enum FlinkJobState {

  /** flink job has been submitted by the streampark. */
  STARTING,

  /** flink k8s resources are being initialized. */
  K8S_INITIALIZING,

  /** lost track of flink job temporarily. */
  SILENT,

  /** flink job has terminated positively (maybe FINISHED or CANCELED) */
  POS_TERMINATED,

  /** flink job has terminated (maybe FINISHED, CACNELED or FAILED) */
  TERMINATED,

  /** lost track of flink job completely. */
  LOST,

  /** other flink state */
  OTHER,

  /**
   * the following enum have the same meaning as the native flink state enum.
   *
   * @see org.apache.flink.api.common.JobStatus
   */
  INITIALIZING,
  CREATED,
  RUNNING,
  FAILING,
  FAILED,
  CANCELLING,
  CANCELED,
  FINISHED,
  RESTARTING,
  SUSPENDED,
  RECONCILING;

  public static final List<FlinkJobState> ENDING_STATES = new ArrayList<>();

  static {
    ENDING_STATES.add(FAILED);
    ENDING_STATES.add(CANCELED);
    ENDING_STATES.add(FINISHED);
    ENDING_STATES.add(POS_TERMINATED);
    ENDING_STATES.add(TERMINATED);
    ENDING_STATES.add(LOST);
  }

  public static FlinkJobState of(String value) {
    FlinkJobState[] values = FlinkJobState.values();
    for (FlinkJobState flinkJobState : values) {
      if (flinkJobState.toString().equals(value.toUpperCase())) {
        return flinkJobState;
      }
    }
    return OTHER;
  }

  public static boolean isEndState(FlinkJobState state) {
    return ENDING_STATES.contains(state);
  }
}
