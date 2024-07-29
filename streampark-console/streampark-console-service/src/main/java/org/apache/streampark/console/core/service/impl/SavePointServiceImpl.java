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

import org.apache.streampark.common.enums.FlinkExecutionMode;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.CompletableFutureUtils;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.console.base.bean.PageRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.SavePoint;
import org.apache.streampark.console.core.enums.CheckPointTypeEnum;
import org.apache.streampark.console.core.enums.OperationEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.mapper.SavePointMapper;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.console.core.util.ServiceHelper;
import org.apache.streampark.console.core.watcher.FlinkAppHttpWatcher;
import org.apache.streampark.flink.client.FlinkClient;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.util.FlinkUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.RestOptions;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.apache.flink.configuration.CheckpointingOptions.MAX_RETAINED_CHECKPOINTS;
import static org.apache.flink.configuration.CheckpointingOptions.SAVEPOINT_DIRECTORY;
import static org.apache.streampark.common.util.PropertiesUtils.extractDynamicPropertiesAsJava;
import static org.apache.streampark.console.core.enums.CheckPointTypeEnum.CHECKPOINT;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SavePointServiceImpl extends ServiceImpl<SavePointMapper, SavePoint>
    implements
        SavePointService {

    @Autowired
    private FlinkEnvService flinkEnvService;

    @Autowired
    private ApplicationManageService applicationManageService;

    @Autowired
    private ApplicationConfigService configService;

    @Autowired
    private FlinkClusterService flinkClusterService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private FlinkAppHttpWatcher flinkAppHttpWatcher;

    @Qualifier("triggerSavepointExecutor")
    @Autowired
    private Executor executorService;

    @Override
    public void expire(Long appId) {
        SavePoint savePoint = new SavePoint();
        savePoint.setLatest(false);
        LambdaQueryWrapper<SavePoint> queryWrapper = new LambdaQueryWrapper<SavePoint>().eq(SavePoint::getAppId, appId);
        this.update(savePoint, queryWrapper);
    }

    @Override
    public boolean save(SavePoint entity) {
        this.expire(entity);
        this.expire(entity.getAppId());
        return super.save(entity);
    }

    @Override
    public SavePoint getLatest(Long id) {
        LambdaQueryWrapper<SavePoint> queryWrapper = new LambdaQueryWrapper<SavePoint>()
            .eq(SavePoint::getAppId, id)
            .eq(SavePoint::getLatest, true);
        return this.getOne(queryWrapper);
    }

    @Override
    public String getSavePointPath(Application appParam) throws Exception {
        Application application = applicationManageService.getById(appParam.getId());

        // 1) properties have the highest priority, read the properties are set: -Dstate.savepoints.dir
        String savepointPath = getSavepointFromDynamicProps(application.getDynamicProperties());
        if (StringUtils.isNotBlank(savepointPath)) {
            return savepointPath;
        }

        // Application conf configuration has the second priority. If it is a streampark|flinksql type
        // task, see if Application conf is configured when the task is defined, if checkpoints are
        // configured
        // and enabled, read `state.savepoints.dir`
        savepointPath = getSavepointFromConfig(application);
        if (StringUtils.isNotBlank(savepointPath)) {
            return savepointPath;
        }

        // 3) If the savepoint is not obtained above, try to obtain the savepoint path according to the
        // deployment type (remote|on yarn)
        // 3.1) At the remote mode, request the flink webui interface to get the savepoint path
        // 3.2) At the yarn or k8s mode, then read the savepoint in flink-conf.yml in the bound flink
        return getSavepointFromDeployLayer(application);
    }

    @Override
    public void trigger(Long appId, @Nullable String savepointPath, @Nullable Boolean nativeFormat) {
        log.info("Start to trigger savepoint for app {}", appId);
        Application application = applicationManageService.getById(appId);
        ApplicationLog applicationLog = getApplicationLog(application);
        FlinkAppHttpWatcher.addSavepoint(application.getId());

        application.setOptionState(OptionStateEnum.SAVEPOINTING.getValue());
        application.setOptionTime(new Date());
        this.applicationManageService.updateById(application);
        flinkAppHttpWatcher.init();

        FlinkEnv flinkEnv = flinkEnvService.getById(application.getVersionId());

        // infer savepoint
        TriggerSavepointRequest request = renderTriggerSavepointRequest(savepointPath, nativeFormat, application,
            flinkEnv);

        CompletableFuture<SavepointResponse> savepointFuture = CompletableFuture
            .supplyAsync(() -> FlinkClient.triggerSavepoint(request), executorService);

        handleSavepointResponseFuture(application, applicationLog, savepointFuture);
    }

    @Nonnull
    private ApplicationLog getApplicationLog(Application application) {
        ApplicationLog applicationLog = new ApplicationLog();
        applicationLog.setOptionName(OperationEnum.SAVEPOINT.getValue());
        applicationLog.setAppId(application.getId());
        applicationLog.setJobManagerUrl(application.getJobManagerUrl());
        applicationLog.setOptionTime(new Date());
        applicationLog.setYarnAppId(application.getClusterId());
        applicationLog.setUserId(ServiceHelper.getUserId());
        return applicationLog;
    }

    @Override
    public Boolean remove(Long id, Application appParam) throws InternalException {
        SavePoint savePoint = getById(id);
        try {
            if (StringUtils.isNotBlank(savePoint.getPath())) {
                appParam.getFsOperator().delete(savePoint.getPath());
            }
            return removeById(id);
        } catch (Exception e) {
            throw new InternalException(e.getMessage());
        }
    }

    @Override
    public IPage<SavePoint> getPage(SavePoint savePoint, PageRequest request) {
        request.setSortField("trigger_time");
        Page<SavePoint> page = MybatisPager.getPage(request);
        LambdaQueryWrapper<SavePoint> queryWrapper = new LambdaQueryWrapper<SavePoint>().eq(SavePoint::getAppId,
            savePoint.getAppId());
        return this.page(page, queryWrapper);
    }

    @Override
    public void remove(Application appParam) {
        Long appId = appParam.getId();

        LambdaQueryWrapper<SavePoint> queryWrapper = new LambdaQueryWrapper<SavePoint>().eq(SavePoint::getAppId, appId);
        this.remove(queryWrapper);

        try {
            appParam
                .getFsOperator()
                .delete(appParam.getWorkspace().APP_SAVEPOINTS().concat("/").concat(appId.toString()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    // private methods.

    private void handleSavepointResponseFuture(
                                               Application application,
                                               ApplicationLog applicationLog,
                                               CompletableFuture<SavepointResponse> savepointFuture) {
        CompletableFutureUtils.runTimeout(
            savepointFuture,
            10L,
            TimeUnit.MINUTES,
            savepointResponse -> {
                if (savepointResponse != null && savepointResponse.savePointDir() != null) {
                    applicationLog.setSuccess(true);
                    String savePointDir = savepointResponse.savePointDir();
                    log.info("Request savepoint successful, savepointDir: {}", savePointDir);
                }
            },
            e -> {
                log.error("Trigger savepoint for flink job failed.", e);
                String exception = ExceptionUtils.stringifyException(e);
                applicationLog.setException(exception);
                if (!(e instanceof TimeoutException)) {
                    applicationLog.setSuccess(false);
                }
            })
            .whenComplete(
                (t, e) -> {
                    applicationLogService.save(applicationLog);
                    application.setOptionState(OptionStateEnum.NONE.getValue());
                    application.setOptionTime(new Date());
                    applicationManageService.update(application);
                    flinkAppHttpWatcher.init();
                });
    }

    private String getFinalSavepointDir(@Nullable String savepointPath, Application application) {
        String result = savepointPath;
        if (StringUtils.isBlank(savepointPath)) {
            try {
                result = this.getSavePointPath(application);
            } catch (Exception e) {
                throw new ApiAlertException(
                    "Error in getting savepoint path for triggering savepoint for app "
                        + application.getId(),
                    e);
            }
        }
        return result;
    }

    @Nonnull
    private Map<String, Object> tryGetRestProps(Application application, FlinkCluster cluster) {
        Map<String, Object> properties = new HashMap<>();

        if (FlinkExecutionMode.isRemoteMode(application.getFlinkExecutionMode())) {
            AssertUtils.notNull(
                cluster,
                String.format(
                    "The clusterId=%s cannot be find, maybe the clusterId is wrong or the cluster has been deleted. Please contact the Admin.",
                    application.getFlinkClusterId()));
            URI activeAddress = cluster.getRemoteURI();
            properties.put(RestOptions.ADDRESS.key(), activeAddress.getHost());
            properties.put(RestOptions.PORT.key(), activeAddress.getPort());
        }
        return properties;
    }

    private String getClusterId(Application application, FlinkCluster cluster) {
        if (FlinkExecutionMode.isKubernetesMode(application.getExecutionMode())) {
            return FlinkExecutionMode.isKubernetesSessionMode(application.getExecutionMode())
                ? cluster.getClusterId()
                : application.getClusterId();
        } else if (FlinkExecutionMode.isYarnMode(application.getExecutionMode())) {
            if (FlinkExecutionMode.YARN_SESSION.equals(application.getFlinkExecutionMode())) {
                AssertUtils.notNull(
                    cluster,
                    String.format(
                        "The yarn session clusterId=%s cannot be find, maybe the clusterId is wrong or the cluster has been deleted. Please contact the Admin.",
                        application.getFlinkClusterId()));
                return cluster.getClusterId();
            } else {
                return application.getClusterId();
            }
        }
        return null;
    }

    /**
     * Try to get the savepoint config item from the dynamic properties.
     *
     * @param dynamicProps dynamic properties string.
     * @return the value of the savepoint in the dynamic properties.
     */
    @VisibleForTesting
    @Nullable
    public String getSavepointFromDynamicProps(String dynamicProps) {
        return extractDynamicPropertiesAsJava(dynamicProps).get(SAVEPOINT_DIRECTORY.key());
    }

    /**
     * Try to obtain the savepoint path If it is a streampark|flinksql type task. See if Application
     * conf is configured when the task is defined, if checkpoints are configured and enabled, read
     * `state.savepoints.dir`.
     *
     * @param application the target application.
     * @return the value of the savepoint if existed.
     */
    @VisibleForTesting
    @Nullable
    public String getSavepointFromConfig(Application application) {
        if (!application.isStreamParkJob() && !application.isFlinkSqlJob()) {
            return null;
        }
        ApplicationConfig applicationConfig = configService.getEffective(application.getId());
        if (applicationConfig == null) {
            return null;
        }
        Map<String, String> configMap = applicationConfig.readConfig();
        return FlinkUtils.isCheckpointEnabled(configMap)
            ? configMap.get(SAVEPOINT_DIRECTORY.key())
            : null;
    }

    /**
     * Try to obtain the savepoint path according to the eployment type (remote|on yarn). At the
     * remote mode, request the flink webui interface to get the savepoint path At the yarn or k8s
     * mode, then read the savepoint in flink-conf.yml in the bound flink
     *
     * @param application the target application.
     * @return the value of the savepoint if existed.
     */
    @VisibleForTesting
    @Nullable
    public String getSavepointFromDeployLayer(Application application) throws JsonProcessingException {
        // At the yarn or k8s mode, then read the savepoint in flink-conf.yml in the bound flink
        if (!FlinkExecutionMode.isRemoteMode(application.getExecutionMode())) {
            FlinkEnv flinkEnv = flinkEnvService.getById(application.getVersionId());
            return flinkEnv.convertFlinkYamlAsMap().get(SAVEPOINT_DIRECTORY.key());
        }

        // At the remote mode, request the flink webui interface to get the savepoint path
        FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
        AssertUtils.notNull(
            cluster,
            String.format(
                "The clusterId=%s cannot be find, maybe the clusterId is wrong or "
                    + "the cluster has been deleted. Please contact the Admin.",
                application.getFlinkClusterId()));
        Map<String, String> config = cluster.getFlinkConfig();
        return config.isEmpty() ? null : config.get(SAVEPOINT_DIRECTORY.key());
    }

    /** Try get the 'state.checkpoints.num-retained' from the dynamic properties. */
    private Optional<Integer> tryGetChkNumRetainedFromDynamicProps(String dynamicProps) {
        String rawCfgValue = extractDynamicPropertiesAsJava(dynamicProps).get(MAX_RETAINED_CHECKPOINTS.key());
        if (StringUtils.isBlank(rawCfgValue)) {
            return Optional.empty();
        }
        try {
            int value = Integer.parseInt(rawCfgValue.trim());
            if (value > 0) {
                return Optional.of(value);
            }
            log.warn(
                "This value of dynamicProperties key: state.checkpoints.num-retained is invalid, must be greater than 0");
        } catch (NumberFormatException e) {
            log.error(
                "This value of dynamicProperties key: state.checkpoints.num-retained invalid, must be number");
        }
        return Optional.empty();
    }

    /** Try get the 'state.checkpoints.num-retained' from the flink env. */
    private int getChkNumRetainedFromFlinkEnv(
                                              @Nonnull FlinkEnv flinkEnv, @Nonnull Application application) {
        String flinkConfNumRetained = flinkEnv.convertFlinkYamlAsMap().get(MAX_RETAINED_CHECKPOINTS.key());
        if (StringUtils.isBlank(flinkConfNumRetained)) {
            log.info(
                "The application: {} is not set {} in dynamicProperties or value is invalid, and flink-conf.yaml is the same problem of flink env: {}, default value: {} will be use.",
                application.getJobName(),
                MAX_RETAINED_CHECKPOINTS.key(),
                flinkEnv.getFlinkHome(),
                MAX_RETAINED_CHECKPOINTS.defaultValue());
            return MAX_RETAINED_CHECKPOINTS.defaultValue();
        }
        try {
            int value = Integer.parseInt(flinkConfNumRetained.trim());
            if (value > 0) {
                return value;
            }
            log.warn(
                "The value of key: state.checkpoints.num-retained in flink-conf.yaml is invalid, must be greater than 0, default value: {} will be used",
                MAX_RETAINED_CHECKPOINTS.defaultValue());
        } catch (NumberFormatException e) {
            log.error(
                "The value of key: state.checkpoints.num-retained in flink-conf.yaml is invalid, must be number, flink env: {}, default value: {} will be used",
                flinkEnv.getFlinkHome(),
                flinkConfNumRetained);
        }
        return MAX_RETAINED_CHECKPOINTS.defaultValue();
    }

    private void expire(SavePoint entity) {
        FlinkEnv flinkEnv = flinkEnvService.getByAppId(entity.getAppId());
        Application application = applicationManageService.getById(entity.getAppId());
        AssertUtils.notNull(flinkEnv);
        AssertUtils.notNull(application);

        int cpThreshold = tryGetChkNumRetainedFromDynamicProps(application.getDynamicProperties())
            .orElse(getChkNumRetainedFromFlinkEnv(flinkEnv, application));
        cpThreshold = CHECKPOINT == CheckPointTypeEnum.of(entity.getType()) ? cpThreshold - 1 : cpThreshold;

        if (cpThreshold == 0) {
            LambdaQueryWrapper<SavePoint> queryWrapper = new LambdaQueryWrapper<SavePoint>()
                .eq(SavePoint::getAppId, entity.getAppId())
                .eq(SavePoint::getType, CHECKPOINT.get());
            this.remove(queryWrapper);
            return;
        }

        LambdaQueryWrapper<SavePoint> queryWrapper = new LambdaQueryWrapper<SavePoint>()
            .select(SavePoint::getTriggerTime)
            .eq(SavePoint::getAppId, entity.getAppId())
            .eq(SavePoint::getType, CHECKPOINT.get())
            .orderByDesc(SavePoint::getTriggerTime);

        Page<SavePoint> savePointPage = this.baseMapper.selectPage(new Page<>(1, cpThreshold + 1), queryWrapper);
        if (CollectionUtils.isEmpty(savePointPage.getRecords())
            || savePointPage.getRecords().size() <= cpThreshold) {
            return;
        }
        SavePoint savePoint = savePointPage.getRecords().get(cpThreshold - 1);
        LambdaQueryWrapper<SavePoint> lambdaQueryWrapper = new LambdaQueryWrapper<SavePoint>()
            .eq(SavePoint::getAppId, entity.getAppId())
            .eq(SavePoint::getType, CHECKPOINT.get())
            .lt(SavePoint::getTriggerTime, savePoint.getTriggerTime());
        this.remove(lambdaQueryWrapper);
    }

    @Nonnull
    private TriggerSavepointRequest renderTriggerSavepointRequest(
                                                                  @Nullable String savepointPath,
                                                                  Boolean nativeFormat,
                                                                  Application application,
                                                                  FlinkEnv flinkEnv) {
        String customSavepoint = this.getFinalSavepointDir(savepointPath, application);

        FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
        String clusterId = getClusterId(application, cluster);

        Map<String, Object> properties = this.tryGetRestProps(application, cluster);

        return new TriggerSavepointRequest(
            application.getId(),
            flinkEnv.getFlinkVersion(),
            application.getFlinkExecutionMode(),
            properties,
            clusterId,
            application.getJobId(),
            customSavepoint,
            nativeFormat,
            application.getK8sNamespace());
    }
}
