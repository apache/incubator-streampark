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

import java.util.Arrays;

/** Release Status */
public enum ReleaseStateEnum {

    /** release failed */
    FAILED(-1),
    /** release done */
    DONE(0),

    /** need release after modify task */
    NEED_RELEASE(1),

    /** releasing */
    RELEASING(2),

    /** release complete, need restart */
    NEED_RESTART(3),

    /** need rollback */
    NEED_ROLLBACK(4),

    /** project has changed, need to check the jar whether to be re-selected */
    NEED_CHECK(5),

    /** revoked */
    REVOKED(10);

    private final int value;

    ReleaseStateEnum(int value) {
        this.value = value;
    }

    public int get() {
        return this.value;
    }

    public static ReleaseStateEnum of(Integer state) {
        return Arrays.stream(values()).filter((x) -> x.value == state).findFirst().orElse(null);
    }
}
