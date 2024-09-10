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
import org.apache.streampark.console.core.entity.DistributionTask;
import org.apache.streampark.console.core.enums.DistributionTaskEnum;
import org.apache.streampark.console.core.enums.EngineTypeEnum;
import org.apache.streampark.console.core.mapper.DistributionTaskMapper;
import org.apache.streampark.console.core.service.DistributionTaskService;
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
public class DistributionTaskServiceImpl extends ServiceImpl<DistributionTaskMapper, DistributionTask>
    implements
        DistributionTaskService {

    /**
     * Server name
     */
    private String serverName;

    /**
     * Consistent hash algorithm for task distribution
     */
    private final ConsistentHash<String> consistentHash = new ConsistentHash<>(Collections.emptyList());

    @Qualifier("streamparkDistributionTaskExecutor")
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
    public void pollDistributionTask() {
        List<DistributionTask> DistributionTaskList = this.list();
        for (DistributionTask DistributionTask : DistributionTaskList) {
            long taskId = DistributionTask.getId();
            if (DistributionTask.getEngineType() != EngineTypeEnum.FLINK || !isLocalProcessing(taskId)) {
                continue;
            }
            if (runningTasks.putIfAbsent(taskId, true) == null) {
                taskExecutor.execute(() -> {
                    try {
                        // Execute Distribution task
                        executeDistributionTask(DistributionTask);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        runningTasks.remove(DistributionTask.getId());
                        this.removeById(DistributionTask.getId());
                    }
                });
            }
        }
    }

    /**
     * This interface is responsible for polling the database to retrieve task records and execute the corresponding operations.
     * @param DistributionTask DistributionTask
     */
    @Override
    public void executeDistributionTask(DistributionTask DistributionTask) throws Exception {
        // Execute Distribution task
        log.info("Execute Distribution task: {}", DistributionTask);
        FlinkTaskItem flinkTaskItem = getFlinkTaskItem(DistributionTask);
        Application appParam = getAppByFlinkTaskItem(flinkTaskItem);
        switch (DistributionTask.getAction()) {
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
                log.error("Unsupported task: {}", DistributionTask.getAction());
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
     * Save Distribution task.
     *
     * @param appParam  Application
     * @param autoStart boolean
     * @param action It may be one of the following values: START, RESTART, REVOKE, CANCEL, ABORT
     */
    @Override
    public void saveDistributionTask(Application appParam, boolean autoStart, DistributionTaskEnum action) {
        try {
            DistributionTask DistributionTask = getDistributionTaskByApp(appParam, autoStart, action);
            this.save(DistributionTask);
        } catch (JsonProcessingException e) {
            log.error("Failed to save Distribution task: {}", e.getMessage());
        }
    }

    public DistributionTask getDistributionTaskByApp(Application appParam, boolean autoStart,
                                                     DistributionTaskEnum action) throws JsonProcessingException {
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

        DistributionTask distributionTask = new DistributionTask();
        distributionTask.setAction(action);
        distributionTask.setEngineType(EngineTypeEnum.FLINK);
        distributionTask.setProperties(JacksonUtils.write(flinkTaskItem));
        return distributionTask;
    }

    public FlinkTaskItem getFlinkTaskItem(DistributionTask DistributionTask) throws JsonProcessingException {
        return JacksonUtils.read(DistributionTask.getProperties(), FlinkTaskItem.class);
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
