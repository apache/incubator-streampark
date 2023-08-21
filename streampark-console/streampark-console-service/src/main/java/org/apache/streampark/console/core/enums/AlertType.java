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

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The AlertType enum represents different types of alerts that can be used for notifications. */
public enum AlertType {
  /** Email */
  EMAIL(1),

  /** Ding talk */
  DING_TALK(2),

  /** WeChat work */
  WE_COM(4),

  /** Http callback */
  HTTP_CALLBACK(8),

  /** Lark */
  LARK(16);

  /** The empty level */
  private static final Integer EMPTY_LEVEL = 0;

  /** Get the alert type by the code */
  private final Integer code;

  /** A cache map used to quickly get the alert type from an integer code */
  private static final Map<Integer, AlertType> CACHE_MAP = createCacheMap();

  private static Map<Integer, AlertType> createCacheMap() {
    Map<Integer, AlertType> map = new HashMap<>();
    for (AlertType notifyType : AlertType.values()) {
      map.put(notifyType.code, notifyType);
    }
    return Collections.unmodifiableMap(map);
  }

  AlertType(Integer code) {
    this.code = code;
  }

  @JsonValue
  public int getCode() {
    return this.code;
  }

  public static List<AlertType> decode(Integer level) {
    if (level == null) {
      level = EMPTY_LEVEL;
    }

    List<AlertType> result = new ArrayList<>(AlertType.values().length);
    while (level != 0) {
      int code = level & -level;
      result.add(getByCode(code));
      level ^= code;
    }
    return result;
  }

  public static int encode(List<AlertType> alertTypes) {
    if (CollectionUtils.isEmpty(alertTypes)) {
      return EMPTY_LEVEL;
    }

    int result = 0;
    for (AlertType alertType : alertTypes) {
      result |= alertType.code;
    }
    return result;
  }

  private static AlertType getByCode(Integer code) {
    return CACHE_MAP.get(code);
  }
}
