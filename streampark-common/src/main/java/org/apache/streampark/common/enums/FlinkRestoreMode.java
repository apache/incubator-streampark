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

import java.util.Objects;

/** Flink state restore mode enum. */
public enum FlinkRestoreMode {

    /**
     * In this mode Flink claims ownership of the snapshot and essentially treats it like a
     * checkpoint: its controls the lifecycle and might delete it if it is not needed for recovery
     * anymore. Hence, it is not safe to manually delete the snapshot or to start two jobs from the
     * same snapshot. Flink keeps around a configured number of checkpoints.
     */
    CLAIM(1),

    /**
     * In the NO_CLAIM mode Flink will not assume ownership of the snapshot. It will leave the files
     * in user’s control and never delete any of the files. In this mode you can start multiple jobs
     * from the same snapshot.
     */
    NO_CLAIM(2),

    /**
     * The legacy mode is how Flink worked until 1.15. In this mode Flink will never delete the
     * initial checkpoint. At the same time, it is not clear if a user can ever delete it as well.
     */
    LEGACY(3);

    public static final String RESTORE_MODE = "execution.savepoint-restore-mode";
    public static final int SINCE_FLINK_VERSION = 15;

    private final int mode;

    public int get() {
        return this.mode;
    }

    FlinkRestoreMode(int mode) {
        this.mode = mode;
    }

    @Nonnull
    public String getName() {
        return this.toString();
    }

    /**
     * Try to resolve the given flink restore mode value into a known {@link FlinkRestoreMode} enum.
     */
    @Nullable
    public static FlinkRestoreMode of(@Nullable Integer value) {
        for (FlinkRestoreMode flinkRestoreModeEnum : values()) {
            if (Objects.equals(flinkRestoreModeEnum.mode, value)) {
                return flinkRestoreModeEnum;
            }
        }
        return null;
    }
}
