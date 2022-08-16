/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes;

import io.fabric8.kubernetes.api.model.DeleteOptions;
import io.fabric8.kubernetes.api.model.DeleteOptionsBuilder;

import javax.annotation.Nullable;

public enum DeletionPropagationPolicy {
    BACKGROUND(deleteOptions("Background")),
    FOREGROUND(deleteOptions("Foreground")),
    ORPHAN(deleteOptions("Orphan")),
    NONE(null),
    UNKNOWN(null);

    @Nullable
    private final DeleteOptions deleteOptions;

    private DeletionPropagationPolicy(DeleteOptions deleteOptions) {
        this.deleteOptions = deleteOptions;
    }

    @Nullable
    DeleteOptions getDeleteOptions() {
        return this.deleteOptions;
    }

    private static DeleteOptions deleteOptions(String propagationPolicy) {
        return new DeleteOptionsBuilder().withPropagationPolicy(propagationPolicy).build();
    }

    public static DeletionPropagationPolicy fromString(String raw) {
        if (raw == null) {
            return NONE;
        } else {
            switch (raw.toLowerCase()) {
                case "background":
                    return BACKGROUND;
                case "foreground":
                    return FOREGROUND;
                case "orphan":
                    return ORPHAN;
                default:
                    return UNKNOWN;
            }
        }
    }
}
