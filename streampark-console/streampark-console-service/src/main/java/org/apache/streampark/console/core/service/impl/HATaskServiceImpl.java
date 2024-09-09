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
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkHATask;
import org.apache.streampark.console.core.entity.HATask;
import org.apache.streampark.console.core.enums.EngineTypeEnum;
import org.apache.streampark.console.core.enums.HATaskEnum;
import org.apache.streampark.console.core.mapper.HATaskMapper;
import org.apache.streampark.console.core.service.HATaskService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class HATaskServiceImpl extends ServiceImpl<HATaskMapper, HATask> implements HATaskService {

    /**
     * Server name
     */
    private String serverName;

    /**
     * Consistent hash algorithm for task distribution
     */
    private final ConsistentHash<String> consistentHash = new ConsistentHash<>(Collections.emptyList());

    @Qualifier("streamparkHATaskExecutor")
    @Autowired
    private Executor taskExecutor;

    @Autowired
    private ApplicationActionService applicationActionService;

    /**
     * Task execution status
     */
    private final ConcurrentHashMap<Long, Boolean> runningTasks = new ConcurrentHashMap<>();

    /**
     * Add the current console server itself to the consistent hash ring.
     */
    public void init(String serverName) {
        this.serverName = serverName;
        consistentHash.add(serverName);
    }

    @Scheduled(fixedDelay = 50)
    public void pollHATask() {
        List<HATask> HATaskList = this.list();
        for (HATask HATask : HATaskList) {
            long taskId = HATask.getId();
            if (HATask.getEngineType() != EngineTypeEnum.FLINK || !isLocalProcessing(taskId)) {
                continue;
            }
            if (runningTasks.putIfAbsent(taskId, true) == null) {
                taskExecutor.execute(() -> {
                    try {
                        // Execute HA task
                        executeHATask(HATask);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        runningTasks.remove(HATask.getId());
                        this.removeById(HATask.getId());
                    }
                });
            }
        }
    }

    /**
     * This interface is responsible for polling the database to retrieve task records and execute the corresponding operations.
     * @param HATask HATask
     */
    @Override
    public void executeHATask(HATask HATask) throws Exception {
        // Execute HA task
        log.info("Execute HA task: {}", HATask);
        FlinkHATask flinkHATask = getFlinkHATask(HATask);
        Application appParam = getAppByFlinkHATask(flinkHATask);
        switch (HATask.getAction()) {
            case START:
                applicationActionService.start(appParam, flinkHATask.getAutoStart());
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
                log.error("Unsupported task: {}", HATask.getAction());
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
     * @param server String
     */
    @Override
    public void addServerRedistribute(String server) {

    }

    /**
     * This interface handles task redistribution when server nodes are removed.
     *
     * @param server String
     */
    @Override
    public void removeServerRedistribute(String server) {

    }

    /**
     * Determine whether the task is processed locally.
     *
     * @param appId Long
     * @return boolean
     */
    @Override
    public boolean isLocalProcessing(Long appId) {
        return consistentHash.get(appId).equals(serverName);
    }

    /**
     * Save HA task.
     *
     * @param appParam  Application
     * @param autoStart boolean
     * @param action It may be one of the following values: START, RESTART, REVOKE, CANCEL, ABORT
     */
    @Override
    public void saveHATask(Application appParam, boolean autoStart, HATaskEnum action) {
        try {
            HATask HATask = getHATaskByApp(appParam, autoStart, action);
            this.save(HATask);
        } catch (JsonProcessingException e) {
            log.error("Failed to save HA task: {}", e.getMessage());
        }
    }

    public HATask getHATaskByApp(Application appParam, boolean autoStart,
                                 HATaskEnum action) throws JsonProcessingException {
        FlinkHATask flinkHATask = new FlinkHATask();
        flinkHATask.setAppId(appParam.getId());
        flinkHATask.setAutoStart(autoStart);
        flinkHATask.setArgs(appParam.getArgs());
        flinkHATask.setDynamicProperties(appParam.getDynamicProperties());
        flinkHATask.setSavepointPath(appParam.getSavepointPath());
        flinkHATask.setRestoreOrTriggerSavepoint(appParam.getRestoreOrTriggerSavepoint());
        flinkHATask.setDrain(appParam.getDrain());
        flinkHATask.setNativeFormat(appParam.getNativeFormat());
        flinkHATask.setRestoreMode(appParam.getRestoreMode());

        HATask haTask = new HATask();
        haTask.setAction(action);
        haTask.setEngineType(EngineTypeEnum.FLINK);
        haTask.setProperties(JacksonUtils.write(flinkHATask));
        return haTask;
    }

    public FlinkHATask getFlinkHATask(HATask HATask) throws JsonProcessingException {
        return JacksonUtils.read(HATask.getProperties(), FlinkHATask.class);
    }

    public Application getAppByFlinkHATask(FlinkHATask flinkHATask) {
        Application appParam = new Application();
        appParam.setId(flinkHATask.getAppId());
        appParam.setArgs(flinkHATask.getArgs());
        appParam.setDynamicProperties(flinkHATask.getDynamicProperties());
        appParam.setSavepointPath(flinkHATask.getSavepointPath());
        appParam.setRestoreOrTriggerSavepoint(flinkHATask.getRestoreOrTriggerSavepoint());
        appParam.setDrain(flinkHATask.getDrain());
        appParam.setNativeFormat(flinkHATask.getNativeFormat());
        appParam.setRestoreMode(flinkHATask.getRestoreMode());
        return appParam;
    }

    public long getConsistentHashSize() {
        return consistentHash.getSize();
    }
}
