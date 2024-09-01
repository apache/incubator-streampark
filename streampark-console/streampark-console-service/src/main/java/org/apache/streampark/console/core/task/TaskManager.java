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

package org.apache.streampark.console.core.task;

import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.TaskActionEnum;

import java.util.List;

/**
 * TaskManager is the interface for managing tasks.
 */
public interface TaskManager {

    /**
     * This interface is responsible for polling the database to retrieve task records and execute the corresponding operations.
     * @param appParam Application
     * @param taskAction It may be one of the following values: START, RESTART, REVOKE, CANCEL, ABORT
     */
    void executeTaskAction(Application appParam, TaskActionEnum taskAction);

    /**
     * Through this interface, the watcher obtains the list of tasks that need to be monitored.
     * @param server String
     * @return List<Application>
     */
    List<Application> getMonitoredTaskList(String server);

    /**
     * This interface handles task redistribution when server nodes are added.
     * @param server String
     */
    void addServerRedistribute(String server);

    /**
     * This interface handles task redistribution when server nodes are removed.
     * @param server String
     */
    void removeServerRedistribute(String server);
}
