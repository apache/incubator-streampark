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

import java.util.Arrays;

public enum CheckPointStatus {
    /**
     * IN_PROGRESS
     */
    IN_PROGRESS(1),
    /**
     * COMPLETED
     */
    COMPLETED(2),

    /**
     * FAILED
     */
    FAILED(3);

    int value;

    public int get() {
        return this.value;
    }

    CheckPointStatus(int value) {
        this.value = value;
    }

    public static CheckPointStatus of(Integer value) {
        return Arrays.stream(values()).filter((x) -> x.value == value).findFirst().orElse(null);
    }
}
