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

/**
 * Internal use of the system.
 *
 * @param <T> the type of the configuration value.
 * @param key key of configuration that consistent with the spring config.
 * @param defaultValue default value of configuration that <b>should not be null</b>.
 * @param classType the class type of value. <b>please use java class type</b>.
 * @param description description of configuration.
 */
public final class InternalOption<T> implements InternalOptionSpec {

    private final String key;
    private final T defaultValue;
    private final Class<T> classType;
    private final String description;

    public InternalOption(String key, T defaultValue, Class<T> classType, String description) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.classType = classType;
        this.description = description;
        InternalConfigHolder.register(this);
    }

    public InternalOption(String key, T defaultValue, Class<T> classType) {
        this(key, defaultValue, classType, "");
    }

    // Scala-compatible accessors (matching case class field names)
    public String key() {
        return key;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public Class<T> classType() {
        return classType;
    }

    public String description() {
        return description;
    }

    public String getKey() {
        return key;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public Class<T> getClassType() {
        return classType;
    }

    public String getDescription() {
        return description;
    }
}
