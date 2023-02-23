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

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.CompletableFutureUtils;
import org.apache.streampark.common.util.FlinkUtils;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.domain.Constant;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.CommonUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.SavePoint;
import org.apache.streampark.console.core.enums.CheckPointType;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.mapper.SavePointMapper;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.task.FlinkRESTAPIWatcher;
import org.apache.streampark.flink.client.FlinkClient;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.RestOptions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SavePointServiceImpl extends ServiceImpl<SavePointMapper, SavePoint>
    implements SavePointService {

  @Autowired private FlinkEnvService flinkEnvService;

  @Autowired private ApplicationService applicationService;

  @Autowired private ApplicationConfigService configService;

  @Autowired private FlinkClusterService flinkClusterService;

  @Autowired private ApplicationLogService applicationLogService;

  @Autowired private FlinkRESTAPIWatcher flinkRESTAPIWatcher;

  private final ExecutorService executorService =
      new ThreadPoolExecutor(
          Runtime.getRuntime().availableProcessors() * 5,
          Runtime.getRuntime().availableProcessors() * 10,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(1024),
          ThreadUtils.threadFactory("trigger-savepoint-executor"),
          new ThreadPoolExecutor.AbortPolicy());

  @Override
  public void expire(Long appId) {
    SavePoint savePoint = new SavePoint();
    savePoint.setLatest(false);
    LambdaQueryWrapper<SavePoint> queryWrapper =
        new LambdaQueryWrapper<SavePoint>().eq(SavePoint::getAppId, appId);
    this.update(savePoint, queryWrapper);
  }

  @Override
  public boolean save(SavePoint entity) {
    this.expire(entity);
    this.expire(entity.getAppId());
    return super.save(entity);
  }

  private void expire(SavePoint entity) {
    FlinkEnv flinkEnv = flinkEnvService.getByAppId(entity.getAppId());
    Application application = applicationService.getById(entity.getAppId());
    Utils.notNull(flinkEnv);
    Utils.notNull(application);

    String numRetainedKey = CheckpointingOptions.MAX_RETAINED_CHECKPOINTS.key();
    String numRetainedFromDynamicProp =
        FlinkClient.extractDynamicPropertiesAsJava(application.getDynamicProperties())
            .get(numRetainedKey);

    int cpThreshold = 0;
    if (numRetainedFromDynamicProp != null) {
      try {
        int value = Integer.parseInt(numRetainedFromDynamicProp.trim());
        if (value > 0) {
          cpThreshold = value;
        } else {
          log.warn(
              "this value of dynamicProperties key: state.checkpoints.num-retained is invalid, must be gt 0");
        }
      } catch (NumberFormatException e) {
        log.warn(
            "this value of dynamicProperties key: state.checkpoints.num-retained invalid, must be number");
      }
    }

    if (cpThreshold == 0) {
      String flinkConfNumRetained = flinkEnv.convertFlinkYamlAsMap().get(numRetainedKey);
      int numRetainedDefaultValue = CheckpointingOptions.MAX_RETAINED_CHECKPOINTS.defaultValue();
      if (flinkConfNumRetained != null) {
        try {
          int value = Integer.parseInt(flinkConfNumRetained.trim());
          if (value > 0) {
            cpThreshold = value;
          } else {
            cpThreshold = numRetainedDefaultValue;
            log.warn(
                "the value of key: state.checkpoints.num-retained in flink-conf.yaml is invalid, must be gt 0, default value: {} will be use",
                numRetainedDefaultValue);
          }
        } catch (NumberFormatException e) {
          cpThreshold = numRetainedDefaultValue;
          log.warn(
              "the value of key: state.checkpoints.num-retained in flink-conf.yaml is invalid, must be number, flink env: {}, default value: {} will be use",
              flinkEnv.getFlinkHome(),
              flinkConfNumRetained);
        }
      } else {
        cpThreshold = numRetainedDefaultValue;
        log.info(
            "the application: {} is not set {} in dynamicProperties or value is invalid, and flink-conf.yaml is the same problem of flink env: {}, default value: {} will be use.",
            application.getJobName(),
            numRetainedKey,
            flinkEnv.getFlinkHome(),
            numRetainedDefaultValue);
      }
    }

    if (CheckPointType.CHECKPOINT.equals(CheckPointType.of(entity.getType()))) {
      cpThreshold = cpThreshold - 1;
    }

    if (cpThreshold == 0) {
      LambdaQueryWrapper<SavePoint> queryWrapper =
          new LambdaQueryWrapper<SavePoint>()
              .eq(SavePoint::getAppId, entity.getAppId())
              .eq(SavePoint::getType, 1);
      this.remove(queryWrapper);
    } else {
      LambdaQueryWrapper<SavePoint> queryWrapper =
          new LambdaQueryWrapper<SavePoint>()
              .select(SavePoint::getTriggerTime)
              .eq(SavePoint::getAppId, entity.getAppId())
              .eq(SavePoint::getType, CheckPointType.CHECKPOINT.get())
              .orderByDesc(SavePoint::getTriggerTime);

      Page<SavePoint> savePointPage =
          this.baseMapper.selectPage(new Page<>(1, cpThreshold + 1), queryWrapper);
      if (!savePointPage.getRecords().isEmpty()
          && savePointPage.getRecords().size() > cpThreshold) {
        SavePoint savePoint = savePointPage.getRecords().get(cpThreshold - 1);
        LambdaQueryWrapper<SavePoint> lambdaQueryWrapper =
            new LambdaQueryWrapper<SavePoint>()
                .eq(SavePoint::getAppId, entity.getAppId())
                .eq(SavePoint::getType, 1)
                .lt(SavePoint::getTriggerTime, savePoint.getTriggerTime());
        this.remove(lambdaQueryWrapper);
      }
    }
  }

  @Override
  public SavePoint getLatest(Long id) {
    LambdaQueryWrapper<SavePoint> queryWrapper =
        new LambdaQueryWrapper<SavePoint>()
            .eq(SavePoint::getAppId, id)
            .eq(SavePoint::getLatest, true);
    return this.getOne(queryWrapper);
  }

  @Override
  public String getSavePointPath(Application appParam) throws Exception {
    Application application = applicationService.getById(appParam.getId());

    // 1) properties have the highest priority, read the properties are set: -Dstate.savepoints.dir
    String savepointPath =
        FlinkClient.extractDynamicPropertiesAsJava(application.getDynamicProperties())
            .get(CheckpointingOptions.SAVEPOINT_DIRECTORY.key());

    // Application conf configuration has the second priority. If it is a streampark|flinksql type
    // task,
    // see if Application conf is configured when the task is defined, if checkpoints are configured
    // and enabled,
    // read `state.savepoints.dir`
    if (StringUtils.isBlank(savepointPath)) {
      if (application.isStreamParkJob() || application.isFlinkSqlJob()) {
        ApplicationConfig applicationConfig = configService.getEffective(application.getId());
        if (applicationConfig != null) {
          Map<String, String> map = applicationConfig.readConfig();
          if (FlinkUtils.isCheckpointEnabled(map)) {
            savepointPath = map.get(CheckpointingOptions.SAVEPOINT_DIRECTORY.key());
          }
        }
      }
    }

    // 3) If the savepoint is not obtained above, try to obtain the savepoint path according to the
    // deployment type (remote|on yarn)
    if (StringUtils.isBlank(savepointPath)) {
      // 3.1) At the remote mode, request the flink webui interface to get the savepoint path
      if (ExecutionMode.isRemoteMode(application.getExecutionMode())) {
        FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
        Utils.notNull(
            cluster,
            String.format(
                "The clusterId=%s cannot be find, maybe the clusterId is wrong or "
                    + "the cluster has been deleted. Please contact the Admin.",
                application.getFlinkClusterId()));
        Map<String, String> config = cluster.getFlinkConfig();
        if (!config.isEmpty()) {
          savepointPath = config.get(CheckpointingOptions.SAVEPOINT_DIRECTORY.key());
        }
      } else {
        // 3.2) At the yarn or k8s mode, then read the savepoint in flink-conf.yml in the bound
        // flink
        FlinkEnv flinkEnv = flinkEnvService.getById(application.getVersionId());
        savepointPath =
            flinkEnv.convertFlinkYamlAsMap().get(CheckpointingOptions.SAVEPOINT_DIRECTORY.key());
      }
    }

    return savepointPath;
  }

  @Override
  public void trigger(Long appId, @Nullable String savepointPath) {
    log.info("Start to trigger savepoint for app {}", appId);
    Application application = applicationService.getById(appId);

    FlinkRESTAPIWatcher.addSavepoint(application.getId());

    application.setOptionState(OptionState.SAVEPOINTING.getValue());
    application.setOptionTime(new Date());
    this.applicationService.updateById(application);
    flinkRESTAPIWatcher.init();

    FlinkEnv flinkEnv = flinkEnvService.getById(application.getVersionId());

    // infer savepoint
    String customSavepoint = this.getFinalSavepointDir(savepointPath, application);

    FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
    String clusterId = getClusterId(application, cluster);

    Map<String, Object> properties = this.tryGetRestProps(application, cluster);

    TriggerSavepointRequest request =
        new TriggerSavepointRequest(
            flinkEnv.getFlinkVersion(),
            application.getExecutionModeEnum(),
            clusterId,
            application.getJobId(),
            customSavepoint,
            application.getK8sNamespace(),
            properties);

    CompletableFuture<SavepointResponse> savepointFuture =
        CompletableFuture.supplyAsync(() -> FlinkClient.triggerSavepoint(request), executorService);

    handleSavepointResponseFuture(application, savepointFuture);
  }

  private void handleSavepointResponseFuture(
      Application application, CompletableFuture<SavepointResponse> savepointFuture) {
    CompletableFutureUtils.runTimeout(
            savepointFuture,
            10L,
            TimeUnit.MINUTES,
            savepointResponse -> {
              if (savepointResponse != null && savepointResponse.savePointDir() != null) {
                String savePointDir = savepointResponse.savePointDir();
                log.info("Request savepoint successful, savepointDir: {}", savePointDir);
              }
            },
            e -> {
              log.error("Trigger savepoint for flink job failed.", e);
              ApplicationLog log = new ApplicationLog();
              log.setAppId(application.getId());
              if (ExecutionMode.isYarnMode(application.getExecutionMode())) {
                log.setYarnAppId(application.getClusterId());
              }
              log.setOptionTime(new Date());
              String exception = Utils.stringifyException(e);
              log.setException(exception);
              if (!(e instanceof TimeoutException)) {
                log.setSuccess(false);
              }
              applicationLogService.save(log);
            })
        .whenComplete(
            (t, e) -> {
              application.setOptionState(OptionState.NONE.getValue());
              application.setOptionTime(new Date());
              applicationService.update(application);
              flinkRESTAPIWatcher.init();
            });
  }

  private String getFinalSavepointDir(@Nullable String savepointPath, Application application) {
    String result = savepointPath;
    if (StringUtils.isEmpty(savepointPath)) {
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

  @NotNull
  private Map<String, Object> tryGetRestProps(Application application, FlinkCluster cluster) {
    Map<String, Object> properties = new HashMap<>();

    if (ExecutionMode.isRemoteMode(application.getExecutionModeEnum())) {
      Utils.notNull(
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
    if (ExecutionMode.isKubernetesMode(application.getExecutionMode())) {
      return application.getClusterId();
    } else if (ExecutionMode.isYarnMode(application.getExecutionMode())) {
      if (ExecutionMode.YARN_SESSION.equals(application.getExecutionModeEnum())) {
        Utils.notNull(
            cluster,
            String.format(
                "The yarn session clusterId=%s cannot be find, maybe the clusterId is wrong or the cluster has been deleted. Please contact the Admin.",
                application.getFlinkClusterId()));
        return cluster.getClusterId();
      } else {
        return application.getAppId();
      }
    }
    return null;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean delete(Long id, Application application) throws InternalException {
    SavePoint savePoint = getById(id);
    try {
      if (CommonUtils.notEmpty(savePoint.getPath())) {
        application.getFsOperator().delete(savePoint.getPath());
      }
      removeById(id);
      return true;
    } catch (Exception e) {
      throw new InternalException(e.getMessage());
    }
  }

  @Override
  public IPage<SavePoint> page(SavePoint savePoint, RestRequest request) {
    Page<SavePoint> page =
        new MybatisPager<SavePoint>().getPage(request, "trigger_time", Constant.ORDER_DESC);
    LambdaQueryWrapper<SavePoint> queryWrapper =
        new LambdaQueryWrapper<SavePoint>().eq(SavePoint::getAppId, savePoint.getAppId());
    return this.page(page, queryWrapper);
  }

  @Override
  public void removeApp(Application application) {
    Long appId = application.getId();

    LambdaQueryWrapper<SavePoint> queryWrapper =
        new LambdaQueryWrapper<SavePoint>().eq(SavePoint::getAppId, appId);
    this.remove(queryWrapper);

    try {
      application
          .getFsOperator()
          .delete(application.getWorkspace().APP_SAVEPOINTS().concat("/").concat(appId.toString()));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
