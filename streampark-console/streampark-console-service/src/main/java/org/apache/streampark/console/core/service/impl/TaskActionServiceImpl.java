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
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.TaskAction;
import org.apache.streampark.console.core.enums.TaskActionEnum;
import org.apache.streampark.console.core.mapper.TaskActionMapper;
import org.apache.streampark.console.core.service.TaskActionService;
import org.apache.streampark.console.core.service.application.ApplicationActionService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class TaskActionServiceImpl extends ServiceImpl<TaskActionMapper, TaskAction> implements TaskActionService {

    /**
     * Server name
     */
    private String serverName;

    /**
     * Consistent hash algorithm for task distribution
     */
    private ConsistentHash<String> consistentHash;

    @Qualifier("streamparkTaskActionExecutor")
    @Autowired
    private Executor taskActionExecutor;

    @Autowired
    private ApplicationActionService applicationActionService;

    /**
     * Task execution status
     */
    private ConcurrentHashMap<Long, Boolean> runningTasks = new ConcurrentHashMap<>();

    TaskActionServiceImpl() {
        serverName = "ServerA";
        consistentHash = new ConsistentHash<>(Collections.singletonList(serverName));

    }

    @Scheduled(fixedDelay = 50)
    public void pollTaskAction() {
        List<TaskAction> taskActionList = this.list();
        for (TaskAction taskAction : taskActionList) {
            long taskId = taskAction.getId();
            if (runningTasks.putIfAbsent(taskId, true) == null) {
                taskActionExecutor.execute(() -> {
                    try {
                        // Execute task action
                        executeTaskAction(taskAction);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        runningTasks.remove(taskAction.getId());
                    }
                });
            }
        }
    }

    /**
     * This interface is responsible for polling the database to retrieve task records and execute the corresponding operations.
     * @param taskAction TaskAction
     */
    @Override
    public void executeTaskAction(TaskAction taskAction) throws Exception {
        // Execute task action
        log.info("Execute task action: {}", taskAction);
        Application appParam = getAppByTaskAction(taskAction);
        switch (taskAction.getAction()) {
            case START:
                applicationActionService.start(appParam, taskAction.getAutoStart());
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
                log.error("Unsupported task action: {}", taskAction.getAction());
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
     * Determine whether the task is processed locally
     *
     * @param appId Long
     * @return boolean
     */
    @Override
    public boolean isLocalProcessing(Long appId) {
        return consistentHash.get(appId).equals(serverName);
    }

    /**
     * Save task action
     *
     * @param appParam  Application
     * @param autoStart boolean
     * @param action It may be one of the following values: START, RESTART, REVOKE, CANCEL, ABORT
     */
    @Override
    public void saveTaskAction(Application appParam, boolean autoStart, TaskActionEnum action) {
        TaskAction taskAction = getTaskActionByApp(appParam, autoStart, action);
        this.save(taskAction);
    }

    private TaskAction getTaskActionByApp(Application appParam, boolean autoStart, TaskActionEnum action) {
        TaskAction taskAction = new TaskAction();
        taskAction.setAction(action);
        taskAction.setAppId(appParam.getId());
        taskAction.setAutoStart(autoStart);
        taskAction.setArgs(appParam.getArgs());
        taskAction.setDynamicProperties(appParam.getDynamicProperties());
        taskAction.setSavepointPath(appParam.getSavepointPath());
        taskAction.setRestoreOrTriggerSavepoint(appParam.getRestoreOrTriggerSavepoint());
        taskAction.setDrain(appParam.getDrain());
        taskAction.setNativeFormat(appParam.getNativeFormat());
        taskAction.setRestoreMode(appParam.getRestoreMode());
        return taskAction;
    }

    private Application getAppByTaskAction(TaskAction taskAction) {
        Application appParam = new Application();
        appParam.setId(taskAction.getAppId());
        appParam.setArgs(taskAction.getArgs());
        appParam.setDynamicProperties(taskAction.getDynamicProperties());
        appParam.setSavepointPath(taskAction.getSavepointPath());
        appParam.setRestoreOrTriggerSavepoint(taskAction.getRestoreOrTriggerSavepoint());
        appParam.setDrain(taskAction.getDrain());
        appParam.setNativeFormat(taskAction.getNativeFormat());
        appParam.setRestoreMode(taskAction.getRestoreMode());
        return appParam;
    }
}
