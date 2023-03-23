package org.apache.streampark.console.core.service.application.impl;

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.CommonUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.entity.SavePoint;
import org.apache.streampark.console.core.enums.AppExistsState;
import org.apache.streampark.console.core.enums.CandidateType;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.mapper.ApplicationMapper;
import org.apache.streampark.console.core.metrics.flink.JobsOverview;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ProjectService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.application.QueryApplicationInfoService;
import org.apache.streampark.console.core.service.application.ValidateApplicationService;
import org.apache.streampark.console.core.task.FlinkRESTAPIWatcher;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.Bridge.toTrackId;
import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.isKubernetesApp;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class QueryApplicationInfoServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements QueryApplicationInfoService {

  private static final Pattern JOB_NAME_PATTERN =
      Pattern.compile("^[.\\x{4e00}-\\x{9fa5}A-Za-z\\d_\\-\\s]+$");

  private static final Pattern SINGLE_SPACE_PATTERN = Pattern.compile("^\\S+(\\s\\S+)*$");

  @Autowired @Lazy private ProjectService projectService;
  @Autowired private ApplicationConfigService configService;
  @Autowired private ValidateApplicationService validateApplicationService;
  @Autowired private FlinkSqlService flinkSqlService;
  @Autowired private SavePointService savePointService;
  @Autowired private FlinkK8sWatcher k8SFlinkTrackMonitor;

  @Override
  public Map<String, Serializable> dashboard(Long teamId) {
    JobsOverview.Task overview = new JobsOverview.Task();
    Integer totalJmMemory = 0;
    Integer totalTmMemory = 0;
    Integer totalTm = 0;
    Integer totalSlot = 0;
    Integer availableSlot = 0;
    Integer runningJob = 0;

    // stat metrics from other than kubernetes mode
    for (Application app : FlinkRESTAPIWatcher.getWatchingApps()) {
      if (!teamId.equals(app.getTeamId())) {
        continue;
      }
      if (app.getJmMemory() != null) {
        totalJmMemory += app.getJmMemory();
      }
      if (app.getTmMemory() != null) {
        totalTmMemory += app.getTmMemory() * (app.getTotalTM() == null ? 1 : app.getTotalTM());
      }
      if (app.getTotalTM() != null) {
        totalTm += app.getTotalTM();
      }
      if (app.getTotalSlot() != null) {
        totalSlot += app.getTotalSlot();
      }
      if (app.getAvailableSlot() != null) {
        availableSlot += app.getAvailableSlot();
      }
      if (app.getState() == FlinkAppState.RUNNING.getValue()) {
        runningJob++;
      }
      JobsOverview.Task task = app.getOverview();
      if (task != null) {
        overview.setTotal(overview.getTotal() + task.getTotal());
        overview.setCreated(overview.getCreated() + task.getCreated());
        overview.setScheduled(overview.getScheduled() + task.getScheduled());
        overview.setDeploying(overview.getDeploying() + task.getDeploying());
        overview.setRunning(overview.getRunning() + task.getRunning());
        overview.setFinished(overview.getFinished() + task.getFinished());
        overview.setCanceling(overview.getCanceling() + task.getCanceling());
        overview.setCanceled(overview.getCanceled() + task.getCanceled());
        overview.setFailed(overview.getFailed() + task.getFailed());
        overview.setReconciling(overview.getReconciling() + task.getReconciling());
      }
    }

    // merge metrics from flink kubernetes cluster
    FlinkMetricCV k8sMetric = k8SFlinkTrackMonitor.getAccGroupMetrics(teamId.toString());
    if (k8sMetric != null) {
      totalJmMemory += k8sMetric.totalJmMemory();
      totalTmMemory += k8sMetric.totalTmMemory();
      totalTm += k8sMetric.totalTm();
      totalSlot += k8sMetric.totalSlot();
      availableSlot += k8sMetric.availableSlot();
      runningJob += k8sMetric.runningJob();
      overview.setTotal(overview.getTotal() + k8sMetric.totalJob());
      overview.setRunning(overview.getRunning() + k8sMetric.runningJob());
      overview.setFinished(overview.getFinished() + k8sMetric.finishedJob());
      overview.setCanceled(overview.getCanceled() + k8sMetric.cancelledJob());
      overview.setFailed(overview.getFailed() + k8sMetric.failedJob());
    }

    // result json
    Map<String, Serializable> map = new HashMap<>(8);
    map.put("task", overview);
    map.put("jmMemory", totalJmMemory);
    map.put("tmMemory", totalTmMemory);
    map.put("totalTM", totalTm);
    map.put("availableSlot", availableSlot);
    map.put("totalSlot", totalSlot);
    map.put("runningJob", runningJob);

    return map;
  }

  @Override
  public IPage<Application> page(Application appParam, RestRequest request) {
    if (appParam.getTeamId() == null) {
      return null;
    }
    Page<Application> page = new MybatisPager<Application>().getDefaultPage(request);
    if (CommonUtils.notEmpty(appParam.getStateArray())) {
      if (Arrays.stream(appParam.getStateArray())
          .anyMatch(x -> x == FlinkAppState.FINISHED.getValue())) {
        Integer[] newArray =
            CommonUtils.arrayInsertIndex(
                appParam.getStateArray(),
                appParam.getStateArray().length,
                FlinkAppState.POS_TERMINATED.getValue());
        appParam.setStateArray(newArray);
      }
    }
    this.baseMapper.page(page, appParam);
    List<Application> records = page.getRecords();
    long now = System.currentTimeMillis();
    List<Application> newRecords =
        records.stream()
            .peek(
                record -> {
                  // status of flink job on kubernetes mode had been automatically persisted to db
                  // in time.
                  if (isKubernetesApp(record)) {
                    // set duration
                    String restUrl = k8SFlinkTrackMonitor.getRemoteRestUrl(toTrackId(record));
                    record.setFlinkRestUrl(restUrl);
                    if (record.getTracking() == 1
                        && record.getStartTime() != null
                        && record.getStartTime().getTime() > 0) {
                      record.setDuration(now - record.getStartTime().getTime());
                    }
                  }
                })
            .collect(Collectors.toList());
    page.setRecords(newRecords);
    return page;
  }

  /** Check if the current jobName and other key identifiers already exist in db and yarn/k8s */
  @Override
  public AppExistsState checkExists(Application appParam) {

    if (!checkJobName(appParam.getJobName())) {
      return AppExistsState.INVALID;
    }

    boolean existsByJobName = validateApplicationService.existsByJobName(appParam.getJobName());

    if (appParam.getId() != null) {
      Application app = getById(appParam.getId());
      if (app.getJobName().equals(appParam.getJobName())) {
        return AppExistsState.NO;
      }

      if (existsByJobName) {
        return AppExistsState.IN_DB;
      }

      FlinkAppState state = FlinkAppState.of(app.getState());
      // has stopped status
      if (state.equals(FlinkAppState.ADDED)
          || state.equals(FlinkAppState.CREATED)
          || state.equals(FlinkAppState.FAILED)
          || state.equals(FlinkAppState.CANCELED)
          || state.equals(FlinkAppState.LOST)
          || state.equals(FlinkAppState.KILLED)) {
        // check whether jobName exists on yarn
        if (ExecutionMode.isYarnMode(appParam.getExecutionMode())
            && YarnUtils.isContains(appParam.getJobName())) {
          return AppExistsState.IN_YARN;
        }
        // check whether clusterId, namespace, jobId on kubernetes
        else if (ExecutionMode.isKubernetesMode(appParam.getExecutionMode())
            && k8SFlinkTrackMonitor.checkIsInRemoteCluster(toTrackId(appParam))) {
          return AppExistsState.IN_KUBERNETES;
        }
      }
    } else {
      if (existsByJobName) {
        return AppExistsState.IN_DB;
      }

      // check whether jobName exists on yarn
      if (ExecutionMode.isYarnMode(appParam.getExecutionMode())
          && YarnUtils.isContains(appParam.getJobName())) {
        return AppExistsState.IN_YARN;
      }
      // check whether clusterId, namespace, jobId on kubernetes
      else if (ExecutionMode.isKubernetesMode(appParam.getExecutionMode())
          && k8SFlinkTrackMonitor.checkIsInRemoteCluster(toTrackId(appParam))) {
        return AppExistsState.IN_KUBERNETES;
      }
    }
    return AppExistsState.NO;
  }

  @Override
  public List<Application> getByProjectId(Long id) {
    return baseMapper.getByProjectId(id);
  }

  @Override
  public List<Application> getByTeamId(Long teamId) {
    return baseMapper.getByTeamId(teamId);
  }

  @Override
  public List<Application> getByTeamIdAndExecutionModes(
      Long teamId, @Nonnull Collection<ExecutionMode> executionModes) {
    return getBaseMapper()
        .selectList(
            new LambdaQueryWrapper<Application>()
                .eq((SFunction<Application, Long>) Application::getTeamId, teamId)
                .in(
                    Application::getExecutionMode,
                    executionModes.stream()
                        .map(ExecutionMode::getMode)
                        .collect(Collectors.toSet())));
  }

  @Override
  public String readConf(Application appParam) throws IOException {
    File file = new File(appParam.getConfig());
    String conf = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(conf.getBytes());
  }

  @Override
  public Application getApp(Application appParam) {
    Application application = this.baseMapper.getApp(appParam);
    ApplicationConfig config = configService.getEffective(appParam.getId());
    config = config == null ? configService.getLatest(appParam.getId()) : config;
    if (config != null) {
      config.setToApplication(application);
    }
    if (application.isFlinkSqlJob()) {
      FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), true);
      if (flinkSql == null) {
        flinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.NEW);
        flinkSql.setSql(DeflaterUtils.unzipString(flinkSql.getSql()));
      }
      flinkSql.setToApplication(application);
    } else {
      if (application.isCICDJob()) {
        String path =
            this.projectService.getAppConfPath(application.getProjectId(), application.getModule());
        application.setConfPath(path);
      }
    }
    // add flink web url info for k8s-mode
    if (isKubernetesApp(application)) {
      String restUrl = k8SFlinkTrackMonitor.getRemoteRestUrl(toTrackId(application));
      application.setFlinkRestUrl(restUrl);

      // set duration
      long now = System.currentTimeMillis();
      if (application.getTracking() == 1
          && application.getStartTime() != null
          && application.getStartTime().getTime() > 0) {
        application.setDuration(now - application.getStartTime().getTime());
      }
    }

    application.setYarnQueueByHotParams();

    return application;
  }

  @Override
  public String getMain(Application application) {
    File jarFile;
    if (application.getProjectId() == null) {
      jarFile = new File(application.getJar());
    } else {
      Project project = new Project();
      project.setId(application.getProjectId());
      String modulePath =
          project.getDistHome().getAbsolutePath().concat("/").concat(application.getModule());
      jarFile = new File(modulePath, application.getJar());
    }
    Manifest manifest = Utils.getJarManifest(jarFile);
    return manifest.getMainAttributes().getValue("Main-Class");
  }

  @Override
  public String checkSavepointPath(Application appParam) throws Exception {
    String savepointPath = appParam.getSavePoint();
    if (StringUtils.isBlank(savepointPath)) {
      savepointPath = savePointService.getSavePointPath(appParam);
    }

    if (StringUtils.isNotBlank(savepointPath)) {
      final URI uri = URI.create(savepointPath);
      final String scheme = uri.getScheme();
      final String pathPart = uri.getPath();
      String error = null;
      if (scheme == null) {
        error =
            "This state.savepoints.dir value "
                + savepointPath
                + " scheme (hdfs://, file://, etc) of  is null. Please specify the file system scheme explicitly in the URI.";
      } else if (pathPart == null) {
        error =
            "This state.savepoints.dir value "
                + savepointPath
                + " path part to store the checkpoint data in is null. Please specify a directory path for the checkpoint data.";
      } else if (pathPart.length() == 0 || "/".equals(pathPart)) {
        error =
            "This state.savepoints.dir value "
                + savepointPath
                + " Cannot use the root directory for checkpoints.";
      }
      return error;
    } else {
      return "When custom savepoint is not set, state.savepoints.dir needs to be set in properties or flink-conf.yaml of application";
    }
  }

  @Override
  public String getSavePointed(Application appParam) {
    if (appParam.getSavePointed()) {
      if (appParam.getSavePoint() == null) {
        SavePoint savePoint = savePointService.getLatest(appParam.getId());
        if (savePoint != null) {
          return savePoint.getPath();
        }
      } else {
        return appParam.getSavePoint();
      }
    }
    return null;
  }

  private Boolean checkJobName(String jobName) {
    if (!StringUtils.isEmpty(jobName.trim())) {
      return JOB_NAME_PATTERN.matcher(jobName).matches()
          && SINGLE_SPACE_PATTERN.matcher(jobName).matches();
    }
    return false;
  }
}
