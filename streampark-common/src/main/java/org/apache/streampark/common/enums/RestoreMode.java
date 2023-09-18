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

import java.util.Arrays;

public enum RestoreMode {
  NO_CLAIM(1),
  CLAIM(2),
  LEGACY(3);

  public static final String RESTORE_MODE = "execution.savepoint-restore-mode";
  public static final int SINCE_FLINK_VERSION = 15;

  private final int value;

  public int get() {
    return this.value;
  }

  RestoreMode(int value) {
    this.value = value;
  }

  public String getName() {
    return String.valueOf(RestoreMode.of(this.value));
  }

  public static RestoreMode of(Integer value) {
    return Arrays.stream(values()).filter((x) -> x.value == value).findFirst().orElse(null);
  }
}
