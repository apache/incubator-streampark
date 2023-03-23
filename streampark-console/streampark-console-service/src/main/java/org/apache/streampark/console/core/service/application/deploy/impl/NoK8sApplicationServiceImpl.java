package org.apache.streampark.console.core.service.application.deploy.impl;

import org.apache.streampark.common.conf.ConfigConst;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.DevelopmentMode;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.enums.ResolveOrder;
import org.apache.streampark.common.util.CompletableFutureUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.enums.ConfigFileType;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.Operation;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.mapper.ApplicationMapper;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.VariableService;
import org.apache.streampark.console.core.service.application.OpApplicationInfoService;
import org.apache.streampark.console.core.service.application.QueryApplicationInfoService;
import org.apache.streampark.console.core.service.application.deploy.YarnApplicationService;
import org.apache.streampark.console.core.task.FlinkRESTAPIWatcher;
import org.apache.streampark.flink.client.FlinkClient;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.core.conf.ParameterCli;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class NoK8sApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements YarnApplicationService {

  private final ExecutorService executorService =
      new ThreadPoolExecutor(
          Runtime.getRuntime().availableProcessors() * 5,
          Runtime.getRuntime().availableProcessors() * 10,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(1024),
          ThreadUtils.threadFactory("streampark-deploy-nok8s-executor"),
          new ThreadPoolExecutor.AbortPolicy());

  private final FlinkEnvService flinkEnvService;
  private final CommonService commonService;
  private final SavePointService savePointService;
  private final ApplicationLogService applicationLogService;
  private final AppBuildPipeService appBuildPipeService;
  private final ApplicationConfigService configService;
  private final FlinkSqlService flinkSqlService;
  private final VariableService variableService;
  private final OpApplicationInfoService opApplicationInfoService;
  private final QueryApplicationInfoService queryApplicationInfoService;
  private final FlinkClusterService flinkClusterService;

  @Override
  public void start(Application appParam, boolean auto) throws Exception {
    Map<Long, CompletableFuture<SubmitResponse>> startFutureMap =
        opApplicationInfoService.getStartFutureMap();
    final Application application = getById(appParam.getId());
    Utils.notNull(application);
    if (!application.isCanBeStart()) {
      throw new ApiAlertException("[StreamPark] The application cannot be started repeatedly.");
    }

    FlinkEnv flinkEnv = flinkEnvService.getByIdOrDefault(application.getVersionId());
    if (flinkEnv == null) {
      throw new ApiAlertException("[StreamPark] can no found flink version");
    }

    // if manually started, clear the restart flag
    if (!auto) {
      application.setRestartCount(0);
    } else {
      if (!application.isNeedRestartOnFailed()) {
        return;
      }
      appParam.setSavePointed(true);
      application.setRestartCount(application.getRestartCount() + 1);
    }

    opApplicationInfoService.starting(application);
    application.setAllowNonRestored(appParam.getAllowNonRestored());
    String savePointed = queryApplicationInfoService.getSavePointed(appParam);

    String appConf;
    String flinkUserJar = null;
    String jobId = new JobID().toHexString();
    ApplicationLog applicationLog = new ApplicationLog();
    applicationLog.setOptionName(Operation.START.getValue());
    applicationLog.setAppId(application.getId());
    applicationLog.setOptionTime(new Date());

    // set the latest to Effective, (it will only become the current effective at this time)
    opApplicationInfoService.toEffective(application);

    ApplicationConfig applicationConfig = configService.getEffective(application.getId());
    ExecutionMode executionMode = ExecutionMode.of(application.getExecutionMode());
    ApiAlertException.throwIfNull(
        executionMode, "ExecutionMode can't be null, start application failed.");
    if (application.isCustomCodeJob()) {
      if (application.isUploadJob()) {
        appConf =
            String.format(
                "json://{\"%s\":\"%s\"}",
                ConfigConst.KEY_FLINK_APPLICATION_MAIN_CLASS(), application.getMainClass());
      } else {
        switch (application.getApplicationType()) {
          case STREAMPARK_FLINK:
            ConfigFileType fileType = ConfigFileType.of(applicationConfig.getFormat());
            if (fileType != null && !fileType.equals(ConfigFileType.UNKNOWN)) {
              appConf =
                  String.format("%s://%s", fileType.getTypeName(), applicationConfig.getContent());
            } else {
              throw new IllegalArgumentException(
                  "application' config type error,must be ( yaml| properties| hocon )");
            }
            break;
          case APACHE_FLINK:
            appConf =
                String.format(
                    "json://{\"%s\":\"%s\"}",
                    ConfigConst.KEY_FLINK_APPLICATION_MAIN_CLASS(), application.getMainClass());
            break;
          default:
            throw new IllegalArgumentException(
                "[StreamPark] ApplicationType must be (StreamPark flink | Apache flink)... ");
        }
      }

      if (ExecutionMode.YARN_APPLICATION.equals(executionMode)) {
        switch (application.getApplicationType()) {
          case STREAMPARK_FLINK:
            flinkUserJar =
                String.format(
                    "%s/%s", application.getAppLib(), application.getModule().concat(".jar"));
            break;
          case APACHE_FLINK:
            flinkUserJar = String.format("%s/%s", application.getAppHome(), application.getJar());
            break;
          default:
            throw new IllegalArgumentException(
                "[StreamPark] ApplicationType must be (StreamPark flink | Apache flink)... ");
        }
      }
    } else if (application.isFlinkSqlJob()) {
      FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
      Utils.notNull(flinkSql);
      // 1) dist_userJar
      String sqlDistJar = commonService.getSqlClientJar(flinkEnv);
      // 2) appConfig
      appConf =
          applicationConfig == null
              ? null
              : String.format("yaml://%s", applicationConfig.getContent());
      // 3) client
      if (ExecutionMode.YARN_APPLICATION.equals(executionMode)) {
        String clientPath = Workspace.remote().APP_CLIENT();
        flinkUserJar = String.format("%s/%s", clientPath, sqlDistJar);
      }
    } else {
      throw new UnsupportedOperationException("Unsupported...");
    }

    Map<String, Object> extraParameter = new HashMap<>(0);
    if (application.isFlinkSqlJob()) {
      FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), true);
      // Get the sql of the replaced placeholder
      String realSql = variableService.replaceVariable(application.getTeamId(), flinkSql.getSql());
      flinkSql.setSql(DeflaterUtils.zipString(realSql));
      extraParameter.put(ConfigConst.KEY_FLINK_SQL(null), flinkSql.getSql());
    }

    AppBuildPipeline buildPipeline = appBuildPipeService.getById(application.getId());

    Utils.notNull(buildPipeline);

    BuildResult buildResult = buildPipeline.getBuildResult();
    if (ExecutionMode.YARN_APPLICATION.equals(executionMode)) {
      buildResult = new ShadedBuildResponse(null, flinkUserJar, true);
    }

    // Get the args after placeholder replacement
    String applicationArgs =
        variableService.replaceVariable(application.getTeamId(), application.getArgs());

    SubmitRequest submitRequest =
        new SubmitRequest(
            flinkEnv.getFlinkVersion(),
            ExecutionMode.of(application.getExecutionMode()),
            getProperties(application),
            flinkEnv.getFlinkConf(),
            DevelopmentMode.of(application.getJobType()),
            application.getId(),
            jobId,
            application.getJobName(),
            appConf,
            application.getApplicationType(),
            savePointed,
            applicationArgs,
            buildResult,
            null,
            extraParameter);

    CompletableFuture<SubmitResponse> future =
        CompletableFuture.supplyAsync(() -> FlinkClient.submit(submitRequest), executorService);

    startFutureMap.put(application.getId(), future);

    CompletableFutureUtils.runTimeout(
            future,
            2L,
            TimeUnit.MINUTES,
            submitResponse -> {
              if (submitResponse.flinkConfig() != null) {
                String jmMemory =
                    submitResponse.flinkConfig().get(ConfigConst.KEY_FLINK_JM_PROCESS_MEMORY());
                if (jmMemory != null) {
                  application.setJmMemory(MemorySize.parse(jmMemory).getMebiBytes());
                }
                String tmMemory =
                    submitResponse.flinkConfig().get(ConfigConst.KEY_FLINK_TM_PROCESS_MEMORY());
                if (tmMemory != null) {
                  application.setTmMemory(MemorySize.parse(tmMemory).getMebiBytes());
                }
              }
              application.setAppId(submitResponse.clusterId());
              if (StringUtils.isNoneEmpty(submitResponse.jobId())) {
                application.setJobId(submitResponse.jobId());
              }

              if (StringUtils.isNoneEmpty(submitResponse.jobManagerUrl())) {
                application.setJobManagerUrl(submitResponse.jobManagerUrl());
                applicationLog.setJobManagerUrl(submitResponse.jobManagerUrl());
              }
              applicationLog.setYarnAppId(submitResponse.clusterId());
              application.setStartTime(new Date());
              application.setEndTime(null);
              updateById(application);

              // if start completed, will be added task to tracking queue
              FlinkRESTAPIWatcher.setOptionState(appParam.getId(), OptionState.STARTING);
              FlinkRESTAPIWatcher.doWatching(application);

              applicationLog.setSuccess(true);
              // set savepoint to expire
              savePointService.expire(application.getId());
            },
            e -> {
              if (e.getCause() instanceof CancellationException) {
                opApplicationInfoService.updateToStopped(application);
              } else {
                String exception = Utils.stringifyException(e);
                applicationLog.setException(exception);
                applicationLog.setSuccess(false);
                Application app = getById(appParam.getId());
                app.setState(FlinkAppState.FAILED.getValue());
                app.setOptionState(OptionState.NONE.getValue());
                updateById(app);

                FlinkRESTAPIWatcher.unWatching(appParam.getId());
              }
            })
        .whenComplete(
            (t, e) -> {
              applicationLogService.save(applicationLog);
              startFutureMap.remove(application.getId());
            });
  }

  @Override
  public void restart(Application application) throws Exception {
    cancel(application);
    start(application, false);
  }

  @Override
  public void cancel(Application application) throws Exception {}

  @Override
  public String getYarnName(Application application) {
    String[] args = new String[2];
    args[0] = "--name";
    args[1] = application.getConfig();
    return ParameterCli.read(args);
  }

  private Map<String, Object> getProperties(Application application) {
    Map<String, Object> properties = application.getOptionMap();
    if (ExecutionMode.isRemoteMode(application.getExecutionModeEnum())) {
      FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
      ApiAlertException.throwIfNull(
          cluster,
          String.format(
              "The clusterId=%s can't be find, maybe the clusterId is wrong or "
                  + "the cluster has been deleted. Please contact the Admin.",
              application.getFlinkClusterId()));
      URI activeAddress = cluster.getRemoteURI();
      properties.put(RestOptions.ADDRESS.key(), activeAddress.getHost());
      properties.put(RestOptions.PORT.key(), activeAddress.getPort());
    } else if (ExecutionMode.isYarnMode(application.getExecutionModeEnum())) {
      if (ExecutionMode.YARN_SESSION.equals(application.getExecutionModeEnum())) {
        FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
        ApiAlertException.throwIfNull(
            cluster,
            String.format(
                "The yarn session clusterId=%s cannot be find, maybe the clusterId is wrong or "
                    + "the cluster has been deleted. Please contact the Admin.",
                application.getFlinkClusterId()));
        properties.put(ConfigConst.KEY_YARN_APP_ID(), cluster.getClusterId());
      } else {
        String yarnQueue =
            (String) application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_QUEUE());
        String yarnLabelExpr =
            (String) application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_NODE_LABEL());
        Optional.ofNullable(yarnQueue)
            .ifPresent(yq -> properties.put(ConfigConst.KEY_YARN_APP_QUEUE(), yq));
        Optional.ofNullable(yarnLabelExpr)
            .ifPresent(yLabel -> properties.put(ConfigConst.KEY_YARN_APP_NODE_LABEL(), yLabel));
      }
    }
    if (application.getAllowNonRestored()) {
      properties.put(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.key(), true);
    }

    Map<String, String> dynamicProperties =
        PropertiesUtils.extractDynamicPropertiesAsJava(application.getDynamicProperties());
    properties.putAll(dynamicProperties);
    ResolveOrder resolveOrder = ResolveOrder.of(application.getResolveOrder());
    if (resolveOrder != null) {
      properties.put(CoreOptions.CLASSLOADER_RESOLVE_ORDER.key(), resolveOrder.getName());
    }

    return properties;
  }
}
