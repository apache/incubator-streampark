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

/** classloader.resolve-order */
public enum ResolveOrder {

  /** parent-first */
  PARENT_FIRST("parent-first", 0),

  /** child-first */
  CHILD_FIRST("child-first", 1);

  private final String name;

  private final Integer order;

  ResolveOrder(String name, Integer order) {
    this.name = name;
    this.order = order;
  }

  public static ResolveOrder of(Integer value) {
    for (ResolveOrder order : values()) {
      if (order.order.equals(value)) {
        return order;
      }
    }
    return null;
  }

  public String getName() {
    return name;
  }

  public Integer getOrder() {
    return order;
  }
}
