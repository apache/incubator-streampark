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

import javax.annotation.Nonnull;

/** Application type enum. */
public enum ApplicationType {

    /** Unknown type replace null */
    UNKNOWN(-1, "unknown"),

    /** StreamPark Flink */
    STREAMPARK_FLINK(1, "StreamPark Flink"),

    /** Apache Flink */
    APACHE_FLINK(2, "Apache Flink"),

    /** StreamPark Spark */
    STREAMPARK_SPARK(3, "StreamPark Spark"),

    /** Apache Spark */
    APACHE_SPARK(4, "Apache Spark");

    private final int type;
    private final String name;

    ApplicationType(int type, @Nonnull String name) {
        this.type = type;
        this.name = name;
    }

    /** Get the type value of the enum. */
    public int getType() {
        return type;
    }

    /** Get the name of application type. */
    @Nonnull
    public String getName() {
        return name;
    }

    /** Try to resolve the given application type value into a known {@link ApplicationType} enum. */
    @Nonnull
    public static ApplicationType of(int type) {
        for (ApplicationType appType : ApplicationType.values()) {
            if (appType.getType() == type) {
                return appType;
            }
        }
        return ApplicationType.UNKNOWN;
    }
}
