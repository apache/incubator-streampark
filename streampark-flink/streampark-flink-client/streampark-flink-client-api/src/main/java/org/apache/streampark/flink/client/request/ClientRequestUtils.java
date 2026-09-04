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

package org.apache.streampark.flink.client.request;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Defensive map conversions shared by immutable Flink client request objects. */
final class ClientRequestUtils {

    private ClientRequestUtils() {
    }

    /** Returns a mutable defensive copy for callers that consume request properties. */
    static Map<String, Object> copyPropertiesMap(
                                                 @Nullable Map<String, Serializable> properties) {
        return properties == null ? new LinkedHashMap<>() : new LinkedHashMap<>(properties);
    }

    /**
     * Validates and copies properties stored on a request crossing the shims classloader boundary.
     *
     * <p>Null values carry no configuration meaning and are omitted. Rejecting non-serializable
     * values at construction time produces a local error instead of a delayed proxy failure.
     */
    static Map<String, Serializable> toSerializableMap(
                                                       @Nullable Map<String, Object> properties) {
        Map<String, Serializable> result = new LinkedHashMap<>();
        if (properties == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof Serializable) {
                result.put(entry.getKey(), (Serializable) value);
            } else {
                throw new IllegalArgumentException(
                    "Flink client property is not serializable: " + entry.getKey());
            }
        }
        return result;
    }
}
