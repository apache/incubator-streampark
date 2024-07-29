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
package org.apache.streampark.flink.core.test;

import org.apache.flink.table.functions.ScalarFunction;

public class FetchMillisecond extends ScalarFunction {

    /**
     * Get current milliseconds.
     *
     * @return Current milliseconds on the system.
     */
    public Long eval() {
        return System.currentTimeMillis();
    }

    /**
     * Whether is a determined value.<br>
     * return true: It represents that the function will be executed once at the stage of flink
     * planner, then it will send the executed result back to <code>runtime</code>, it's a determined
     * value.<br>
     * return false: It represents that the function will be executed when the engine processed a row
     * in the running stage. It's not a determined value.<br>
     * Note: The function will return <code>true</code> by default, so the function need not be
     * overwrite in general.
     *
     * @return <code>true</code> if it's a determined value else <code>false</code>.
     */
    @Override
    public boolean isDeterministic() {
        return false;
    }
}
