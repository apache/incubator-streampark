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

/** Application status, whether it exists, and where it exists */
public enum AppExistsStateEnum {

    /** no exists */
    NO(0),

    /** exists in database */
    IN_DB(1),

    /** exists in yarn */
    IN_YARN(2),

    /** exists in remote kubernetes cluster. */
    IN_KUBERNETES(3),

    /** job name invalid because of special utf-8 character */
    INVALID(4);

    private final int value;

    AppExistsStateEnum(int value) {
        this.value = value;
    }

    public int get() {
        return this.value;
    }
}
