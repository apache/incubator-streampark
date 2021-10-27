/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.streamxhub.streamx.console.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.conf.ConfigurationOptions;
import com.streamxhub.streamx.common.conf.Workspace;
import com.streamxhub.streamx.common.domain.FlinkMemorySize;
import com.streamxhub.streamx.common.enums.DevelopmentMode;
import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.enums.ResolveOrder;
import com.streamxhub.streamx.common.enums.StorageType;
import com.streamxhub.streamx.common.fs.FsOperator;
import com.streamxhub.streamx.common.util.*;
import com.streamxhub.streamx.console.base.domain.Constant;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.exception.ServiceException;
import com.streamxhub.streamx.console.base.util.CommonUtils;
import com.streamxhub.streamx.console.base.util.SortUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.annotation.RefreshCache;
import com.streamxhub.streamx.console.core.dao.ApplicationMapper;
import com.streamxhub.streamx.console.core.entity.*;
import com.streamxhub.streamx.console.core.enums.*;
import com.streamxhub.streamx.console.core.metrics.flink.JobsOverview;
import com.streamxhub.streamx.console.core.runner.EnvInitializer;
import com.streamxhub.streamx.console.core.service.*;
import com.streamxhub.streamx.console.core.task.FlinkTrackingTask;
import com.streamxhub.streamx.console.system.authentication.ServerComponent;
import com.streamxhub.streamx.flink.core.conf.ParameterCli;
import com.streamxhub.streamx.flink.kubernetes.K8sFlinkTrkMonitor;
import com.streamxhub.streamx.flink.kubernetes.model.FlinkMetricCV;
import com.streamxhub.streamx.flink.kubernetes.model.TrkId;
import com.streamxhub.streamx.flink.packer.docker.DockerAuthConf;
import com.streamxhub.streamx.flink.packer.maven.JarPackDeps;
import com.streamxhub.streamx.flink.submit.FlinkSubmitHelper;
import com.streamxhub.streamx.flink.submit.domain.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import scala.collection.JavaConversions;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static com.streamxhub.streamx.console.core.task.K8sFlinkTrkMonitorWrapper.Bridge.toTrkId;
import static com.streamxhub.streamx.console.core.task.K8sFlinkTrkMonitorWrapper.isKubernetesApp;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
@DependsOn({"flyway", "flywayInitializer"})
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements ApplicationService {

    private final Map<Long, Long> tailOutMap = new ConcurrentHashMap<>();

    private final Map<Long, StringBuilder> tailBuffer = new ConcurrentHashMap<>();

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

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private FlinkEnvService flinkEnvService;

    @Autowired
    private ApplicationConfigService configService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private SavePointService savePointService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private EffectiveService effectiveService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SettingService settingService;

    @Autowired
    private ServerComponent serverComponent;

    @Autowired
    private EnvInitializer envInitializer;

    @Autowired
    private SimpMessageSendingOperations simpMessageSendingOperations;

    @Autowired
    private K8sFlinkTrkMonitor k8sFlinkTrkMonitor;

    @PostConstruct
    public void resetOptionState() {
        this.baseMapper.resetOptionState();
    }

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
                totalTmMemory += v.getTmMemory();
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

        // mergee metrics from flink kubernetes cluster
        FlinkMetricCV k8sMetric = k8sFlinkTrkMonitor.getAccClusterMetrics();
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
    public boolean upload(MultipartFile file, StorageType storageType) throws Exception {
        envInitializer.storageInitialize(storageType);
        String APP_UPLOADS = Workspace.of(storageType).APP_UPLOADS();
        String uploadFile = APP_UPLOADS.concat("/").concat(Objects.requireNonNull(file.getOriginalFilename()));
        FsOperator fsOperator = FsOperator.of(storageType);
        //1)检查文件是否存在,md5是否一致.
        if (fsOperator.exists(uploadFile)) {
            String md5 = DigestUtils.md5Hex(file.getInputStream());
            //md5一致,则无需在上传.
            if (md5.equals(fsOperator.fileMd5(uploadFile))) {
                return true;
            } else {
                //md5不一致,删除
                fsOperator.delete(uploadFile);
            }
        }

        //2) 确定需要上传,先上传到本地零时目录
        String temp = WebUtils.getAppDir("temp");
        File saveFile = new File(temp, file.getOriginalFilename());
        // delete when exsit
        if (saveFile.exists()) {
            saveFile.delete();
        }
        // save file to temp dir
        FileUtils.writeByteArrayToFile(saveFile, file.getBytes());
        //3) 从本地temp目录上传到hdfs
        fsOperator.upload(saveFile.getAbsolutePath(), APP_UPLOADS, true, true);
        return true;
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
                this.flinkSqlService.toEffective(application.getId(), flinkSql.getId());
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
    public void revoke(Application appParma) throws Exception {
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
            updateWrapper.set(Application::getDeploy, DeployState.NEED_DEPLOY_DOWN_DEPENDENCY_FAILED.get());
        } else {
            updateWrapper.set(Application::getDeploy, DeployState.NEED_DEPLOY_AFTER_BUILD.get());
        }
        if (!application.isRunning()) {
            updateWrapper.set(Application::getState, FlinkAppState.REVOKED.getValue());
        }
        FlinkTrackingTask.refreshTracking(application.getId(), () -> {
            baseMapper.update(application, updateWrapper);
            return null;
        });
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

            //7) 删除 app
            removeApp(application);

            if (isKubernetesApp(paramApp)) {
                k8sFlinkTrkMonitor.unTrackingJob(toTrkId(paramApp));
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
    public boolean checkEnv(Application appParam) {
        Application application = getById(appParam.getId());
        FlinkAppState appState = FlinkAppState.of(application.getState());
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
            updateState(application, FlinkAppState.INITIALIZING);
            envInitializer.checkFlinkEnv(application.getStorageType(), flinkEnv);
            envInitializer.storageInitialize(application.getStorageType());
            return true;
        } catch (Throwable e) {
            log.error(ExceptionUtils.stringifyException(e));
            updateState(application, appState);
            return false;
        }
    }

    @RefreshCache
    private void updateState(Application application, FlinkAppState state) {
        application.setState(state.getValue());
        updateById(application);
    }

    private void removeApp(Application application) {
        Long appId = application.getId();
        removeById(appId);
        application.getFsOperator().delete(application.getWorkspace().APP_WORKSPACE().concat("/").concat(appId.toString()));
    }

    @Override
    public IPage<Application> page(Application appParam, RestRequest request) {
        Page<Application> page = new Page<>();
        SortUtils.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
        this.baseMapper.page(page, appParam);
        //瞒天过海,暗度陈仓,偷天换日,鱼目混珠.
        List<Application> records = page.getRecords();
        long now = System.currentTimeMillis();
        List<Application> newRecords = records.stream().map(record -> {
            // status of flink job on kubernetes mode had been automatically persisted to db in time.
            if (isKubernetesApp(record)) {
                // set duration
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
                state.equals(FlinkAppState.DEPLOYED) ||
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
                    && k8sFlinkTrkMonitor.checkIsInRemoteCluster(toTrkId(appParam))) {
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
                && k8sFlinkTrkMonitor.checkIsInRemoteCluster(toTrkId(appParam))) {
                return AppExistsState.IN_KUBERNETES;
            }
        }
        return AppExistsState.NO;
    }

    @SneakyThrows
    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean create(Application appParam) {
        appParam.setUserId(serverComponent.getUser().getUserId());
        appParam.setState(FlinkAppState.CREATED.getValue());
        appParam.setOptionState(OptionState.NONE.getValue());
        appParam.setCreateTime(new Date());
        boolean saved = save(appParam);
        if (saved) {
            if (appParam.isFlinkSqlJob()) {
                FlinkSql flinkSql = new FlinkSql(appParam);
                flinkSqlService.create(flinkSql, CandidateType.NEW);
            }
            if (appParam.getConfig() != null) {
                configService.create(appParam, true);
            }
            assert appParam.getId() != null;
            deploy(appParam);
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
            //检查任务相关的参数是否发生变化,发生变化则设置需要重启的状态
            if (!appParam.eqJobParam(application)) {
                application.setDeploy(DeployState.NEED_RESTART_AFTER_CONF_UPDATE.get());
            } else if (application.isStreamXJob()) {
                ApplicationConfig config = configService.getEffective(application.getId());
                if (config != null) {
                    if (appParam.getConfigId() == null || !appParam.getConfigId().equals(config.getId())) {
                        application.setDeploy(DeployState.NEED_RESTART_AFTER_CONF_UPDATE.get());
                    } else {
                        String decode = new String(Base64.getDecoder().decode(appParam.getConfig()));
                        String encode = DeflaterUtils.zipString(decode.trim());
                        if (!config.getContent().equals(encode)) {
                            application.setDeploy(DeployState.NEED_RESTART_AFTER_CONF_UPDATE.get());
                        }
                    }
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
            application.setK8sRestExposedType(appParam.getK8sRestExposedType());
            application.setK8sPodTemplate(appParam.getK8sPodTemplate());
            application.setK8sJmPodTemplate(appParam.getK8sJmPodTemplate());
            application.setK8sTmPodTemplate(appParam.getK8sTmPodTemplate());

            //以下参数发生改变不影响正在运行的任务
            application.setDescription(appParam.getDescription());
            application.setAlertEmail(appParam.getAlertEmail());
            application.setRestartSize(appParam.getRestartSize());
            application.setCpFailureAction(appParam.getCpFailureAction());
            application.setCpFailureRateInterval(appParam.getCpFailureRateInterval());
            application.setCpMaxFailureInterval(appParam.getCpMaxFailureInterval());

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
            CandidateType type = (changedType.isDependencyChanged() || application.isRunning()) ? CandidateType.NEW : CandidateType.NONE;
            flinkSqlService.create(sql, type);
            if (changedType.isDependencyChanged()) {
                application.setDeploy(DeployState.NEED_DEPLOY_AFTER_DEPENDENCY_UPDATE.get());
            } else {
                application.setDeploy(DeployState.NEED_RESTART_AFTER_SQL_UPDATE.get());
            }
        } else {
            // 2) 判断版本是否发生变化
            //获取正式版本的flinkSql
            FlinkSql effectiveFlinkSql = flinkSqlService.getEffective(application.getId(), true);
            assert effectiveFlinkSql != null;
            boolean versionChanged = !effectiveFlinkSql.getId().equals(appParam.getSqlId());
            if (versionChanged) {
                //sql和依赖未发生变更,但是版本号发生了变化,说明是回滚到某个版本了
                CandidateType type = CandidateType.HISTORY;
                flinkSqlService.setCandidateOrEffective(type, appParam.getId(), appParam.getSqlId());
                //直接回滚到某个历史版本(rollback)
                application.setDeploy(DeployState.NEED_ROLLBACK.get());
            }
        }
        // 7) 配置文件修改
        this.configService.update(appParam, application.isRunning());
    }

    @Override
    public void deploy(Application application) {
        executorService.submit(() -> {
            try {
                FlinkTrackingTask.refreshTracking(application.getId(), () -> {
                    // 1) 需要重启的先停止服务
                    if (application.getRestart()) {
                        this.cancel(application);
                    }
                    FlinkTrackingTask.refreshTracking(application.getId(), () -> {
                        baseMapper.update(
                            application,
                            new UpdateWrapper<Application>()
                                .lambda()
                                .eq(Application::getId, application.getId())
                                .set(Application::getDeploy, DeployState.DEPLOYING.get())

                        );
                        return null;
                    });

                    LambdaUpdateWrapper<Application> updateWrapper = Wrappers.lambdaUpdate();
                    updateWrapper.eq(Application::getId, application.getId());
                    try {
                        if (application.isCustomCodeJob()) {
                            log.info("CustomCodeJob deploying...");
                            // 2) backup
                            if (application.getBackUp()) {
                                this.backUpService.backup(application);
                            }
                            // 3) deploying...
                            String appHome = application.getAppHome();
                            FsOperator fsOperator = application.getFsOperator();
                            fsOperator.delete(appHome);
                            //本地编译路径
                            String distHome = application.getDistHome();
                            fsOperator.upload(distHome, appHome);
                        } else {
                            log.info("FlinkSqlJob deploying...");
                            FlinkSql flinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.NEW);
                            assert flinkSql != null;
                            application.setDependency(flinkSql.getDependency());
                            downloadDependency(application);
                        }
                        // 4) 更新发布状态,需要重启的应用则重新启动...
                        if (application.getRestart()) {
                            application.setSavePointed(true);
                            // 重新启动.
                            start(application, false);
                            // 将"需要重新发布"状态清空...
                            updateWrapper.set(Application::getDeploy, DeployState.DONE.get());
                        } else {
                            //正在运行的任务...
                            if (application.isRunning()) {
                                updateWrapper.set(Application::getDeploy, DeployState.NEED_RESTART_AFTER_DEPLOY.get());
                            } else {
                                updateWrapper.set(Application::getOptionState, OptionState.NONE.getValue());
                                updateWrapper.set(Application::getDeploy, DeployState.DONE.get());
                                updateWrapper.set(Application::getState, FlinkAppState.DEPLOYED.getValue());
                            }
                        }
                    } catch (Exception e) {
                        Message message = new Message(
                            serverComponent.getUser().getUserId(),
                            application.getId(),
                            application.getJobName().concat(" deploy failed"),
                            ExceptionUtils.stringifyException(e),
                            NoticeType.EXCEPTION
                        );
                        messageService.push(message);
                        updateWrapper.set(Application::getState, FlinkAppState.ADDED.getValue());
                        updateWrapper.set(Application::getOptionState, OptionState.NONE.getValue());
                        updateWrapper.set(Application::getDeploy, DeployState.NEED_DEPLOY_DOWN_DEPENDENCY_FAILED.get());
                    } finally {
                        FlinkTrackingTask.refreshTracking(application.getId(), () -> {
                            baseMapper.update(application, updateWrapper);
                            return null;
                        });
                        //如果当前任务未运行,或者刚刚新增的任务,则直接将候选版本的设置为正式版本
                        FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
                        if (!application.isRunning() || flinkSql == null) {
                            toEffective(application);
                        }
                    }
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void downloadDependency(Application application) throws Exception {
        //1) init.
        File jobLocalHome = application.getLocalFlinkSqlHome();
        if (jobLocalHome.exists()) {
            if (!CommonUtils.deleteFile(jobLocalHome)) {
                throw new RuntimeException(jobLocalHome + " delete failed.");
            }
        }

        File lib = new File(jobLocalHome, "lib");
        if (!lib.mkdirs()) {
            throw new RuntimeException(lib.getAbsolutePath() + " mkdirs failed.");
        }

        //2) maven pom...
        List<Application.Pom> pom = application.getDependencyObject().getPom();
        if (Utils.notEmpty(pom)) {
            log.info("downloadDependency..{}", pom.toString());
            StringBuilder builder = new StringBuilder();
            pom.forEach(x -> {
                String info = String.format("%s:%s:%s,", x.getGroupId(), x.getArtifactId(), x.getVersion());
                builder.append(info);
            });
            String packages = builder.deleteCharAt(builder.length() - 1).toString();
            /*
             * 默认去除以下依赖...
             */
            builder.setLength(0);
            builder.append("org.apache.flink:force-shading,")
                .append("com.google.code.findbugs:jsr305,")
                .append("org.slf4j:*,")
                .append("org.apache.logging.log4j:*,");
            /*
             * 用户指定需要排除的依赖.
             */
            pom.stream().filter(x -> x.getExclusions() != null).forEach(x -> x.getExclusions().forEach(e -> {
                String info = String.format("%s:%s,", e.getGroupId(), e.getArtifactId());
                builder.append(info);
            }));

            String exclusions = builder.deleteCharAt(builder.length() - 1).toString();

            Long id = application.getId();
            StringBuilder logBuilder = this.tailBuffer.getOrDefault(id, new StringBuilder());

            Collection<String> dependencyJars;
            try {
                dependencyJars = JavaConversions.asJavaCollection(DependencyUtils.resolveMavenDependencies(
                    exclusions,
                    packages,
                    null,
                    null,
                    null,
                    out -> {
                        if (tailOutMap.containsKey(id)) {
                            if (tailBeginning.containsKey(id)) {
                                tailBeginning.remove(id);
                                Arrays.stream(logBuilder.toString().split("\n"))
                                    .forEach(x -> simpMessageSendingOperations.convertAndSend("/resp/mvn", x));
                            } else {
                                simpMessageSendingOperations.convertAndSend("/resp/mvn", out);
                            }
                        }
                        logBuilder.append(out).append("\n");
                    }
                ));
            } catch (Exception e) {
                simpMessageSendingOperations.convertAndSend("/resp/mvn", "[Exception] ".concat(e.getMessage()));
                throw new ServiceException("downloadDependency error: " + e.getMessage());
            } finally {
                tailOutMap.remove(id);
                tailBeginning.remove(id);
                tailBuffer.remove(id);
            }

            // 2) backup
            if (application.getBackUp()) {
                this.backUpService.backup(application);
            }

            for (String x : dependencyJars) {
                File jar = new File(x);
                FileUtils.copyFileToDirectory(jar, lib);
            }

            // 3) deploying...
            FsOperator fsOperator = application.getFsOperator();
            fsOperator.delete(application.getAppHome());

            //3) upload jar by pomJar
            fsOperator.delete(application.getAppHome());

            fsOperator.upload(jobLocalHome.getAbsolutePath(), application.getWorkspace().APP_WORKSPACE());

            //4) upload jar by uploadJar
            List<String> jars = application.getDependencyObject().getJar();
            String APP_UPLOADS = application.getWorkspace().APP_UPLOADS();
            if (Utils.notEmpty(jars)) {
                jars.forEach(jar -> {
                    String src = APP_UPLOADS.concat("/").concat(jar);
                    fsOperator.copy(src, application.getAppHome().concat("/lib"), false, true);
                });
            }

        }
    }

    @Override
    @RefreshCache
    public void clean(Application appParam) {
        appParam.setDeploy(DeployState.DONE.get());
        this.baseMapper.updateDeploy(appParam);
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
        if (config != null) {
            config.setToApplication(application);
        }
        if (application.isFlinkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), true);
            flinkSql.setToApplication(application);
        } else {
            String path = this.projectService.getAppConfPath(application.getProjectId(), application.getModule());
            application.setConfPath(path);
        }
        // add flink web url info for k8s-mode
        if (isKubernetesApp(application)) {
            String restUrl = k8sFlinkTrkMonitor.getRemoteRestUrl(toTrkId(application));
            application.setFlinkRestUrl(restUrl);
        }

        return application;
    }

    @Override
    public String getMain(Application application) {
        Project project = new Project();
        project.setId(application.getProjectId());
        String modulePath = project.getDistHome().getAbsolutePath().concat("/").concat(application.getModule());
        File jarFile = new File(modulePath, application.getJar());
        Manifest manifest = Utils.getJarManifest(jarFile);
        return manifest.getMainAttributes().getValue("Main-Class");
    }

    @Override
    @RefreshCache
    public boolean mapping(Application appParam) {
        boolean mapping = this.baseMapper.mapping(appParam);
        Application application = getById(appParam.getId());
        if (isKubernetesApp(application)) {
            k8sFlinkTrkMonitor.unTrackingJob(toTrkId(application));
        } else {
            FlinkTrackingTask.addTracking(application);
        }
        return mapping;
    }

    @Override
    @RefreshCache
    public void cancel(Application appParam) {
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
        this.baseMapper.updateById(application);
        //此步骤可能会比较耗时,重新开启一个线程去执行

        FlinkEnv flinkEnv = flinkEnvService.getById(application.getVersionId());

        executorService.submit(() -> {
            try {
                // intfer savepoint
                String customSavepoint = "";
                if (isKubernetesApp(application)) {
                    customSavepoint = StringUtils.isNotBlank(appParam.getSavePoint()) ? appParam.getSavePoint() :
                        FlinkSubmitHelper
                            .extractDynamicOptionAsJava(application.getDynamicOptions())
                            .getOrDefault(ConfigConst.KEY_FLINK_SAVEPOINT_PATH(), "");
                }
                StopRequest stopInfo = new StopRequest(
                    flinkEnv.toFlinkVersion(),
                    ExecutionMode.of(application.getExecutionMode()),
                    application.getAppId(),
                    application.getJobId(),
                    appParam.getSavePointed(),
                    appParam.getDrain(),
                    customSavepoint,
                    application.getK8sNamespace()
                );

                StopResponse stopResponse = FlinkSubmitHelper.stop(stopInfo);

                assert stopResponse != null;

                String savePointDir = stopResponse.savePointDir();
                if (savePointDir != null) {
                    log.info("savePoint path:{}", savePointDir);
                    log.info("savePoint path:{}", savePointDir);
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
            } catch (Exception e) {
                log.error("stop flink job fail.");
                e.printStackTrace();
                // 保持savepoint失败.则将之前的统统设置为过期
                if (appParam.getSavePointed()) {
                    savePointService.obsolete(application.getId());
                }
                // retracking flink job on kubernetes and logging exception
                if (isKubernetesApp(application)) {
                    TrkId trkid = toTrkId(application);
                    k8sFlinkTrkMonitor.unTrackingJob(trkid);
                    ApplicationLog log = new ApplicationLog();
                    log.setAppId(application.getId());
                    log.setStartTime(new Date());
                    log.setYarnAppId(application.getClusterId());
                    log.setException(ExceptionUtils.stringifyException(e));
                    log.setSuccess(false);
                    applicationLogService.save(log);
                    k8sFlinkTrkMonitor.trackingJob(trkid);
                }
            }
        });
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
        updateById(application);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    @RefreshCache
    public boolean start(Application appParam, boolean auto) throws Exception {

        final Application application = getById(appParam.getId());
        //手动启动的,将reStart清空
        assert application != null;

        if (!auto) {
            application.setRestartCount(0);
        } else {
            if (!application.isNeedRestartOnFailed()) {
                return false;
            }
            application.setRestartCount(application.getRestartCount() + 1);
            application.setSavePointed(true);
        }

        //1) 真正执行启动相关的操作..
        String appConf, flinkUserJar;

        //回滚任务.
        if (application.isNeedRollback()) {
            if (application.isFlinkSqlJob()) {
                flinkSqlService.rollback(application);
            }
        }

        //2) 将lastst的设置为Effective的,(此时才真正变成当前生效的)
        this.toEffective(application);

        //获取一个最新的Effective的配置
        ApplicationConfig applicationConfig = configService.getEffective(application.getId());
        ExecutionMode executionMode = ExecutionMode.of(application.getExecutionMode());

        if (application.isCustomCodeJob()) {
            assert executionMode != null;
            switch (application.getApplicationType()) {
                case STREAMX_FLINK:
                    String format = applicationConfig.getFormat() == 1 ? "yaml" : "prop";
                    appConf = String.format("%s://%s", format, applicationConfig.getContent());
                    flinkUserJar = String.format("%s/lib/%s", application.getAppHome(), application.getModule().concat(".jar"));
                    break;
                case APACHE_FLINK:
                    appConf = String.format("json://{\"%s\":\"%s\"}",
                        ConfigurationOptions.KEY_APPLICATION_MAIN_CLASS,
                        application.getMainClass()
                    );
                    flinkUserJar = String.format("%s/%s", application.getAppHome(), application.getJar());
                    break;
                default:
                    throw new IllegalArgumentException("[StreamX] ApplicationType must be (StreamX flink | Apache flink)... ");
            }
        } else if (application.isFlinkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
            assert flinkSql != null;
            this.flinkSqlService.cleanCandidate(flinkSql.getId());

            //1) dist_userJar
            File localPlugins = new File(WebUtils.getAppDir("plugins"));
            assert localPlugins.exists();
            List<String> jars =
                Arrays.stream(Objects.requireNonNull(localPlugins.list())).filter(x -> x.matches("streamx-flink-sqlclient-.*\\.jar"))
                    .collect(Collectors.toList());
            if (jars.isEmpty()) {
                throw new IllegalArgumentException("[StreamX] can no found streamx-flink-sqlclient jar in " + localPlugins);
            }
            if (jars.size() > 1) {
                throw new IllegalArgumentException("[StreamX] found multiple streamx-flink-sqlclient jar in " + localPlugins);
            }
            String sqlDistJar = jars.get(0);
            //2) appConfig
            appConf = applicationConfig == null ? null : String.format("yaml://%s", applicationConfig.getContent());
            assert executionMode != null;
            //3) plugin
            switch (executionMode) {
                case YARN_PRE_JOB:
                case KUBERNETES_NATIVE_SESSION:
                case KUBERNETES_NATIVE_APPLICATION:
                    flinkUserJar = Workspace.local().APP_PLUGINS().concat("/").concat(sqlDistJar);
                    break;
                case YARN_APPLICATION:
                    String pluginPath = Workspace.remote().APP_PLUGINS();
                    flinkUserJar = String.format("%s/%s", pluginPath, sqlDistJar);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported..." + executionMode);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported...");
        }

        StringBuilder option = new StringBuilder();
        if (appParam.getAllowNonRestored()) {
            option.append(" -n ");
        }

        String[] dynamicOption = CommonUtils.notEmpty(application.getDynamicOptions())
            ? application.getDynamicOptions().split("\\s+")
            : new String[0];

        Map<String, Object> optionMap = application.getOptionMap();
        JarPackDeps jarPackDeps = JarPackDeps.empty();
        if (application.isFlinkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
            jarPackDeps = Application.Dependency.jsonToDependency(flinkSql.getDependency()).toJarPackDeps();
            optionMap.put(ConfigConst.KEY_FLINK_SQL(null), flinkSql.getSql());
            optionMap.put(ConfigConst.KEY_JOB_ID(), application.getId());
        }

        ResolveOrder resolveOrder = ResolveOrder.of(application.getResolveOrder());

        KubernetesSubmitParam kubernetesSubmitParam = new KubernetesSubmitParam(
            application.getClusterId(),
            application.getFlinkImage(),
            application.getK8sNamespace(),
            jarPackDeps,
            new DockerAuthConf(
                settingService.getDockerRegisterAddress(),
                settingService.getDockerRegisterUser(),
                settingService.getDockerRegisterPassword()),
            application.getK8sPodTemplates(),
            application.getK8sRestExposedTypeEnum()
        );

        FlinkEnv flinkEnv = flinkEnvService.getByIdOrDefault(application.getVersionId());
        if (flinkEnv == null) {
            throw new IllegalArgumentException("[StreamX] can no found flink version");
        }

        SubmitRequest submitRequest = new SubmitRequest(
            flinkEnv.toFlinkVersion(),
            flinkEnv.getFlinkConf(),
            flinkUserJar,
            DevelopmentMode.of(application.getJobType()),
            ExecutionMode.of(application.getExecutionMode()),
            resolveOrder,
            application.getJobName(),
            appConf,
            application.getApplicationType().getName(),
            getSavePointed(appParam),
            appParam.getFlameGraph() ? getFlameGraph(application) : null,
            option.toString(),
            optionMap,
            dynamicOption,
            application.getArgs(),
            kubernetesSubmitParam
        );

        ApplicationLog applicationLog = new ApplicationLog();
        applicationLog.setAppId(application.getId());
        applicationLog.setStartTime(new Date());

        try {

            SubmitResponse submitResponse = FlinkSubmitHelper.submit(submitRequest);

            assert submitResponse != null;

            if (submitResponse.flinkConfig() != null) {
                String jmMemory = submitResponse.flinkConfig().get(ConfigurationOptions.KEY_TOTAL_PROCESS_MEMORY);
                if (jmMemory != null) {
                    application.setJmMemory(FlinkMemorySize.parse(jmMemory).getMebiBytes());
                }
                String tmMemory = submitResponse.flinkConfig().get(ConfigurationOptions.KEY_TOTAL_PROCESS_MEMORY);
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
                application.setDeploy(DeployState.DONE.get());
            }
            updateById(application);

            //2) 启动完成将任务加入到监控中...
            // 更改操作状态...x
            if (isKubernetesApp(application)) {
                k8sFlinkTrkMonitor.trackingJob(toTrkId(application));
            } else {
                FlinkTrackingTask.setOptionState(appParam.getId(), OptionState.STARTING);
                // 加入到跟踪监控中...
                FlinkTrackingTask.addTracking(application);
            }

            applicationLog.setSuccess(true);
            applicationLogService.save(applicationLog);
            //将savepoint设置为过期
            savePointService.obsolete(application.getId());
            return true;
        } catch (Exception e) {
            String exception = ExceptionUtils.stringifyException(e);
            applicationLog.setException(exception);
            applicationLog.setSuccess(false);
            applicationLogService.save(applicationLog);
            Application app = getById(appParam.getId());
            app.setState(FlinkAppState.FAILED.getValue());
            app.setOptionState(OptionState.NONE.getValue());
            updateById(app);
            if (isKubernetesApp(app)) {
                k8sFlinkTrkMonitor.unTrackingJob(toTrkId(app));
            } else {
                FlinkTrackingTask.stopTracking(appParam.getId());
            }
            return false;
        }
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
