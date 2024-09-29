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

package org.apache.streampark.console.core.enums;

/**
 * The DistributedTaskEnum represents the possible actions that can be performed on a task.
 */
public enum DistributedTaskEnum {

    /**
     * Starts the specified application.
     */
    START(0),

    /**
     * Restarts the given application.
     */
    RESTART(1),

    /**
     * Revokes access for the given application.
     */
    REVOKE(2),

    /**
     * Cancels the given application. Throws an exception if cancellation fails.
     */
    CANCEL(3),

    /**
     * Forces the given application to stop.
     */
    ABORT(4),

    /**
     * Forces the given application to stop.
     */
    FORCED_STOP(5);

    private final int value;

    DistributedTaskEnum(int value) {
        this.value = value;
    }

    public int get() {
        return this.value;
    }
}
