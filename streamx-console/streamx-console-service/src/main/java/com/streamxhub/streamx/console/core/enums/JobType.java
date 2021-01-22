/*
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
package com.streamxhub.streamx.console.core.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum JobType {
    /**
     * dataStream APi类型的作业
     */
    DATASTREAM(1, "dataStream"),
    /**
     * FLink SQL
     */
    PURESQL(2, "Pure SQL"),
    /**
     * 代码开发的SQL作业
     */
    DEVSQL(3, "Dev SQL");

    int type;
    String name;

    JobType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public static JobType of(Integer type) {
        return Arrays.stream(values()).filter((x) -> x.type == type).findFirst().orElse(null);
    }
}
