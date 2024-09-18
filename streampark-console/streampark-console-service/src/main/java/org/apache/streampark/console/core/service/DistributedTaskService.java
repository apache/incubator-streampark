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

import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.DistributedTask;
import org.apache.streampark.console.core.enums.DistributedTaskEnum;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * DistributedTaskService is the interface for managing tasks.
 */
public interface DistributedTaskService extends IService<DistributedTask> {

    /**
     * Initialize the consistent hash ring.
     * @param allServers All servers
     * @param serverId The name of the current server
     */
    void init(Set<String> allServers, String serverId);

    /**
     * This interface is responsible for polling the database to retrieve task records and execute the corresponding operations.
     * @param DistributedTask DistributedTask
     */
    void executeDistributedTask(DistributedTask DistributedTask) throws Exception;

    /**
     * Through this interface, the watcher obtains the list of tasks that need to be monitored.
     * @param applications List<Application>
     * @return List<Application> List of tasks that need to be monitored
     */
    List<Application> getMonitoredTaskList(List<Application> applications);

    /**
     * This interface handles task redistribution when server nodes are added.
     * @param serverName String
     */
    void addServer(String serverName);

    /**
     * This interface handles task redistribution when server nodes are removed.
     * @param serverName String
     */
    void removeServer(String serverName);

    /**
     * Determine whether the task is processed locally.
     *
     * @param appId Long
     * @return boolean
     */
    public boolean isLocalProcessing(Long appId);

    /**
     * Save Distributed Task.
     *
     * @param appParam  Application
     * @param autoStart boolean
     * @param action It may be one of the following values: START, RESTART, REVOKE, CANCEL, ABORT
     */
    public void saveDistributedTask(Application appParam, boolean autoStart, DistributedTaskEnum action);
}
