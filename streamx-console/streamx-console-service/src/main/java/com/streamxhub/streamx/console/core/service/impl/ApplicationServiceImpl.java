/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.service.impl;

import static com.streamxhub.streamx.console.core.task.K8sFlinkTrackMonitorWrapper.Bridge.toTrackId;
import static com.streamxhub.streamx.console.core.task.K8sFlinkTrackMonitorWrapper.isKubernetesApp;

import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.conf.Workspace;
import com.streamxhub.streamx.common.domain.FlinkMemorySize;
import com.streamxhub.streamx.common.enums.ApplicationType;
import com.streamxhub.streamx.common.enums.DevelopmentMode;
import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.enums.ResolveOrder;
import com.streamxhub.streamx.common.enums.StorageType;
import com.streamxhub.streamx.common.fs.HdfsOperator;
import com.streamxhub.streamx.common.util.CompletableFutureUtils;
import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.common.util.ExceptionUtils;
import com.streamxhub.streamx.common.util.ThreadUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.common.util.YarnUtils;
import com.streamxhub.streamx.console.base.domain.Constant;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.exception.ApplicationException;
import com.streamxhub.streamx.console.base.util.CommonUtils;
import com.streamxhub.streamx.console.base.util.ObjectUtils;
import com.streamxhub.streamx.console.base.util.SortUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.annotation.RefreshCache;
import com.streamxhub.streamx.console.core.dao.ApplicationMapper;
import com.streamxhub.streamx.console.core.entity.AppBuildPipeline;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.ApplicationConfig;
import com.streamxhub.streamx.console.core.entity.ApplicationLog;
import com.streamxhub.streamx.console.core.entity.FlinkCluster;
import com.streamxhub.streamx.console.core.entity.FlinkEnv;
import com.streamxhub.streamx.console.core.entity.FlinkSql;
import com.streamxhub.streamx.console.core.entity.Project;
import com.streamxhub.streamx.console.core.entity.SavePoint;
import com.streamxhub.streamx.console.core.enums.AppExistsState;
import com.streamxhub.streamx.console.core.enums.CandidateType;
import com.streamxhub.streamx.console.core.enums.ChangedType;
import com.streamxhub.streamx.console.core.enums.CheckPointType;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
import com.streamxhub.streamx.console.core.enums.LaunchState;
import com.streamxhub.streamx.console.core.enums.OptionState;
import com.streamxhub.streamx.console.core.metrics.flink.JobsOverview;
import com.streamxhub.streamx.console.core.runner.EnvInitializer;
import com.streamxhub.streamx.console.core.service.AppBuildPipeService;
import com.streamxhub.streamx.console.core.service.ApplicationBackUpService;
import com.streamxhub.streamx.console.core.service.ApplicationConfigService;
import com.streamxhub.streamx.console.core.service.ApplicationLogService;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.console.core.service.CommonService;
import com.streamxhub.streamx.console.core.service.EffectiveService;
import com.streamxhub.streamx.console.core.service.FlinkClusterService;
import com.streamxhub.streamx.console.core.service.FlinkEnvService;
import com.streamxhub.streamx.console.core.service.FlinkSqlService;
import com.streamxhub.streamx.console.core.service.ProjectService;
import com.streamxhub.streamx.console.core.service.SavePointService;
import com.streamxhub.streamx.console.core.service.SettingService;
import com.streamxhub.streamx.console.core.task.FlinkTrackingTask;
import com.streamxhub.streamx.console.system.service.TeamUserService;
import com.streamxhub.streamx.flink.core.conf.ParameterCli;
import com.streamxhub.streamx.flink.kubernetes.IngressController;
import com.streamxhub.streamx.flink.kubernetes.K8sFlinkTrackMonitor;
import com.streamxhub.streamx.flink.kubernetes.model.FlinkMetricCV;
import com.streamxhub.streamx.flink.kubernetes.model.TrackId;
import com.streamxhub.streamx.flink.packer.pipeline.BuildResult;
import com.streamxhub.streamx.flink.packer.pipeline.DockerImageBuildResponse;
import com.streamxhub.streamx.flink.packer.pipeline.ShadedBuildResponse;
import com.streamxhub.streamx.flink.submit.FlinkSubmitter;
import com.streamxhub.streamx.flink.submit.bean.CancelRequest;
import com.streamxhub.streamx.flink.submit.bean.CancelResponse;
import com.streamxhub.streamx.flink.submit.bean.KubernetesSubmitParam;
import com.streamxhub.streamx.flink.submit.bean.SubmitRequest;
import com.streamxhub.streamx.flink.submit.bean.SubmitResponse;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements ApplicationService {

    private final Map<Long, Long> tailOutMap = new ConcurrentHashMap<>();

    private final Map<Long, Boolean> tailBeginning = new ConcurrentHashMap<>();

    private final ExecutorService executorService = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 2,
        200,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        ThreadUtils.threadFactory("streamx-deploy-executor"),
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

    @Autowired
    private TeamUserService teamUserService;

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
        // 首次会从buffer里从头读取数据.有且仅有一次.
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
        //将Latest的设置为Effective
        ApplicationConfig config = configService.getLatest(application.getId());
        if (config != null) {
            this.configService.toEffective(application.getId(), config.getId());
        }
        if (application.isFlinkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getCandidate(application.getId(), null);
            if (flinkSql != null) {
                flinkSqlService.toEffective(application.getId(), flinkSql.getId());
                //清除备选标记.
                flinkSqlService.cleanCandidate(flinkSql.getId());
            }
        }
    }

    /**
     * 撤销发布.
     *
     * @param appParma
     */
    @Override
    public void revoke(Application appParma) throws ApplicationException {
        Application application = getById(appParma.getId());
        assert application != null;

        //1) 将已经发布到workspace的文件删除
        application.getFsOperator().delete(application.getAppHome());

        //2) 将backup里的文件回滚到workspace
        backUpService.revoke(application);

        //3) 相关状态恢复
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
                baseMapper.update(application, updateWrapper);
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
            //1) 删除flink sql
            flinkSqlService.removeApp(application.getId());

            //2) 删除 log
            applicationLogService.removeApp(application.getId());

            //3) 删除 config
            configService.removeApp(application.getId());

            //4) 删除 effective
            effectiveService.removeApp(application.getId());

            //以下涉及到hdfs文件的删除

            //5) 删除 backup
            backUpService.removeApp(application);

            //6) 删除savepoint
            savePointService.removeApp(application);

            //7) 删除 BuildPipeline
            appBuildPipeService.removeApp(application.getId());

            //8) 删除 app
            removeApp(application);

            if (isKubernetesApp(paramApp)) {
                k8SFlinkTrackMonitor.unTrackingJob(toTrackId(application));
            } else {
                FlinkTrackingTask.stopTracking(paramApp.getId());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
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
                //任务未指定flink version.则检查是否配置的默认的flink version
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
    public Long getCountByTeam(Long teamId) {
        return baseMapper.getCountByTeam(teamId);
    }

    private void removeApp(Application application) {
        Long appId = application.getId();
        removeById(appId);
        application.getFsOperator().delete(application.getWorkspace().APP_WORKSPACE().concat("/").concat(appId.toString()));
        try {
            //曾经设置过yarn-application类型,尝试删除,不留后患.
            HdfsOperator.delete(Workspace.of(StorageType.HDFS).APP_WORKSPACE().concat("/").concat(appId.toString()));
        } catch (Exception e) {
            //skip
        }
    }

    @Override
    public IPage<Application> page(Application appParam, RestRequest request) {
        Page<Application> page = new Page<>();
        List<Long> teamList = teamUserService.getTeamIdList();
        appParam.setTeamIdList(teamList);
        SortUtils.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
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
        //瞒天过海,暗度陈仓,偷天换日,鱼目混珠.
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
            app.setFlinkVersion(record.getFlinkVersion());
            app.setProjectName(record.getProjectName());
            app.setTeamName(record.getTeamName());
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
     * 检查当前的 jobName 以及其他关键标识别在 db 和 yarn/k8s 中是否已经存在
     *
     * @param appParam
     * @return
     */
    @Override
    public AppExistsState checkExists(Application appParam) {

        if (!checkJobName(appParam.getJobName())) {
            return AppExistsState.INVALID;
        }
        boolean inDB = this.baseMapper.selectCount(
            new QueryWrapper<Application>().lambda()
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
            //当前任务已停止的状态
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
                //部署模式发生了变化.
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
                    !ObjectUtils.safeTrimEquals(application.getK8sHadoopIntegration(), appParam.getK8sHadoopIntegration())) {
                    application.setBuild(true);
                }
            }

            //从db中补全jobType到appParam
            appParam.setJobType(application.getJobType());
            //以下参数发生变化,需要重新任务才能生效
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

            //以下参数发生改变不影响正在运行的任务
            application.setModifyTime(new Date());
            application.setDescription(appParam.getDescription());
            application.setAlertId(appParam.getAlertId());
            application.setRestartSize(appParam.getRestartSize());
            application.setCpFailureAction(appParam.getCpFailureAction());
            application.setCpFailureRateInterval(appParam.getCpFailureRateInterval());
            application.setCpMaxFailureInterval(appParam.getCpMaxFailureInterval());
            application.setFlinkClusterId(appParam.getFlinkClusterId());

            // Flink Sql job...
            if (application.isFlinkSqlJob()) {
                updateFlinkSqlJob(application, appParam);
            } else {
                if (application.isStreamXJob()) {
                    configService.update(appParam, application.isRunning());
                } else {
                    application.setJar(appParam.getJar());
                    application.setMainClass(appParam.getMainClass());
                }
            }
            baseMapper.updateById(application);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 更新 FlinkSql 类型的作业.要考虑3个方面<br/>
     * 1. flink Sql是否发生更新 <br/>
     * 2. 依赖是否发生更新<br/>
     * 3. 配置参数是否发生更新<br/>
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
            //1) 获取copy的源FlinkSql
            FlinkSql copySourceFlinkSql = flinkSqlService.getById(appParam.getSqlId());
            assert copySourceFlinkSql != null;
            copySourceFlinkSql.decode();

            //当前提交的FlinkSql记录
            FlinkSql targetFlinkSql = new FlinkSql(appParam);

            //2) 判断sql和依赖是否发生变化
            ChangedType changedType = copySourceFlinkSql.checkChange(targetFlinkSql);

            log.info("updateFlinkSqlJob changedType: {}", changedType);

            //依赖或sql发生了变更
            if (changedType.hasChanged()) {
                // 3) 检查是否存在新增记录的候选版本
                FlinkSql newFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.NEW);
                //存在新增记录的候选版本则直接删除,只会保留一个候选版本,新增候选版本在没有生效的情况下,如果再次编辑,下个记录进来,则删除上个候选版本
                if (newFlinkSql != null) {
                    //删除候选版本的所有记录
                    flinkSqlService.removeById(newFlinkSql.getId());
                }
                FlinkSql historyFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.HISTORY);
                //将已经存在的但是被设置成候选版本的移除候选标记
                if (historyFlinkSql != null) {
                    flinkSqlService.cleanCandidate(historyFlinkSql.getId());
                }
                FlinkSql sql = new FlinkSql(appParam);
                flinkSqlService.create(sql);
                if (changedType.isDependencyChanged()) {
                    application.setBuild(true);
                }
            } else {
                // 2) 判断版本是否发生变化
                //获取正式版本的flinkSql
                boolean versionChanged = !effectiveFlinkSql.getId().equals(appParam.getSqlId());
                if (versionChanged) {
                    //sql和依赖未发生变更,但是版本号发生了变化,说明是回滚到某个版本了
                    CandidateType type = CandidateType.HISTORY;
                    flinkSqlService.setCandidate(type, appParam.getId(), appParam.getSqlId());
                    //直接回滚到某个历史版本(rollback)
                    application.setLaunch(LaunchState.NEED_ROLLBACK.get());
                    application.setBuild(true);
                }
            }
        }
        // 7) 配置文件修改
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
        baseMapper.update(application, updateWrapper);
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
            baseMapper.update(application, updateWrapper);

            // backup.
            if (application.isFlinkSqlJob()) {
                FlinkSql newFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.NEW);
                if (!application.isNeedRollback() && newFlinkSql != null) {
                    backUpService.backup(application, newFlinkSql);
                }
            }

            //如果当前任务未运行,或者刚刚新增的任务,则直接将候选版本的设置为正式版本
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
            // 正在执行savepoint...
            FlinkTrackingTask.addSavepoint(application.getId());
            application.setOptionState(OptionState.SAVEPOINTING.getValue());
        } else {
            application.setOptionState(OptionState.CANCELLING.getValue());
        }

        application.setOptionTime(new Date());
        this.baseMapper.updateById(application);
        //此步骤可能会比较耗时,重新开启一个线程去执行

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
            optionMap.put(ConfigConst.KEY_YARN_APP_QUEUE(), yarnQueue);

            if (ExecutionMode.YARN_SESSION.equals(application.getExecutionModeEnum())) {
                String yarnSessionClusterId = (String) application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_ID());
                assert yarnSessionClusterId != null;
                extraParameter.put(ConfigConst.KEY_YARN_APP_ID(), yarnSessionClusterId);
            }
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

                    // 保持savepoint失败.则将之前的统统设置为过期
                    if (appParam.getSavePointed()) {
                        savePointService.obsolete(application.getId());
                    }

                    // retracking flink job on kubernetes and logging exception
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
            return "When custom savepoint is not set, state.savepoints.dir needs to be set in Dynamic Option or flick-conf.yaml of application";
        }
    }

    @Override
    public void updateTracking(Application appParam) {
        this.baseMapper.updateTracking(appParam);
    }

    /**
     * 设置任务正在启动中.(for webUI "state" display)
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

        //手动启动的,将reStart清空
        if (!auto) {
            application.setRestartCount(0);
        } else {
            if (!application.isNeedRestartOnFailed()) {
                return;
            }
            application.setRestartCount(application.getRestartCount() + 1);
            application.setSavePointed(true);
        }

        //1) 真正执行启动相关的操作..
        String appConf;
        String flinkUserJar = null;
        ApplicationLog applicationLog = new ApplicationLog();
        applicationLog.setAppId(application.getId());
        applicationLog.setOptionTime(new Date());

        //2) 将latest的设置为Effective的,(此时才真正变成当前生效的)
        this.toEffective(application);

        //获取一个最新的Effective的配置
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
                    case STREAMX_FLINK:
                        String format = applicationConfig.getFormat() == 1 ? "yaml" : "prop";
                        appConf = String.format("%s://%s", format, applicationConfig.getContent());
                        break;
                    case APACHE_FLINK:
                        appConf = String.format("json://{\"%s\":\"%s\"}", ConfigConst.KEY_FLINK_APPLICATION_MAIN_CLASS(), application.getMainClass());
                        break;
                    default:
                        throw new IllegalArgumentException("[StreamX] ApplicationType must be (StreamX flink | Apache flink)... ");
                }
            }

            if (executionMode.equals(ExecutionMode.YARN_APPLICATION)) {
                switch (application.getApplicationType()) {
                    case STREAMX_FLINK:
                        flinkUserJar = String.format("%s/%s", application.getAppLib(), application.getModule().concat(".jar"));
                        break;
                    case APACHE_FLINK:
                        flinkUserJar = String.format("%s/%s", application.getAppHome(), application.getJar());
                        break;
                    default:
                        throw new IllegalArgumentException("[StreamX] ApplicationType must be (StreamX flink | Apache flink)... ");
                }
            }
        } else if (application.isFlinkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
            assert flinkSql != null;
            //1) dist_userJar
            FlinkEnv flinkEnv = flinkEnvService.getByIdOrDefault(application.getVersionId());
            String sqlDistJar = commonService.getSqlClientJar(flinkEnv);
            //2) appConfig
            appConf = applicationConfig == null ? null : String.format("yaml://%s", applicationConfig.getContent());
            //3) client
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

        if (ExecutionMode.YARN_SESSION.equals(application.getExecutionModeEnum())) {
            String yarnSessionClusterId = (String) application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_ID());
            assert yarnSessionClusterId != null;
            extraParameter.put(ConfigConst.KEY_YARN_APP_ID(), yarnSessionClusterId);
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
            throw new IllegalArgumentException("[StreamX] can no found flink version");
        }

        AppBuildPipeline buildPipeline = appBuildPipeService.getById(application.getId());

        assert buildPipeline != null;

        BuildResult buildResult = buildPipeline.getBuildResult();
        if (executionMode.equals(ExecutionMode.YARN_APPLICATION)) {
            buildResult = new ShadedBuildResponse(null, flinkUserJar, true);
        } else {
            if (ExecutionMode.isKubernetesMode(application.getExecutionMode())) {
                DockerImageBuildResponse result = buildResult.as(DockerImageBuildResponse.class);
                String ingressTemplates = application.getIngressTemplate();
                String domainName = application.getDefaultModeIngress();
                if (StringUtils.isNotBlank(ingressTemplates)) {
                    String ingressOutput = result.workspacePath() + "/ingress.yaml";
                    IngressController.configureIngress(ingressOutput);
                }
                if (StringUtils.isNotBlank(domainName)) {
                    IngressController.configureIngress(domainName, application.getClusterId(), application.getK8sNamespace());
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

                //2) 启动完成将任务加入到监控中...
                if (isKubernetesApp(application)) {
                    k8SFlinkTrackMonitor.trackingJob(toTrackId(application));
                } else {
                    FlinkTrackingTask.setOptionState(appParam.getId(), OptionState.STARTING);
                    FlinkTrackingTask.addTracking(application);
                }

                applicationLog.setSuccess(true);
                applicationLogService.save(applicationLog);
                //将savepoint设置为过期
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
        // retracking flink job on kubernetes and logging exception
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

        //1) 动态参数优先级最高,读取动态参数中是否设置: -Dstate.savepoints.dir
        String savepointPath = FlinkSubmitter
            .extractDynamicOptionAsJava(application.getDynamicOptions())
            .get(ConfigConst.KEY_FLINK_STATE_SAVEPOINTS_DIR().substring(6));

        // 2) Application conf配置优先级第二,如果是 streamx|flinksql 类型的任务,则看任务定义时是否配置了Application conf,
        // 如配置了并开启了checkpoints则读取state.savepoints.dir
        if (StringUtils.isBlank(savepointPath)) {
            if (application.isStreamXJob() || application.isFlinkSqlJob()) {
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

        // 3) 以上都未获取到savepoint, 则按照部署类型来尝试获取savepoint路径(remote|on yarn)
        if (StringUtils.isBlank(savepointPath)) {
            // 3.1) 如果是remote模式,则通过restapi请求flink webui接口,获取savepoint路径
            if (ExecutionMode.isRemoteMode(application.getExecutionMode())) {
                FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
                assert cluster != null;
                Map<String, String> config = cluster.getFlinkConfig();
                if (!config.isEmpty()) {
                    savepointPath = config.get(ConfigConst.KEY_FLINK_STATE_SAVEPOINTS_DIR().substring(6));
                }
            } else if (ExecutionMode.isYarnMode(application.getExecutionMode())) {
                // 3.2) 如是 on yarn模式. 则读取绑定的flink里的flink-conf.yml中的savepoint
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
        flameGraph.put("reporter", "com.streamxhub.streamx.plugin.profiling.reporter.HttpReporter");
        flameGraph.put("type", ApplicationType.STREAMX_FLINK.getType());
        flameGraph.put("id", application.getId());
        flameGraph.put("url", settingService.getStreamXAddress().concat("/metrics/report"));
        flameGraph.put("token", Utils.uuid());
        flameGraph.put("sampleInterval", 1000 * 60 * 2);
        flameGraph.put("metricInterval", 1000 * 60 * 2);
        return flameGraph;
    }
}
