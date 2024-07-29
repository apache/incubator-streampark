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
package org.apache.streampark.console.core.service;

import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.system.entity.User;

/** Base Service */
public interface CommonService {

    /**
     * Get the information of the currently login user
     *
     * @return Current user information
     */
    User getCurrentUser();

    /**
     * Get the user id of the currently login user
     *
     * @return Current user id
     */
    Long getUserId();

    /**
     * Get SQL client Jar by flink environment
     *
     * @param flinkEnv The FlinkEnv Contains relevant information
     * @return Jar
     */
    String getSqlClientJar(FlinkEnv flinkEnv);

    String getSqlClientJar(SparkEnv flinkEnv);
}
