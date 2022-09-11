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

import static org.apache.streampark.console.core.task.K8sFlinkTrackMonitorWrapper.Bridge.toTrackId;
import static org.apache.streampark.console.core.task.K8sFlinkTrackMonitorWrapper.isKubernetesApp;

import org.apache.streampark.common.conf.ConfigConst;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.domain.FlinkMemorySize;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.DevelopmentMode;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.enums.ResolveOrder;
import org.apache.streampark.common.enums.StorageType;
import org.apache.streampark.common.fs.HdfsOperator;
import org.apache.streampark.common.util.CompletableFutureUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.CommonUtils;
import org.apache.streampark.console.base.util.ObjectUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.annotation.RefreshCache;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.entity.SavePoint;
import org.apache.streampark.console.core.enums.AppExistsState;
import org.apache.streampark.console.core.enums.CandidateType;
import org.apache.streampark.console.core.enums.ChangedType;
import org.apache.streampark.console.core.enums.CheckPointType;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.LaunchState;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.mapper.ApplicationMapper;
import org.apache.streampark.console.core.metrics.flink.JobsOverview;
import org.apache.streampark.console.core.runner.EnvInitializer;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.EffectiveService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ProjectService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.SettingService;
import org.apache.streampark.console.core.task.FlinkTrackingTask;
import org.apache.streampark.flink.core.conf.ParameterCli;
import org.apache.streampark.flink.kubernetes.IngressController;
import org.apache.streampark.flink.kubernetes.K8sFlinkTrackMonitor;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;
import org.apache.streampark.flink.submit.FlinkSubmitter;
import org.apache.streampark.flink.submit.bean.CancelRequest;
import org.apache.streampark.flink.submit.bean.CancelResponse;
import org.apache.streampark.flink.submit.bean.KubernetesSubmitParam;
import org.apache.streampark.flink.submit.bean.SubmitRequest;
import org.apache.streampark.flink.submit.bean.SubmitResponse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements ApplicationService {

    private final Map<Long, Long> tailOutMap = new ConcurrentHashMap<>();

    private final Map<Long, Boolean> tailBeginning = new ConcurrentHashMap<>();

    private final ExecutorService executorService = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 5,
        Runtime.getRuntime().availableProcessors() * 10,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        ThreadUtils.threadFactory("streampark-deploy-executor"),
        new ThreadPoolExecutor.AbortPolicy()
    );

    private static final Pattern JOB_NAME_PATTERN = Pattern.compile("^[.\\x{4e00}-\\x{9fa5}A-Za-z\\d_\\-\\s]+$");

    private static final Pattern SINGLE_SPACE_PATTERN = Pattern.compile("^\\S+(\\s\\S+)*$");

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private ApplicationConfigService configService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private FlinkEnvService flinkEnvService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private SavePointService savePointService;

    @Autowired
    private EffectiveService effectiveService;

    @Autowired
    private SettingService settingService;

    @Autowired
    private CommonService commonService;

    @Autowired
    private EnvInitializer envInitializer;

    @Autowired
    private K8sFlinkTrackMonitor k8SFlinkTrackMonitor;

    @Autowired
    private AppBuildPipeService appBuildPipeService;

    @Autowired
    private FlinkClusterService flinkClusterService;

    @PostConstruct
    public void resetOptionState() {
        this.baseMapper.resetOptionState();
    }

    private volatile Map<Long, CompletableFuture> startFutureMap = new ConcurrentHashMap<>();

    private volatile Map<Long, CompletableFuture> cancelFutureMap = new ConcurrentHashMap<>();

    @Override
    public Map<String, Serializable> dashboard() {
        JobsOverview.Task overview = new JobsOverview.Task();
        Integer totalJmMemory = 0;
        Integer totalTmMemory = 0;
        Integer totalTm = 0;
        Integer totalSlot = 0;
        Integer availableSlot = 0;
        Integer runningJob = 0;

        // stat metrics from other than kubernetes mode
        for (Application v : FlinkTrackingTask.getAllTrackingApp().values()) {
            if (v.getJmMemory() != null) {
                totalJmMemory += v.getJmMemory();
            }
            if (v.getTmMemory() != null) {
                totalTmMemory += v.getTmMemory() * (v.getTotalTM() == null ? 1 : v.getTotalTM());
            }
            if (v.getTotalTM() != null) {
                totalTm += v.getTotalTM();
            }
            if (v.getTotalSlot() != null) {
                totalSlot += v.getTotalSlot();
            }
            if (v.getAvailableSlot() != null) {
                availableSlot += v.getAvailableSlot();
            }
            if (v.getState() == FlinkAppState.RUNNING.getValue()) {
                runningJob++;
            }
            JobsOverview.Task task = v.getOverview();
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
        FlinkMetricCV k8sMetric = k8SFlinkTrackMonitor.getAccClusterMetrics();
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
    public void tailMvnDownloading(Long id) {
        this.tailOutMap.put(id, id);
        // the first time, will be read from the beginning of the buffer. Only once
        this.tailBeginning.put(id, true);
    }

    @Override
    public String upload(MultipartFile file) throws ApplicationException {
        File temp = WebUtils.getAppTempDir();
        File saveFile = new File(temp, Objects.requireNonNull(file.getOriginalFilename()));
        // delete when exists
        if (saveFile.exists()) {
            saveFile.delete();
        }
        // save file to temp dir
        try {
            file.transferTo(saveFile);
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
        return saveFile.getAbsolutePath();
    }

    @Override
    public void toEffective(Application application) {
        // set latest to Effective
        ApplicationConfig config = configService.getLatest(application.getId());
        if (config != null) {
            this.configService.toEffective(application.getId(), config.getId());
        }
        if (application.isFlinkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getCandidate(application.getId(), null);
            if (flinkSql != null) {
                flinkSqlService.toEffective(application.getId(), flinkSql.getId());
                // clean candidate
                flinkSqlService.cleanCandidate(flinkSql.getId());
            }
        }
    }

    @Override
    public void revoke(Application appParma) throws ApplicationException {
        Application application = getById(appParma.getId());
        assert application != null;

        //1) delete files that have been published to workspace
        application.getFsOperator().delete(application.getAppHome());

        //2) rollback the files to the workspace
        backUpService.revoke(application);

        //3) restore related status
        LambdaUpdateWrapper<Application> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(Application::getId, application.getId());
        if (application.isFlinkSqlJob()) {
            updateWrapper.set(Application::getLaunch, LaunchState.FAILED.get());
        } else {
            updateWrapper.set(Application::getLaunch, LaunchState.NEED_LAUNCH.get());
        }
        if (!application.isRunning()) {
            updateWrapper.set(Application::getState, FlinkAppState.REVOKED.getValue());
        }
        try {
            FlinkTrackingTask.refreshTracking(application.getId(), () -> {
                baseMapper.update(null, updateWrapper);
                return null;
            });
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    @RefreshCache
    public Boolean delete(Application paramApp) {

        Application application = getById(paramApp.getId());

        try {
            // 1) remove flink sql
            flinkSqlService.removeApp(application.getId());

            // 2) remove log
            applicationLogService.removeApp(application.getId());

            // 3) remove config
            configService.removeApp(application.getId());

            // 4) remove effective
            effectiveService.removeApp(application.getId());

            // remove related hdfs
            // 5) remove backup
            backUpService.removeApp(application);

            // 6) remove savepoint
            savePointService.removeApp(application);

            // 7) remove BuildPipeline
            appBuildPipeService.removeApp(application.getId());

            // 8) remove app
            removeApp(application);

            if (isKubernetesApp(paramApp)) {
                k8SFlinkTrackMonitor.unTrackingJob(toTrackId(application));
            } else {
                FlinkTrackingTask.stopTracking(paramApp.getId());
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void restart(Application application) throws Exception {
        this.cancel(application);
        this.start(application, false);
    }

    @Override
    public boolean checkEnv(Application appParam) throws ApplicationException {
        Application application = getById(appParam.getId());
        try {
            FlinkEnv flinkEnv;
            if (application.getVersionId() != null) {
                flinkEnv = flinkEnvService.getByIdOrDefault(application.getVersionId());
            } else {
                flinkEnv = flinkEnvService.getDefault();
            }
            if (flinkEnv == null) {
                return false;
            }
            envInitializer.checkFlinkEnv(application.getStorageType(), flinkEnv);
            envInitializer.storageInitialize(application.getStorageType());
            return true;
        } catch (Exception e) {
            log.error(ExceptionUtils.stringifyException(e));
            throw new ApplicationException(e);
        }
    }

    @Override
    public boolean checkAlter(Application application) {
        Long appId = application.getId();
        FlinkAppState state = FlinkAppState.of(application.getState());
        if (!FlinkAppState.CANCELED.equals(state)) {
            return false;
        }
        Long useId = FlinkTrackingTask.getCanlledJobUserId(appId);
        if (useId == null || application.getUserId().longValue() != FlinkTrackingTask.getCanlledJobUserId(appId).longValue()) {
            return true;
        }
        return false;
    }

    private void removeApp(Application application) {
        Long appId = application.getId();
        removeById(appId);
        application.getFsOperator().delete(application.getWorkspace().APP_WORKSPACE().concat("/").concat(appId.toString()));
        try {
            // try to delete yarn-application, and leave no trouble.
            String path = Workspace.of(StorageType.HDFS).APP_WORKSPACE().concat("/").concat(appId.toString());
            if (HdfsOperator.exists(path)) {
                HdfsOperator.delete(path);
            }
        } catch (Exception e) {
            //skip
        }
    }

    @Override
    public IPage<Application> page(Application appParam, RestRequest request) {
        Page<Application> page = new MybatisPager<Application>().getDefaultPage(request);
        if (CommonUtils.notEmpty(appParam.getStateArray())) {
            if (Arrays.stream(appParam.getStateArray()).anyMatch(x -> x == FlinkAppState.FINISHED.getValue())) {
                Integer[] newArray = CommonUtils.arrayInsertIndex(
                    appParam.getStateArray(),
                    appParam.getStateArray().length,
                    FlinkAppState.POS_TERMINATED.getValue()
                );
                appParam.setStateArray(newArray);
            }
        }
        this.baseMapper.page(page, appParam);
        List<Application> records = page.getRecords();
        long now = System.currentTimeMillis();
        List<Application> newRecords = records.stream().map(record -> {
            // status of flink job on kubernetes mode had been automatically persisted to db in time.
            if (isKubernetesApp(record)) {
                // set duration
                String restUrl = k8SFlinkTrackMonitor.getRemoteRestUrl(toTrackId(record));
                record.setFlinkRestUrl(restUrl);
                if (record.getTracking() == 1 && record.getStartTime() != null && record.getStartTime().getTime() > 0) {
                    record.setDuration(now - record.getStartTime().getTime());
                }
                return record;
            }
            Application app = FlinkTrackingTask.getTracking(record.getId());
            if (app == null) {
                return record;
            }
            app.setNickName(record.getNickName());
            app.setUserName(record.getUserName());
            app.setFlinkVersion(record.getFlinkVersion());
            app.setProjectName(record.getProjectName());
            return app;
        }).collect(Collectors.toList());
        page.setRecords(newRecords);
        return page;
    }

    @Override
    public String getYarnName(Application appParam) {
        String[] args = new String[2];
        args[0] = "--name";
        args[1] = appParam.getConfig();
        return ParameterCli.read(args);
    }

    /**
     * Check if the current jobName and other key identifiers already exist in db and yarn/k8s
     */
    @Override
    public AppExistsState checkExists(Application appParam) {

        if (!checkJobName(appParam.getJobName())) {
            return AppExistsState.INVALID;
        }
        boolean inDB = this.baseMapper.selectCount(
            new LambdaQueryWrapper<Application>()
                .eq(Application::getJobName, appParam.getJobName())) > 0;

        if (appParam.getId() != null) {
            Application app = getById(appParam.getId());
            if (app.getJobName().equals(appParam.getJobName())) {
                return AppExistsState.NO;
            }

            if (inDB) {
                return AppExistsState.IN_DB;
            }

            FlinkAppState state = FlinkAppState.of(app.getState());
            // has stopped status
            if (state.equals(FlinkAppState.ADDED) ||
                state.equals(FlinkAppState.CREATED) ||
                state.equals(FlinkAppState.FAILED) ||
                state.equals(FlinkAppState.CANCELED) ||
                state.equals(FlinkAppState.LOST) ||
                state.equals(FlinkAppState.KILLED)) {
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
            if (inDB) {
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

    @SneakyThrows
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean create(Application appParam) {
        appParam.setUserId(commonService.getCurrentUser().getUserId());
        appParam.setState(FlinkAppState.ADDED.getValue());
        appParam.setLaunch(LaunchState.NEED_LAUNCH.get());
        appParam.setOptionState(OptionState.NONE.getValue());
        appParam.setCreateTime(new Date());
        appParam.doSetHotParams();
        if (appParam.isUploadJob()) {
            String jarPath = WebUtils.getAppTempDir().getAbsolutePath().concat("/").concat(appParam.getJar());
            appParam.setJarCheckSum(FileUtils.checksumCRC32(new File(jarPath)));
        }

        boolean saved = save(appParam);
        if (saved) {
            if (appParam.isFlinkSqlJob()) {
                FlinkSql flinkSql = new FlinkSql(appParam);
                flinkSqlService.create(flinkSql);
            }
            if (appParam.getConfig() != null) {
                configService.create(appParam, true);
            }
            assert appParam.getId() != null;
            return true;
        }
        return false;
    }

    @SuppressWarnings("checkstyle:WhitespaceAround")
    @Override
    @SneakyThrows
    @Transactional(rollbackFor = {Exception.class})
    public Long copy(Application appParam) {
        long count = this.baseMapper.selectCount(
            new LambdaQueryWrapper<Application>()
                .eq(Application::getJobName, appParam.getJobName()));
        if (count > 0) {
            throw new IllegalArgumentException("[StreamPark] Application names cannot be repeated");
        }
        Application oldApp = getById(appParam.getId());

        Application newApp = new Application();
        String jobName = appParam.getJobName();
        String args = appParam.getArgs();

        newApp.setJobName(jobName);
        newApp.setClusterId(oldApp.getExecutionModeEnum() == ExecutionMode.KUBERNETES_NATIVE_SESSION ? oldApp.getClusterId() : jobName);
        args = args != null && !"".equals(args) ? args : oldApp.getArgs();
        newApp.setArgs(args);
        newApp.setVersionId(oldApp.getVersionId());

        newApp.setFlinkClusterId(oldApp.getFlinkClusterId());
        newApp.setRestartSize(oldApp.getRestartSize());
        newApp.setJobType(oldApp.getJobType());
        newApp.setOptions(oldApp.getOptions());
        newApp.setDynamicOptions(oldApp.getDynamicOptions());
        newApp.setResolveOrder(oldApp.getResolveOrder());
        newApp.setExecutionMode(oldApp.getExecutionMode());
        newApp.setFlinkImage(oldApp.getFlinkImage());
        newApp.setK8sNamespace(oldApp.getK8sNamespace());
        newApp.setK8sRestExposedType(oldApp.getK8sRestExposedType());
        newApp.setK8sPodTemplate(oldApp.getK8sPodTemplate());
        newApp.setK8sJmPodTemplate(oldApp.getK8sJmPodTemplate());
        newApp.setK8sTmPodTemplate(oldApp.getK8sTmPodTemplate());
        newApp.setK8sHadoopIntegration(oldApp.getK8sHadoopIntegration());
        newApp.setDescription(oldApp.getDescription());
        newApp.setAlertId(oldApp.getAlertId());
        newApp.setCpFailureAction(oldApp.getCpFailureAction());
        newApp.setCpFailureRateInterval(oldApp.getCpFailureRateInterval());
        newApp.setCpMaxFailureInterval(oldApp.getCpMaxFailureInterval());
        newApp.setMainClass(oldApp.getMainClass());
        newApp.setAppType(oldApp.getAppType());
        newApp.setResourceFrom(oldApp.getResourceFrom());
        newApp.setProjectId(oldApp.getProjectId());
        newApp.setModule(oldApp.getModule());
        newApp.setDefaultModeIngress(oldApp.getDefaultModeIngress());

        newApp.setUserId(commonService.getCurrentUser().getUserId());
        newApp.setState(FlinkAppState.ADDED.getValue());
        newApp.setLaunch(LaunchState.NEED_LAUNCH.get());
        newApp.setOptionState(OptionState.NONE.getValue());
        newApp.setCreateTime(new Date());
        newApp.doSetHotParams();

        newApp.setJar(oldApp.getJar());
        newApp.setJarCheckSum(oldApp.getJarCheckSum());
        newApp.setTags(oldApp.getTags());

        boolean saved = save(newApp);
        if (saved) {
            if (newApp.isFlinkSqlJob()) {
                FlinkSql copyFlinkSql = flinkSqlService.getLatestFlinkSql(appParam.getId(), true);
                newApp.setFlinkSql(copyFlinkSql.getSql());
                newApp.setDependency(copyFlinkSql.getDependency());
                FlinkSql flinkSql = new FlinkSql(newApp);
                flinkSqlService.create(flinkSql);
            }
            if (newApp.getConfig() != null) {
                ApplicationConfig copyConfig = configService.getEffective(appParam.getId());
                ApplicationConfig config = new ApplicationConfig();
                config.setAppId(newApp.getId());
                config.setFormat(copyConfig.getFormat());
                config.setContent(copyConfig.getContent());
                config.setCreateTime(new Date());
                config.setVersion(1);
                configService.save(config);
                configService.setLatestOrEffective(true, config.getId(), newApp.getId());
            }
            assert newApp.getId() != null;
            return newApp.getId();
        }
        return 0L;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    @RefreshCache
    public boolean update(Application appParam) {
        try {
            Application application = getById(appParam.getId());

            application.setLaunch(LaunchState.NEED_LAUNCH.get());

            if (application.isUploadJob()) {
                if (!ObjectUtils.safeEquals(application.getJar(), appParam.getJar())) {
                    application.setBuild(true);
                } else {
                    File jarFile = new File(WebUtils.getAppTempDir(), appParam.getJar());
                    if (jarFile.exists()) {
                        long checkSum = FileUtils.checksumCRC32(jarFile);
                        if (!ObjectUtils.safeEquals(checkSum, application.getJarCheckSum())) {
                            application.setBuild(true);
                        }
                    }
                }
            }

            if (!application.getBuild()) {
                if (!application.getExecutionMode().equals(appParam.getExecutionMode())) {
                    if (appParam.getExecutionModeEnum().equals(ExecutionMode.YARN_APPLICATION) ||
                        application.getExecutionModeEnum().equals(ExecutionMode.YARN_APPLICATION)) {
                        application.setBuild(true);
                    }
                }
            }

            if (ExecutionMode.isKubernetesMode(appParam.getExecutionMode())) {
                if (!ObjectUtils.safeTrimEquals(application.getK8sRestExposedType(), appParam.getK8sRestExposedType()) ||
                    !ObjectUtils.safeTrimEquals(application.getK8sJmPodTemplate(), appParam.getK8sJmPodTemplate()) ||
                    !ObjectUtils.safeTrimEquals(application.getK8sTmPodTemplate(), appParam.getK8sTmPodTemplate()) ||
                    !ObjectUtils.safeTrimEquals(application.getK8sPodTemplates(), appParam.getK8sPodTemplates()) ||
                    !ObjectUtils.safeTrimEquals(application.getK8sHadoopIntegration(), appParam.getK8sHadoopIntegration()) ||
                    !ObjectUtils.safeTrimEquals(application.getFlinkImage(), appParam.getFlinkImage())
                ) {
                    application.setBuild(true);
                }
            }

            appParam.setJobType(application.getJobType());
            // changes to the following parameters need to be re-launched to take effect
            application.setJobName(appParam.getJobName());
            application.setVersionId(appParam.getVersionId());
            application.setArgs(appParam.getArgs());
            application.setOptions(appParam.getOptions());
            application.setDynamicOptions(appParam.getDynamicOptions());
            application.setResolveOrder(appParam.getResolveOrder());
            application.setExecutionMode(appParam.getExecutionMode());
            application.setClusterId(appParam.getClusterId());
            application.setFlinkImage(appParam.getFlinkImage());
            application.setK8sNamespace(appParam.getK8sNamespace());
            application.updateHotParams(appParam);
            application.setK8sRestExposedType(appParam.getK8sRestExposedType());
            application.setK8sPodTemplate(appParam.getK8sPodTemplate());
            application.setK8sJmPodTemplate(appParam.getK8sJmPodTemplate());
            application.setK8sTmPodTemplate(appParam.getK8sTmPodTemplate());
            application.setK8sHadoopIntegration(appParam.getK8sHadoopIntegration());

            // changes to the following parameters do not affect running tasks
            application.setModifyTime(new Date());
            application.setDescription(appParam.getDescription());
            application.setAlertId(appParam.getAlertId());
            application.setRestartSize(appParam.getRestartSize());
            application.setCpFailureAction(appParam.getCpFailureAction());
            application.setCpFailureRateInterval(appParam.getCpFailureRateInterval());
            application.setCpMaxFailureInterval(appParam.getCpMaxFailureInterval());
            application.setFlinkClusterId(appParam.getFlinkClusterId());
            application.setTags(appParam.getTags());

            // Flink Sql job...
            if (application.isFlinkSqlJob()) {
                updateFlinkSqlJob(application, appParam);
            } else {
                if (application.isStreamParkJob()) {
                    configService.update(appParam, application.isRunning());
                } else {
                    application.setJar(appParam.getJar());
                    application.setMainClass(appParam.getMainClass());
                }
            }
            baseMapper.updateById(application);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * update FlinkSql type jobs, there are 3 aspects to consider<br/>
     * 1. flink sql has changed <br/>
     * 2. dependency has changed<br/>
     * 3. parameter has changed<br/>
     *
     * @param application
     * @param appParam
     */
    private void updateFlinkSqlJob(Application application, Application appParam) {
        FlinkSql effectiveFlinkSql = flinkSqlService.getEffective(application.getId(), true);
        if (effectiveFlinkSql == null) {
            effectiveFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.NEW);
            flinkSqlService.removeById(effectiveFlinkSql.getId());
            FlinkSql sql = new FlinkSql(appParam);
            flinkSqlService.create(sql);
            application.setBuild(true);
        } else {
            // get previous flink sql and decode
            FlinkSql copySourceFlinkSql = flinkSqlService.getById(appParam.getSqlId());
            assert copySourceFlinkSql != null;
            copySourceFlinkSql.decode();

            // get submit flink sql
            FlinkSql targetFlinkSql = new FlinkSql(appParam);

            // judge sql and dependency has changed
            ChangedType changedType = copySourceFlinkSql.checkChange(targetFlinkSql);

            log.info("updateFlinkSqlJob changedType: {}", changedType);

            // if has been changed
            if (changedType.hasChanged()) {
                // check if there is a candidate version for the newly added record
                FlinkSql newFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.NEW);
                // If the candidate version of the new record exists, it will be deleted directly,
                // and only one candidate version will be retained. If the new candidate version is not effective,
                // if it is edited again and the next record comes in, the previous candidate version will be deleted.
                if (newFlinkSql != null) {
                    // delete all records about candidates
                    flinkSqlService.removeById(newFlinkSql.getId());
                }
                FlinkSql historyFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.HISTORY);
                // remove candidate flags that already exist but are set as candidates
                if (historyFlinkSql != null) {
                    flinkSqlService.cleanCandidate(historyFlinkSql.getId());
                }
                FlinkSql sql = new FlinkSql(appParam);
                flinkSqlService.create(sql);
                if (changedType.isDependencyChanged()) {
                    application.setBuild(true);
                }
            } else {
                // judge version has changed
                boolean versionChanged = !effectiveFlinkSql.getId().equals(appParam.getSqlId());
                if (versionChanged) {
                    // sql and dependency not changed, but version changed, means that rollback to the version
                    CandidateType type = CandidateType.HISTORY;
                    flinkSqlService.setCandidate(type, appParam.getId(), appParam.getSqlId());
                    application.setLaunch(LaunchState.NEED_ROLLBACK.get());
                    application.setBuild(true);
                }
            }
        }
        this.configService.update(appParam, application.isRunning());
    }

    @Override
    @RefreshCache
    public void updateLaunch(Application application) {
        LambdaUpdateWrapper<Application> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(Application::getId, application.getId());
        updateWrapper.set(Application::getLaunch, application.getLaunch());
        updateWrapper.set(Application::getBuild, application.getBuild());
        if (application.getOptionState() != null) {
            updateWrapper.set(Application::getOptionState, application.getOptionState());
        }
        this.update(updateWrapper);
    }

    @Override
    public List<Application> getByProjectId(Long id) {
        return baseMapper.getByProjectId(id);
    }

    @Override
    @RefreshCache
    public boolean checkBuildAndUpdate(Application application) {
        boolean build = application.getBuild();
        if (!build) {
            LambdaUpdateWrapper<Application> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.eq(Application::getId, application.getId());
            if (application.isRunning()) {
                updateWrapper.set(Application::getLaunch, LaunchState.NEED_RESTART.get());
            } else {
                updateWrapper.set(Application::getLaunch, LaunchState.DONE.get());
                updateWrapper.set(Application::getOptionState, OptionState.NONE.getValue());
            }
            this.update(updateWrapper);

            // backup
            if (application.isFlinkSqlJob()) {
                FlinkSql newFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.NEW);
                if (!application.isNeedRollback() && newFlinkSql != null) {
                    backUpService.backup(application, newFlinkSql);
                }
            }

            // If the current task is not running, or the task has just been added,
            // directly set the candidate version to the official version
            FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
            if (!application.isRunning() || flinkSql == null) {
                this.toEffective(application);
            }

        }
        return build;
    }

    @Override
    public void forcedStop(Application app) {
        CompletableFuture startFuture = startFutureMap.remove(app.getId());
        CompletableFuture cancelFuture = cancelFutureMap.remove(app.getId());
        if (startFuture != null) {
            startFuture.cancel(true);
        }
        if (cancelFuture != null) {
            cancelFuture.cancel(true);
        }
        if (startFuture == null && cancelFuture == null) {
            this.updateToStopped(app);
        }
    }

    @Override
    @RefreshCache
    public void clean(Application appParam) {
        appParam.setLaunch(LaunchState.DONE.get());
        this.updateLaunch(appParam);
    }

    @Override
    public String readConf(Application appParam) throws IOException {
        File file = new File(appParam.getConfig());
        String conf = FileUtils.readFileToString(file);
        return Base64.getEncoder().encodeToString(conf.getBytes());
    }

    @Override
    @RefreshCache
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
                String path = this.projectService.getAppConfPath(application.getProjectId(), application.getModule());
                application.setConfPath(path);
            }
        }
        // add flink web url info for k8s-mode
        if (isKubernetesApp(application)) {
            String restUrl = k8SFlinkTrackMonitor.getRemoteRestUrl(toTrackId(application));
            application.setFlinkRestUrl(restUrl);

            // set duration
            long now = System.currentTimeMillis();
            if (application.getTracking() == 1 && application.getStartTime() != null && application.getStartTime().getTime() > 0) {
                application.setDuration(now - application.getStartTime().getTime());
            }
        }

        if (ExecutionMode.YARN_APPLICATION.equals(application.getExecutionModeEnum())) {
            if (!application.getHotParamsMap().isEmpty()) {
                if (application.getHotParamsMap().containsKey(ConfigConst.KEY_YARN_APP_QUEUE())) {
                    application.setYarnQueue(application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_QUEUE()).toString());
                }
            }
        }

        if (ExecutionMode.YARN_SESSION.equals(application.getExecutionModeEnum())) {
            if (!application.getHotParamsMap().isEmpty()) {
                if (application.getHotParamsMap().containsKey(ConfigConst.KEY_YARN_APP_ID())) {
                    application.setYarnSessionClusterId(application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_ID()).toString());
                }
            }
        }
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
            String modulePath = project.getDistHome().getAbsolutePath().concat("/").concat(application.getModule());
            jarFile = new File(modulePath, application.getJar());
        }
        Manifest manifest = Utils.getJarManifest(jarFile);
        return manifest.getMainAttributes().getValue("Main-Class");
    }

    @Override
    @RefreshCache
    public boolean mapping(Application appParam) {
        boolean mapping = this.baseMapper.mapping(appParam);
        Application application = getById(appParam.getId());
        if (isKubernetesApp(application)) {
            k8SFlinkTrackMonitor.unTrackingJob(toTrackId(application));
        } else {
            FlinkTrackingTask.addTracking(application);
        }
        return mapping;
    }

    @Override
    @RefreshCache
    public void cancel(Application appParam) throws Exception {
        FlinkTrackingTask.setOptionState(appParam.getId(), OptionState.CANCELLING);
        Application application = getById(appParam.getId());

        application.setState(FlinkAppState.CANCELLING.getValue());
        if (appParam.getSavePointed()) {
            FlinkTrackingTask.addSavepoint(application.getId());
            application.setOptionState(OptionState.SAVEPOINTING.getValue());
        } else {
            application.setOptionState(OptionState.CANCELLING.getValue());
        }

        application.setOptionTime(new Date());
        this.baseMapper.updateById(application);

        FlinkEnv flinkEnv = flinkEnvService.getById(application.getVersionId());

        // infer savepoint
        String customSavepoint = null;
        if (appParam.getSavePointed()) {
            customSavepoint = appParam.getSavePoint();
            if (StringUtils.isBlank(customSavepoint)) {
                customSavepoint = getSavePointPath(appParam);
            }
        }

        Map<String, Object> extraParameter = new HashMap<>(0);

        Map<String, Object> optionMap = application.getOptionMap();

        if (ExecutionMode.isRemoteMode(application.getExecutionModeEnum())) {
            FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
            assert cluster != null;
            URI activeAddress = cluster.getActiveAddress();
            extraParameter.put(RestOptions.ADDRESS.key(), activeAddress.getHost());
            extraParameter.put(RestOptions.PORT.key(), activeAddress.getPort());
        }

        if (ExecutionMode.isYarnMode(application.getExecutionModeEnum())) {
            String yarnQueue = (String) application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_QUEUE());
            if (yarnQueue != null) {
                optionMap.put(ConfigConst.KEY_YARN_APP_QUEUE(), yarnQueue);
            }
            if (ExecutionMode.YARN_SESSION.equals(application.getExecutionModeEnum())) {
                String yarnSessionClusterId = (String) application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_ID());
                assert yarnSessionClusterId != null;
                extraParameter.put(ConfigConst.KEY_YARN_APP_ID(), yarnSessionClusterId);
            }
        }

        Long userId = commonService.getCurrentUser().getUserId();
        if (!application.getUserId().equals(userId)) {
            FlinkTrackingTask.addCanlledApp(application.getId(), userId);
        }

        CancelRequest cancelRequest = new CancelRequest(
            flinkEnv.getFlinkVersion(),
            ExecutionMode.of(application.getExecutionMode()),
            application.getAppId(),
            application.getJobId(),
            appParam.getSavePointed(),
            appParam.getDrain(),
            customSavepoint,
            application.getK8sNamespace(),
            application.getDynamicOptions(),
            extraParameter
        );

        CompletableFuture<CancelResponse> cancelFuture = CompletableFuture.supplyAsync(() -> FlinkSubmitter.cancel(cancelRequest), executorService);

        cancelFutureMap.put(application.getId(), cancelFuture);

        CompletableFutureUtils.runTimeout(
            cancelFuture,
            10L,
            TimeUnit.MINUTES,
            cancelResponse -> {
                if (cancelResponse != null && cancelResponse.savePointDir() != null) {
                    String savePointDir = cancelResponse.savePointDir();
                    log.info("savePoint path: {}", savePointDir);
                    SavePoint savePoint = new SavePoint();
                    Date now = new Date();
                    savePoint.setPath(savePointDir);
                    savePoint.setAppId(application.getId());
                    savePoint.setLatest(true);
                    savePoint.setType(CheckPointType.SAVEPOINT.get());
                    savePoint.setTriggerTime(now);
                    savePoint.setCreateTime(now);
                    savePointService.save(savePoint);
                }
                if (isKubernetesApp(application)) {
                    k8SFlinkTrackMonitor.unTrackingJob(toTrackId(application));
                }
            },
            e -> {
                if (e.getCause() instanceof CancellationException) {
                    updateToStopped(application);
                } else {
                    log.error("stop flink job fail.", e);
                    application.setOptionState(OptionState.NONE.getValue());
                    application.setState(FlinkAppState.FAILED.getValue());
                    updateById(application);

                    if (appParam.getSavePointed()) {
                        savePointService.obsolete(application.getId());
                    }

                    // re-tracking flink job on kubernetes and logging exception
                    if (isKubernetesApp(application)) {
                        TrackId id = toTrackId(application);
                        k8SFlinkTrackMonitor.unTrackingJob(id);
                        k8SFlinkTrackMonitor.trackingJob(id);
                    } else {
                        FlinkTrackingTask.stopTracking(application.getId());
                    }

                    ApplicationLog log = new ApplicationLog();
                    log.setAppId(application.getId());
                    log.setYarnAppId(application.getClusterId());
                    log.setOptionTime(new Date());
                    String exception = ExceptionUtils.stringifyException(e);
                    log.setException(exception);
                    log.setSuccess(false);
                    applicationLogService.save(log);
                }
            }
        ).whenComplete((t, e) -> {
            cancelFuture.cancel(true);
            cancelFutureMap.remove(application.getId());
        });

    }

    @Override
    public String checkSavepointPath(Application appParam) throws Exception {
        String savepointPath = getSavePointPath(appParam);
        if (StringUtils.isNotBlank(savepointPath)) {
            final URI uri = URI.create(savepointPath);
            final String scheme = uri.getScheme();
            final String pathPart = uri.getPath();
            String error = null;
            if (scheme == null) {
                error = "This state.savepoints.dir value " + savepointPath + " scheme (hdfs://, file://, etc) of  is null. Please specify the file system scheme explicitly in the URI.";
            } else if (pathPart == null) {
                error = "This state.savepoints.dir value " + savepointPath + " path part to store the checkpoint data in is null. Please specify a directory path for the checkpoint data.";
            } else if (pathPart.length() == 0 || pathPart.equals("/")) {
                error = "This state.savepoints.dir value " + savepointPath + " Cannot use the root directory for checkpoints.";
            }
            return error;
        } else {
            return "When custom savepoint is not set, state.savepoints.dir needs to be set in Dynamic Option or flink-conf.yaml of application";
        }
    }

    @Override
    public void updateTracking(Application appParam) {
        this.baseMapper.updateTracking(appParam);
    }

    /**
     * Setup task is starting (for webUI "state" display)
     *
     * @param appParam
     */
    @Override
    @RefreshCache
    public void starting(Application appParam) {
        Application application = getById(appParam.getId());
        assert application != null;
        application.setState(FlinkAppState.STARTING.getValue());
        application.setOptionTime(new Date());
        updateById(application);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    @RefreshCache
    public void start(Application appParam, boolean auto) throws Exception {

        final Application application = getById(appParam.getId());

        assert application != null;

        // if manually started, clear the restart flag
        if (!auto) {
            application.setRestartCount(0);
        } else {
            if (!application.isNeedRestartOnFailed()) {
                return;
            }
            application.setRestartCount(application.getRestartCount() + 1);
            application.setSavePointed(true);
        }

        String appConf;
        String flinkUserJar = null;
        ApplicationLog applicationLog = new ApplicationLog();
        applicationLog.setAppId(application.getId());
        applicationLog.setOptionTime(new Date());

        // set the latest to Effective, (it will only become the current effective at this time)
        this.toEffective(application);

        ApplicationConfig applicationConfig = configService.getEffective(application.getId());
        ExecutionMode executionMode = ExecutionMode.of(application.getExecutionMode());
        assert executionMode != null;
        if (application.isCustomCodeJob()) {
            if (application.isUploadJob()) {
                appConf = String.format("json://{\"%s\":\"%s\"}",
                    ConfigConst.KEY_FLINK_APPLICATION_MAIN_CLASS(),
                    application.getMainClass()
                );
            } else {
                switch (application.getApplicationType()) {
                    case STREAMPARK_FLINK:
                        String format = applicationConfig.getFormat() == 1 ? "yaml" : "prop";
                        appConf = String.format("%s://%s", format, applicationConfig.getContent());
                        break;
                    case APACHE_FLINK:
                        appConf = String.format("json://{\"%s\":\"%s\"}", ConfigConst.KEY_FLINK_APPLICATION_MAIN_CLASS(), application.getMainClass());
                        break;
                    default:
                        throw new IllegalArgumentException("[StreamPark] ApplicationType must be (StreamPark flink | Apache flink)... ");
                }
            }

            if (executionMode.equals(ExecutionMode.YARN_APPLICATION)) {
                switch (application.getApplicationType()) {
                    case STREAMPARK_FLINK:
                        flinkUserJar = String.format("%s/%s", application.getAppLib(), application.getModule().concat(".jar"));
                        break;
                    case APACHE_FLINK:
                        flinkUserJar = String.format("%s/%s", application.getAppHome(), application.getJar());
                        break;
                    default:
                        throw new IllegalArgumentException("[StreamPark] ApplicationType must be (StreamPark flink | Apache flink)... ");
                }
            }
        } else if (application.isFlinkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
            assert flinkSql != null;
            // 1) dist_userJar
            FlinkEnv flinkEnv = flinkEnvService.getByIdOrDefault(application.getVersionId());
            String sqlDistJar = commonService.getSqlClientJar(flinkEnv);
            // 2) appConfig
            appConf = applicationConfig == null ? null : String.format("yaml://%s", applicationConfig.getContent());
            // 3) client
            if (executionMode.equals(ExecutionMode.YARN_APPLICATION)) {
                String clientPath = Workspace.remote().APP_CLIENT();
                flinkUserJar = String.format("%s/%s", clientPath, sqlDistJar);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported...");
        }

        Map<String, String> dynamicOption = FlinkSubmitter.extractDynamicOptionAsJava(application.getDynamicOptions());

        Map<String, Object> extraParameter = new HashMap<>(0);
        extraParameter.put(ConfigConst.KEY_JOB_ID(), application.getId());

        if (appParam.getAllowNonRestored()) {
            extraParameter.put(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.key(), true);
        }

        if (ExecutionMode.isRemoteMode(application.getExecutionModeEnum())) {
            FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
            assert cluster != null;
            URI activeAddress = cluster.getActiveAddress();
            extraParameter.put(RestOptions.ADDRESS.key(), activeAddress.getHost());
            extraParameter.put(RestOptions.PORT.key(), activeAddress.getPort());
        }

        if (ExecutionMode.isYarnMode(application.getExecutionModeEnum())) {
            String yarnQueue = (String) application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_QUEUE());
            if (yarnQueue != null) {
                dynamicOption.put(ConfigConst.KEY_YARN_APP_QUEUE(), yarnQueue);
            }
            if (ExecutionMode.YARN_SESSION.equals(application.getExecutionModeEnum())) {
                String yarnSessionClusterId = (String) application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_ID());
                assert yarnSessionClusterId != null;
                extraParameter.put(ConfigConst.KEY_YARN_APP_ID(), yarnSessionClusterId);
            }
        }

        if (application.isFlinkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
            extraParameter.put(ConfigConst.KEY_FLINK_SQL(null), flinkSql.getSql());
        }

        ResolveOrder resolveOrder = ResolveOrder.of(application.getResolveOrder());

        KubernetesSubmitParam kubernetesSubmitParam = new KubernetesSubmitParam(
            application.getClusterId(),
            application.getK8sNamespace(),
            application.getK8sRestExposedTypeEnum());

        FlinkEnv flinkEnv = flinkEnvService.getByIdOrDefault(application.getVersionId());
        if (flinkEnv == null) {
            throw new IllegalArgumentException("[StreamPark] can no found flink version");
        }

        AppBuildPipeline buildPipeline = appBuildPipeService.getById(application.getId());

        assert buildPipeline != null;

        BuildResult buildResult = buildPipeline.getBuildResult();
        if (executionMode.equals(ExecutionMode.YARN_APPLICATION)) {
            buildResult = new ShadedBuildResponse(null, flinkUserJar, true);
        } else {
            if (ExecutionMode.isKubernetesApplicationMode(application.getExecutionMode())) {
                assert buildResult != null;
                DockerImageBuildResponse result = buildResult.as(DockerImageBuildResponse.class);
                String ingressTemplates = application.getIngressTemplate();
                String domainName = application.getDefaultModeIngress();
                if (StringUtils.isNotBlank(ingressTemplates)) {
                    String ingressOutput = result.workspacePath() + "/ingress.yaml";
                    IngressController.configureIngress(ingressOutput);
                }
                if (StringUtils.isNotBlank(domainName)) {
                    try {
                        IngressController.configureIngress(domainName, application.getClusterId(), application.getK8sNamespace());
                    } catch (KubernetesClientException e) {
                        log.info("Failed to create ingress, stack info:{}", e.getMessage());
                        applicationLog.setException(e.getMessage());
                        applicationLog.setSuccess(false);
                        applicationLogService.save(applicationLog);
                        application.setState(FlinkAppState.FAILED.getValue());
                        application.setOptionState(OptionState.NONE.getValue());
                        updateById(application);
                        return;
                    }
                }
            }
        }
        SubmitRequest submitRequest = new SubmitRequest(
            flinkEnv.getFlinkVersion(),
            flinkEnv.getFlinkConf(),
            DevelopmentMode.of(application.getJobType()),
            ExecutionMode.of(application.getExecutionMode()),
            resolveOrder,
            application.getJobName(),
            appConf,
            application.getApplicationType(),
            getSavePointed(appParam),
            appParam.getFlameGraph() ? getFlameGraph(application) : null,
            application.getOptionMap(),
            dynamicOption,
            application.getArgs(),
            buildResult,
            kubernetesSubmitParam,
            extraParameter
        );

        CompletableFuture<SubmitResponse> future = CompletableFuture.supplyAsync(() -> FlinkSubmitter.submit(submitRequest), executorService);

        startFutureMap.put(application.getId(), future);

        CompletableFutureUtils.runTimeout(
            future,
            2L,
            TimeUnit.MINUTES,
            submitResponse -> {
                if (submitResponse.flinkConfig() != null) {
                    String jmMemory = submitResponse.flinkConfig().get(ConfigConst.KEY_FLINK_JM_PROCESS_MEMORY());
                    if (jmMemory != null) {
                        application.setJmMemory(FlinkMemorySize.parse(jmMemory).getMebiBytes());
                    }
                    String tmMemory = submitResponse.flinkConfig().get(ConfigConst.KEY_FLINK_TM_PROCESS_MEMORY());
                    if (tmMemory != null) {
                        application.setTmMemory(FlinkMemorySize.parse(tmMemory).getMebiBytes());
                    }
                }
                application.setAppId(submitResponse.clusterId());
                if (StringUtils.isNoneEmpty(submitResponse.jobId())) {
                    application.setJobId(submitResponse.jobId());
                }
                application.setFlameGraph(appParam.getFlameGraph());
                applicationLog.setYarnAppId(submitResponse.clusterId());
                application.setStartTime(new Date());
                application.setEndTime(null);
                if (isKubernetesApp(application)) {
                    application.setLaunch(LaunchState.DONE.get());
                }
                updateById(application);

                // if start completed, will be added task to tracking queue
                if (isKubernetesApp(application)) {
                    k8SFlinkTrackMonitor.trackingJob(toTrackId(application));
                } else {
                    FlinkTrackingTask.setOptionState(appParam.getId(), OptionState.STARTING);
                    FlinkTrackingTask.addTracking(application);
                }

                applicationLog.setSuccess(true);
                applicationLogService.save(applicationLog);
                // set savepoint to expire
                savePointService.obsolete(application.getId());
            }, e -> {
                if (e.getCause() instanceof CancellationException) {
                    updateToStopped(application);
                } else {
                    String exception = ExceptionUtils.stringifyException(e);
                    applicationLog.setException(exception);
                    applicationLog.setSuccess(false);
                    applicationLogService.save(applicationLog);
                    Application app = getById(appParam.getId());
                    app.setState(FlinkAppState.FAILED.getValue());
                    app.setOptionState(OptionState.NONE.getValue());
                    updateById(app);
                    if (isKubernetesApp(app)) {
                        k8SFlinkTrackMonitor.unTrackingJob(toTrackId(app));
                    } else {
                        FlinkTrackingTask.stopTracking(appParam.getId());
                    }
                }
            }
        ).whenComplete((t, e) -> {
            future.cancel(true);
            startFutureMap.remove(application.getId());
        });

    }

    private void updateToStopped(Application app) {
        Application application = getById(app);
        application.setOptionState(OptionState.NONE.getValue());
        application.setState(FlinkAppState.CANCELED.getValue());
        application.setOptionTime(new Date());
        updateById(application);
        savePointService.obsolete(application.getId());
        // re-tracking flink job on kubernetes and logging exception
        if (isKubernetesApp(application)) {
            TrackId id = toTrackId(application);
            k8SFlinkTrackMonitor.unTrackingJob(id);
            k8SFlinkTrackMonitor.trackingJob(id);
        } else {
            FlinkTrackingTask.stopTracking(application.getId());
        }
    }

    private Boolean checkJobName(String jobName) {
        if (!StringUtils.isEmpty(jobName.trim())) {
            return JOB_NAME_PATTERN.matcher(jobName).matches() && SINGLE_SPACE_PATTERN.matcher(jobName).matches();
        }
        return false;
    }

    private String getSavePointPath(Application appParam) throws Exception {
        Application application = getById(appParam.getId());

        // 1) dynamic parameters have the highest priority, read the dynamic parameters are set: -Dstate.savepoints.dir
        String savepointPath = FlinkSubmitter
            .extractDynamicOptionAsJava(application.getDynamicOptions())
            .get(ConfigConst.KEY_FLINK_STATE_SAVEPOINTS_DIR().substring(6));

        // Application conf configuration has the second priority. If it is a streampark|flinksql type task,
        // see if Application conf is configured when the task is defined, if checkpoints are configured and enabled,
        // read `state.savepoints.dir`
        if (StringUtils.isBlank(savepointPath)) {
            if (application.isStreamParkJob() || application.isFlinkSqlJob()) {
                ApplicationConfig applicationConfig = configService.getEffective(application.getId());
                if (applicationConfig != null) {
                    Map<String, String> map = applicationConfig.readConfig();
                    boolean checkpointEnable = Boolean.parseBoolean(map.get(ConfigConst.KEY_FLINK_CHECKPOINTS_ENABLE()));
                    if (checkpointEnable) {
                        savepointPath = map.get(ConfigConst.KEY_FLINK_STATE_SAVEPOINTS_DIR());
                    }
                }
            }
        }

        // 3) If the savepoint is not obtained above, try to obtain the savepoint path according to the deployment type (remote|on yarn)
        if (StringUtils.isBlank(savepointPath)) {
            // 3.1) At the remote mode, request the flink webui interface to get the savepoint path
            if (ExecutionMode.isRemoteMode(application.getExecutionMode())) {
                FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
                assert cluster != null;
                Map<String, String> config = cluster.getFlinkConfig();
                if (!config.isEmpty()) {
                    savepointPath = config.get(ConfigConst.KEY_FLINK_STATE_SAVEPOINTS_DIR().substring(6));
                }
            } else {
                // 3.2) At the yarn or k8s mode, then read the savepoint in flink-conf.yml in the bound flink
                FlinkEnv flinkEnv = flinkEnvService.getById(application.getVersionId());
                savepointPath = flinkEnv.convertFlinkYamlAsMap().get(ConfigConst.KEY_FLINK_STATE_SAVEPOINTS_DIR().substring(6));
            }
        }

        return savepointPath;
    }

    private String getSavePointed(Application appParam) {
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

    private Map<String, Serializable> getFlameGraph(Application application) {
        Map<String, Serializable> flameGraph = new HashMap<>(8);
        flameGraph.put("reporter", "org.apache.streampark.plugin.profiling.reporter.HttpReporter");
        flameGraph.put("type", ApplicationType.STREAMPARK_FLINK.getType());
        flameGraph.put("id", application.getId());
        flameGraph.put("url", settingService.getStreamParkAddress().concat("/metrics/report"));
        flameGraph.put("token", Utils.uuid());
        flameGraph.put("sampleInterval", 1000 * 60 * 2);
        flameGraph.put("metricInterval", 1000 * 60 * 2);
        return flameGraph;
    }
}
