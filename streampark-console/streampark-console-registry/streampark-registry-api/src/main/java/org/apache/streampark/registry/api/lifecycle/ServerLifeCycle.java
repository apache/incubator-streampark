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

package org.apache.streampark.registry.api.lifecycle;

import lombok.Getter;

/**
 * This enum is used to represent the server status, include master/worker.
 */
@Getter
public enum ServerLifeCycle {

    RUNNING(0, "The current server is running"),
    STOPPED(2, "The current server is stopped"),
    ;

    private final int code;
    private final String desc;

    ServerLifeCycle(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
