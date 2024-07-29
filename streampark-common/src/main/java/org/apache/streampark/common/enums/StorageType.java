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

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Storage type enum. */
public enum StorageType {

    /** hdfs */
    HDFS("hdfs"),

    /** local File system */
    LFS("lfs");

    private final String type;

    StorageType(@Nonnull String type) {
        this.type = type;
    }

    @Nonnull
    public String getType() {
        return type;
    }

    /** Try to resolve the given storage type identifier into a known {@link StorageType}. */
    @Nonnull
    public static StorageType of(@Nullable String identifier) {
        if (StringUtils.isBlank(identifier)) {
            return LFS;
        }
        for (StorageType type : values()) {
            if (type.type.equals(identifier)) {
                return type;
            }
        }
        return LFS;
    }
}
