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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.console.base.util.ConsistentHash;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.bean.FlinkTaskItem;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.DistributedTask;
import org.apache.streampark.console.core.enums.DistributedTaskEnum;
import org.apache.streampark.console.core.enums.EngineTypeEnum;
import org.apache.streampark.console.core.mapper.DistributedTaskMapper;
import org.apache.streampark.console.core.registry.ConsoleRegistryClient;
import org.apache.streampark.console.core.service.DistributedTaskService;
import org.apache.streampark.console.core.service.application.ApplicationActionService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class DistributedTaskServiceImpl extends ServiceImpl<DistributedTaskMapper, DistributedTask>
    implements
        DistributedTaskService {

    @Qualifier("streamparkDistributedTaskExecutor")
    @Autowired
    private Executor taskExecutor;

    @Autowired
    private ApplicationActionService applicationActionService;

    @Autowired
    private ConsoleRegistryClient consoleRegistryClient;

    /**
     * Server name
     */
    private String serverId;

    /**
     * Consistent hash algorithm for task distribution
     */
    private final ConsistentHash<String> consistentHash = new ConsistentHash<>(Collections.emptyList());

    /**
     * Task execution status
     */
    private final ConcurrentHashMap<Long, Boolean> runningTasks = new ConcurrentHashMap<>();

    /**
     * Initialize the consistent hash ring.
     * @param allServers All servers
     * @param serverId The name of the current server
     */
    public void init(Set<String> allServers, String serverId) {
        this.serverId = serverId;
        for (String server : allServers) {
            consistentHash.add(server);
        }
    }

    @Scheduled(fixedDelay = 50)
    public void pollDistributedTask() {
        List<DistributedTask> distributedTaskList = this.list();
        for (DistributedTask DistributedTask : distributedTaskList) {
            long taskId = DistributedTask.getId();
            if (DistributedTask.getEngineType() != EngineTypeEnum.FLINK || !isLocalProcessing(taskId)) {
                continue;
            }
            if (runningTasks.putIfAbsent(taskId, true) == null) {
                taskExecutor.execute(() -> {
                    try {
                        // Execute Distributed task
                        executeDistributedTask(DistributedTask);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        runningTasks.remove(DistributedTask.getId());
                        this.removeById(DistributedTask.getId());
                    }
                });
            }
        }
    }

    /**
     * This interface is responsible for polling the database to retrieve task records and execute the corresponding operations.
     * @param DistributedTask DistributedTask
     */
    @Override
    public void executeDistributedTask(DistributedTask DistributedTask) throws Exception {
        // Execute Distributed task
        log.info("Execute Distributed task: {}", DistributedTask);
        FlinkTaskItem flinkTaskItem = getFlinkTaskItem(DistributedTask);
        Application appParam = getAppByFlinkTaskItem(flinkTaskItem);
        switch (DistributedTask.getAction()) {
            case START:
                applicationActionService.start(appParam, flinkTaskItem.getAutoStart());
                break;
            case RESTART:
                applicationActionService.restart(appParam);
                break;
            case REVOKE:
                applicationActionService.revoke(appParam.getId());
                break;
            case CANCEL:
                applicationActionService.cancel(appParam);
                break;
            case ABORT:
                applicationActionService.abort(appParam.getId());
                break;
            default:
                log.error("Unsupported task: {}", DistributedTask.getAction());
        }
    }

    /**
     * Through this interface, the watcher obtains the list of tasks that need to be monitored.
     * @param applications List<Application>
     * @return List<Application> List of tasks that need to be monitored
     */
    @Override
    public List<Application> getMonitoredTaskList(List<Application> applications) {
        return applications.stream()
            .filter(application -> isLocalProcessing(application.getId()))
            .collect(Collectors.toList());
    }

    /**
     * This interface handles task redistribution when server nodes are added.
     *
     * @param serverName String
     */
    @Override
    public void addServer(String serverName) {
        consistentHash.add(serverName);
    }

    /**
     * This interface handles task redistribution when server nodes are removed.
     *
     * @param serverName String
     */
    @Override
    public void removeServer(String serverName) {
        consistentHash.remove(serverName);
    }

    /**
     * Determine whether the task is processed locally.
     *
     * @param appId Long
     * @return boolean
     */
    @Override
    public boolean isLocalProcessing(Long appId) {
        return consistentHash.get(appId).equals(serverId);
    }

    /**
     * Save Distributed task.
     *
     * @param appParam  Application
     * @param autoStart boolean
     * @param action It may be one of the following values: START, RESTART, REVOKE, CANCEL, ABORT
     */
    @Override
    public void saveDistributedTask(Application appParam, boolean autoStart, DistributedTaskEnum action) {
        try {
            DistributedTask DistributedTask = getDistributedTaskByApp(appParam, autoStart, action);
            this.save(DistributedTask);
        } catch (JsonProcessingException e) {
            log.error("Failed to save Distributed task: {}", e.getMessage());
        }
    }

    public DistributedTask getDistributedTaskByApp(Application appParam, boolean autoStart,
                                                   DistributedTaskEnum action) throws JsonProcessingException {
        FlinkTaskItem flinkTaskItem = new FlinkTaskItem();
        flinkTaskItem.setAppId(appParam.getId());
        flinkTaskItem.setAutoStart(autoStart);
        flinkTaskItem.setArgs(appParam.getArgs());
        flinkTaskItem.setDynamicProperties(appParam.getDynamicProperties());
        flinkTaskItem.setSavepointPath(appParam.getSavepointPath());
        flinkTaskItem.setRestoreOrTriggerSavepoint(appParam.getRestoreOrTriggerSavepoint());
        flinkTaskItem.setDrain(appParam.getDrain());
        flinkTaskItem.setNativeFormat(appParam.getNativeFormat());
        flinkTaskItem.setRestoreMode(appParam.getRestoreMode());

        DistributedTask distributedTask = new DistributedTask();
        distributedTask.setAction(action);
        distributedTask.setEngineType(EngineTypeEnum.FLINK);
        distributedTask.setProperties(JacksonUtils.write(flinkTaskItem));
        return distributedTask;
    }

    public FlinkTaskItem getFlinkTaskItem(DistributedTask DistributedTask) throws JsonProcessingException {
        return JacksonUtils.read(DistributedTask.getProperties(), FlinkTaskItem.class);
    }

    public Application getAppByFlinkTaskItem(FlinkTaskItem flinkTaskItem) {
        Application appParam = new Application();
        appParam.setId(flinkTaskItem.getAppId());
        appParam.setArgs(flinkTaskItem.getArgs());
        appParam.setDynamicProperties(flinkTaskItem.getDynamicProperties());
        appParam.setSavepointPath(flinkTaskItem.getSavepointPath());
        appParam.setRestoreOrTriggerSavepoint(flinkTaskItem.getRestoreOrTriggerSavepoint());
        appParam.setDrain(flinkTaskItem.getDrain());
        appParam.setNativeFormat(flinkTaskItem.getNativeFormat());
        appParam.setRestoreMode(flinkTaskItem.getRestoreMode());
        return appParam;
    }

    public long getConsistentHashSize() {
        return consistentHash.getSize();
    }
}
