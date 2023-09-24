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

public enum FlinkDevelopmentMode {

  /** custom code */
  CUSTOM_CODE("Custom Code", 1),

  /** Flink SQL */
  FLINK_SQL("Flink SQL", 2),

  /** Py flink */
  PYFLINK("Python Flink", 3);

  private final String name;

  private final Integer mode;

  FlinkDevelopmentMode(String name, Integer mode) {
    this.name = name;
    this.mode = mode;
  }

  public static FlinkDevelopmentMode of(Integer value) {
    for (FlinkDevelopmentMode flinkDevelopmentMode : values()) {
      if (flinkDevelopmentMode.mode.equals(value)) {
        return flinkDevelopmentMode;
      }
    }
    return null;
  }

  public Integer getMode() {
    return mode;
  }
}
