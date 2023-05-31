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

package org.apache.streampark.common.enums;

import java.io.Serializable;

/** @since: 1.2.3 */
public enum ClusterState implements Serializable {
  /** The cluster was just created but not started */
  CREATED(0),
  /** cluster started */
  RUNNING(1),
  /** cluster stopped */
  STOPPED(2),
  /** cluster lost */
  LOST(3),
  /** cluster unknown */
  UNKNOWN(4);

  private final Integer value;

  ClusterState(Integer value) {
    this.value = value;
  }

  public static ClusterState of(Integer value) {
    for (ClusterState clusterState : values()) {
      if (clusterState.value.equals(value)) {
        return clusterState;
      }
    }
    return ClusterState.UNKNOWN;
  }

  public static ClusterState of(String value) {
    for (ClusterState clusterState : values()) {
      if (clusterState.name().equals(value)) {
        return clusterState;
      }
    }
    return ClusterState.UNKNOWN;
  }

  public Integer getValue() {
    return value;
  }

  public static boolean isCreateState(ClusterState state) {
    return CREATED.equals(state);
  }

  public static boolean isRunningState(ClusterState state) {
    return RUNNING.equals(state);
  }

  public static boolean isStoppedState(ClusterState state) {
    return STOPPED.equals(state);
  }

  public static boolean isLostState(ClusterState state) {
    return LOST.equals(state);
  }
}
