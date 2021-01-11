/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.console.core.enums;

/**
 * @author benjobs
 */

public enum ApplicationType {
    /**
     * StreamX Flink
     */
    STREAMX_FLINK(1, "StreamX Flink"),
    /**
     * Apache Flink
     */
    APACHE_FLINK(2, "Apache Flink"),
    /**
     * StreamX Spark
     */
    STREAMX_SPARK(3, "StreamX Spark"),
    /**
     * Apache Spark
     */
    APACHE_SPARK(4, "Apache Spark");
    int type;
    String name;

    ApplicationType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static ApplicationType of(int type) {
        for (ApplicationType etype : ApplicationType.values()) {
            if (etype.getType() == type) {
                return etype;
            }
        }
        return null;
    }


}
