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
import javax.annotation.Nullable;

/** Flink consistency semantics */
public enum Semantic {

    /**
     * Ensure that the counting results obtained after a fault are consistent with the correct values.
     */
    EXACTLY_ONCE,

    /** The program may calculate more after a malfunction, but it will never calculate less. */
    AT_LEAST_ONCE,

    /** After the fault occurs, the counting results may be lost. */
    NONE;

    /** Try to resolve the given semantic name into a known {@link Semantic}. */
    @Nullable
    public static Semantic of(@Nonnull String name) {
        for (Semantic semantic : Semantic.values()) {
            if (name.equals(semantic.name())) {
                return semantic;
            }
        }
        return null;
    }
}
