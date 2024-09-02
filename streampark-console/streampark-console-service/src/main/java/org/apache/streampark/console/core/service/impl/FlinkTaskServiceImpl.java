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
import org.apache.streampark.console.core.entity.FlinkTask;
import org.apache.streampark.console.core.enums.FlinkTaskEnum;
import org.apache.streampark.console.core.mapper.FlinkTaskMapper;
import org.apache.streampark.console.core.service.FlinkTaskService;
import org.apache.streampark.console.core.service.application.ApplicationActionService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkTaskServiceImpl extends ServiceImpl<FlinkTaskMapper, FlinkTask> implements FlinkTaskService {

    /**
     * Server name
     */
    private String serverName;

    /**
     * Consistent hash algorithm for task distribution
     */
    private ConsistentHash<String> consistentHash;

    @Qualifier("streamparkFlinkTaskExecutor")
    @Autowired
    private Executor flinkTaskExecutor;

    @Autowired
    private ApplicationActionService applicationActionService;

    /**
     * Task execution status
     */
    private ConcurrentHashMap<Long, Boolean> runningTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        serverName = "ServerA";
        consistentHash = new ConsistentHash<>(Collections.singletonList(serverName));

    }

    @Scheduled(fixedDelay = 50)
    public void pollFinkTask() {
        List<FlinkTask> flinkTaskList = this.list();
        for (FlinkTask flinkTask : flinkTaskList) {
            long taskId = flinkTask.getId();
            if (runningTasks.putIfAbsent(taskId, true) == null) {
                flinkTaskExecutor.execute(() -> {
                    try {
                        // Execute flink task
                        executeFlinkTask(flinkTask);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        runningTasks.remove(flinkTask.getId());
                    }
                });
            }
        }
    }

    /**
     * This interface is responsible for polling the database to retrieve task records and execute the corresponding operations.
     * @param flinkTask FlinkTask
     */
    @Override
    public void executeFlinkTask(FlinkTask flinkTask) throws Exception {
        // Execute flink task
        log.info("Execute flink task: {}", flinkTask);
        Application appParam = getAppByTask(flinkTask);
        switch (flinkTask.getAction()) {
            case START:
                applicationActionService.start(appParam, flinkTask.getAutoStart());
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
                log.error("Unsupported task: {}", flinkTask.getAction());
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
     * Save flink task
     *
     * @param appParam  Application
     * @param autoStart boolean
     * @param action It may be one of the following values: START, RESTART, REVOKE, CANCEL, ABORT
     */
    @Override
    public void saveFlinkTask(Application appParam, boolean autoStart, FlinkTaskEnum action) {
        FlinkTask flinkTask = getTaskByApp(appParam, autoStart, action);
        this.save(flinkTask);
    }

    private FlinkTask getTaskByApp(Application appParam, boolean autoStart, FlinkTaskEnum action) {
        FlinkTask flinkTask = new FlinkTask();
        flinkTask.setAction(action);
        flinkTask.setAppId(appParam.getId());
        flinkTask.setAutoStart(autoStart);
        flinkTask.setArgs(appParam.getArgs());
        flinkTask.setDynamicProperties(appParam.getDynamicProperties());
        flinkTask.setSavepointPath(appParam.getSavepointPath());
        flinkTask.setRestoreOrTriggerSavepoint(appParam.getRestoreOrTriggerSavepoint());
        flinkTask.setDrain(appParam.getDrain());
        flinkTask.setNativeFormat(appParam.getNativeFormat());
        flinkTask.setRestoreMode(appParam.getRestoreMode());
        return flinkTask;
    }

    private Application getAppByTask(FlinkTask flinkTask) {
        Application appParam = new Application();
        appParam.setId(flinkTask.getAppId());
        appParam.setArgs(flinkTask.getArgs());
        appParam.setDynamicProperties(flinkTask.getDynamicProperties());
        appParam.setSavepointPath(flinkTask.getSavepointPath());
        appParam.setRestoreOrTriggerSavepoint(flinkTask.getRestoreOrTriggerSavepoint());
        appParam.setDrain(flinkTask.getDrain());
        appParam.setNativeFormat(flinkTask.getNativeFormat());
        appParam.setRestoreMode(flinkTask.getRestoreMode());
        return appParam;
    }
}
