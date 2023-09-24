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

import com.google.common.collect.Lists;

import java.util.List;

public enum FlinkExecutionMode {

  /** Local mode */
  LOCAL(0, "local"),

  /** remote */
  REMOTE(1, "remote"),

  /** yarn-per-job mode */
  YARN_PER_JOB(2, "yarn-per-job"),

  /** yarn session */
  YARN_SESSION(3, "yarn-session"),

  /** yarn application */
  YARN_APPLICATION(4, "yarn-application"),

  /** kubernetes session */
  KUBERNETES_NATIVE_SESSION(5, "kubernetes-session"),

  /** kubernetes application */
  KUBERNETES_NATIVE_APPLICATION(6, "kubernetes-application");

  private final Integer mode;

  private final String name;

  FlinkExecutionMode(Integer mode, String name) {
    this.mode = mode;
    this.name = name;
  }

  public static FlinkExecutionMode of(Integer value) {
    for (FlinkExecutionMode mode : values()) {
      if (mode.mode.equals(value)) {
        return mode;
      }
    }
    return null;
  }

  public static FlinkExecutionMode of(String name) {
    for (FlinkExecutionMode mode : values()) {
      if (mode.name.equals(name)) {
        return mode;
      }
    }
    return null;
  }

  public int getMode() {
    return mode;
  }

  public String getName() {
    return name;
  }

  public static boolean isYarnMode(FlinkExecutionMode mode) {
    return YARN_PER_JOB == mode || YARN_APPLICATION == mode || YARN_SESSION == mode;
  }

  // TODO: We'll inline this method back to the corresponding caller lines
  //  after dropping the yarn perjob mode.
  public static boolean isYarnPerJobOrAppMode(FlinkExecutionMode mode) {
    return YARN_PER_JOB == mode || YARN_APPLICATION == mode;
  }

  public static boolean isYarnSessionMode(FlinkExecutionMode mode) {
    return YARN_SESSION == mode;
  }

  public static boolean isYarnMode(Integer value) {
    return isYarnMode(of(value));
  }

  public static boolean isKubernetesSessionMode(Integer value) {
    return KUBERNETES_NATIVE_SESSION == of(value);
  }

  public static boolean isKubernetesMode(FlinkExecutionMode mode) {
    return KUBERNETES_NATIVE_SESSION == mode || KUBERNETES_NATIVE_APPLICATION == mode;
  }

  public static boolean isKubernetesMode(Integer value) {
    return isKubernetesMode(of(value));
  }

  public static boolean isKubernetesApplicationMode(Integer value) {
    return KUBERNETES_NATIVE_APPLICATION == of(value);
  }

  public static List<Integer> getKubernetesMode() {
    return Lists.newArrayList(
        KUBERNETES_NATIVE_SESSION.getMode(), KUBERNETES_NATIVE_APPLICATION.getMode());
  }

  public static boolean isSessionMode(FlinkExecutionMode mode) {
    return KUBERNETES_NATIVE_SESSION == mode || YARN_SESSION == mode;
  }

  public static boolean isRemoteMode(Integer value) {
    return isRemoteMode(of(value));
  }

  public static boolean isRemoteMode(FlinkExecutionMode mode) {
    return REMOTE == mode;
  }
}
