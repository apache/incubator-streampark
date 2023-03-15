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

package org.apache.streampark.console.core.enums;

import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;

@Getter
public enum OptionState implements Serializable {

  /** Application which is currently action: none. */
  NONE(0),
  /** Application which is currently action: releasing. */
  RELEASING(1),
  /** Application which is currently action: cancelling. */
  CANCELLING(2),

  /** Application which is currently action: starting. */
  STARTING(3),

  /** Application which is currently action: savepointing. */
  SAVEPOINTING(4);

  private final int value;

  OptionState(int value) {
    this.value = value;
  }

  public static OptionState of(Integer state) {
    return Arrays.stream(values()).filter((x) -> x.value == state).findFirst().orElse(null);
  }
}
