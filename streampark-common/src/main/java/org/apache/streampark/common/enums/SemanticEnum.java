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

/** Flink consistency semantics */
public enum SemanticEnum {

  /**
   * Ensure that the counting results obtained after a fault are consistent with the correct values.
   */
  EXACTLY_ONCE,

  /** The program may calculate more after a malfunction, but it will never calculate less. */
  AT_LEAST_ONCE,

  /** After the fault occurs, the counting results may be lost. */
  NONE;

  public static SemanticEnum of(String name) {
    for (SemanticEnum semanticEnum : SemanticEnum.values()) {
      if (name.equals(semanticEnum.name())) {
        return semanticEnum;
      }
    }
    return null;
  }
}
