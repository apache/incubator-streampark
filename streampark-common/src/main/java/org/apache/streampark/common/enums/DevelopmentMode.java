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

public enum DevelopmentMode implements Serializable {

  /** custom code */
  CUSTOM_CODE("Custom Code", 1),

  /** Flink SQL */
  FLINK_SQL("Flink SQL", 2);

  private final String mode;

  private final Integer value;

  DevelopmentMode(String mode, Integer value) {
    this.mode = mode;
    this.value = value;
  }

  public static DevelopmentMode of(Integer value) {
    for (DevelopmentMode mode : values()) {
      if (mode.value.equals(value)) {
        return mode;
      }
    }
    return null;
  }

  public Integer getValue() {
    return value;
  }
}
