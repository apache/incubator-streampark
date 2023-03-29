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

package org.apache.streampark.flink.packer.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 *
 * <pre>
 * Status of building pipeline instance.
 * state machine:
 * ┌───────────────────────────────────┐
 * │                      ┌─► success  │
 * │  pending ─► running ─┤            │
 * │                      └─► failure  │
 * └───────────────────────────────────┘
 * </pre>
 */
@SuppressWarnings("AlibabaEnumConstantsMustHaveComment")
public enum PipelineStatus {
  unknown(0),
  pending(1),
  running(2),
  success(3),
  failure(4);

  private final Integer code;

  PipelineStatus(Integer code) {
    this.code = code;
  }

  @JsonCreator
  public static PipelineStatus of(Integer code) {
    for (PipelineStatus status : PipelineStatus.values()) {
      if (status.getCode().equals(code)) {
        return status;
      }
    }
    return unknown;
  }

  @JsonValue
  public Integer getCode() {
    return code;
  }

  public boolean isUnknown() {
    return this == unknown;
  }
}
