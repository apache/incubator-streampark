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

import java.io.Serializable;
import java.util.Arrays;

/** configFile Type enum */
public enum ConfigFileType implements Serializable {
  YAML(1, "yaml"),

  PROPERTIES(2, "prop"),

  HOCON(3, "conf"),

  UNKNOWN(0, null);

  private final int value;
  private final String typeName;

  ConfigFileType(int value, String name) {
    this.value = value;
    this.typeName = name;
  }

  public int getValue() {
    return value;
  }

  public String getTypeName() {
    return typeName;
  }

  public static ConfigFileType of(Integer value) {
    return Arrays.stream(values()).filter((x) -> x.value == value).findFirst().orElse(null);
  }
}
