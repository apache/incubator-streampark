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

package org.apache.streampark.common.conf;

/** Internal use of the system configuration option. */
public class InternalOption {

    private final String key;
    private final Object defaultValue;
    private final Class<?> classType;
    private final String description;

    public InternalOption(String key, Object defaultValue, Class<?> classType) {
        this(key, defaultValue, classType, "");
    }

    public InternalOption(String key, Object defaultValue, Class<?> classType, String description) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.classType = classType;
        this.description = description;
        InternalConfigHolder.register(this);
    }

    public String getKey() {
        return key;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Class<?> getClassType() {
        return classType;
    }

    public String getDescription() {
        return description;
    }

    /** Scala API alias for {@link #getKey()}. */
    public String key() {
        return getKey();
    }

    /** Scala API alias for {@link #getDefaultValue()}. */
    public Object defaultValue() {
        return getDefaultValue();
    }

    /** Scala API alias for {@link #getClassType()}. */
    public Class<?> classType() {
        return getClassType();
    }

    @Override
    public String toString() {
        return "InternalOption{key='" + key + "'}";
    }
}
