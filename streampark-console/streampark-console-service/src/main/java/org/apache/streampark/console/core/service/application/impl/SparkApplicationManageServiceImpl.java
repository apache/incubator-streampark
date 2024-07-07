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

package org.apache.streampark.console.core.service.application.impl;

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.SparkExecutionMode;
import org.apache.streampark.common.enums.StorageType;
import org.apache.streampark.common.fs.HdfsOperator;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.bean.AppControl;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.enums.CandidateTypeEnum;
import org.apache.streampark.console.core.enums.ChangeTypeEnum;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.mapper.SparkApplicationMapper;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.EffectiveService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ProjectService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.ServiceHelper;
import org.apache.streampark.console.core.service.SettingService;
import org.apache.streampark.console.core.service.YarnQueueService;
import org.apache.streampark.console.core.service.application.SparkApplicationManageService;
import org.apache.streampark.flink.packer.pipeline.PipelineStatusEnum;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.annotations.VisibleForTesting;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkApplicationManageServiceImpl
        extends
            ServiceImpl<SparkApplicationMapper, SparkApplication>
        implements
            SparkApplicationManageService {

    private static final String ERROR_APP_QUEUE_HINT =
            "Queue label '%s' isn't available for teamId '%d', please add it into the team first.";

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private ApplicationConfigService configService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private EffectiveService effectiveService;

    @Autowired
    private SettingService settingService;

    @Autowired
    private ServiceHelper serviceHelper;

    @Autowired
    private AppBuildPipeService appBuildPipeService;

    @Autowired
    private YarnQueueService yarnQueueService;

    @Autowired
    private ResourceService resourceService;

    @PostConstruct
    public void resetOptionState() {
        this.baseMapper.resetOptionState();
    }

    @Override
    public void toEffective(SparkApplication appParam) {
        // set latest to Effective
        ApplicationConfig config = configService.getLatest(appParam.getId());
        if (config != null) {
            this.configService.toEffective(appParam.getId(), config.getId());
        }
        if (appParam.isSparkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getCandidate(appParam.getId(), null);
            if (flinkSql != null) {
                flinkSqlService.toEffective(appParam.getId(), flinkSql.getId());
                // clean candidate
                flinkSqlService.cleanCandidate(flinkSql.getId());
            }
        }
    }

    @Override
    public void persistMetrics(SparkApplication appParam) {
        this.baseMapper.persistMetrics(appParam);
    }

    @Override
    public boolean mapping(SparkApplication appParam) {
        boolean mapping = this.baseMapper.mapping(appParam);
        SparkApplication application = getById(appParam.getId());
        return mapping;
    }

    @Override
    public Boolean remove(Long appId) {

        SparkApplication application = getById(appId);

        // 1) remove flink sql
        flinkSqlService.removeByAppId(application.getId());

        // 2) remove log
        applicationLogService.removeByAppId(application.getId());

        // 3) remove config
        configService.removeByAppId(application.getId());

        // 4) remove effective
        effectiveService.removeByAppId(application.getId());

        // remove related hdfs
        // 5) remove backup
        // backUpService.remove(application);

        // 6) remove savepoint
        // savePointService.remove(application);

        // 7) remove BuildPipeline
        appBuildPipeService.removeByAppId(application.getId());

        // 8) remove app
        removeApp(application);
        return true;
    }

    private void removeApp(SparkApplication application) {
        Long appId = application.getId();
        removeById(appId);
        try {
            application
                    .getFsOperator()
                    .delete(application.getWorkspace().APP_WORKSPACE().concat("/").concat(appId.toString()));
            // try to delete yarn-application, and leave no trouble.
            String path = Workspace.of(StorageType.HDFS).APP_WORKSPACE().concat("/").concat(appId.toString());
            if (HdfsOperator.exists(path)) {
                HdfsOperator.delete(path);
            }
        } catch (Exception e) {
            // skip
        }
    }

    @Override
    public IPage<SparkApplication> page(SparkApplication appParam, RestRequest request) {
        if (appParam.getTeamId() == null) {
            return null;
        }
        Page<SparkApplication> page = MybatisPager.getPage(request);

        if (ArrayUtils.isNotEmpty(appParam.getStateArray())
                && Arrays.stream(appParam.getStateArray())
                        .anyMatch(x -> x == FlinkAppStateEnum.FINISHED.getValue())) {
            Integer[] newArray = ArrayUtils.insert(
                    appParam.getStateArray().length,
                    appParam.getStateArray(),
                    FlinkAppStateEnum.POS_TERMINATED.getValue());
            appParam.setStateArray(newArray);
        }
        this.baseMapper.selectPage(page, appParam);
        List<SparkApplication> records = page.getRecords();
        long now = System.currentTimeMillis();

        List<Long> appIds = records.stream().map(SparkApplication::getId).collect(Collectors.toList());
        Map<Long, PipelineStatusEnum> pipeStates = appBuildPipeService.listAppIdPipelineStatusMap(appIds);

        List<SparkApplication> newRecords = records.stream()
                .peek(
                        record -> {
                            if (pipeStates.containsKey(record.getId())) {
                                record.setBuildStatus(pipeStates.get(record.getId()).getCode());
                            }

                            AppControl appControl = new AppControl()
                                    .setAllowBuild(
                                            record.getBuildStatus() == null
                                                    || !PipelineStatusEnum.running
                                                            .getCode()
                                                            .equals(record.getBuildStatus()))
                                    .setAllowStart(
                                            !record.shouldTracking()
                                                    && PipelineStatusEnum.success
                                                            .getCode()
                                                            .equals(record.getBuildStatus()))
                                    .setAllowStop(record.isRunning());
                            record.setAppControl(appControl);
                        })
                .collect(Collectors.toList());
        page.setRecords(newRecords);
        return page;
    }

    @Override
    public void changeOwnership(Long userId, Long targetUserId) {
        LambdaUpdateWrapper<SparkApplication> updateWrapper = new LambdaUpdateWrapper<SparkApplication>()
                .eq(SparkApplication::getUserId, userId)
                .set(SparkApplication::getUserId, targetUserId);
        this.baseMapper.update(null, updateWrapper);
    }

    @SneakyThrows
    @Override
    public boolean create(SparkApplication appParam) {
        ApiAlertException.throwIfNull(
                appParam.getTeamId(), "The teamId can't be null. Create application failed.");
        appParam.setUserId(serviceHelper.getUserId());
        appParam.setState(FlinkAppStateEnum.ADDED.getValue());
        appParam.setRelease(ReleaseStateEnum.NEED_RELEASE.get());
        appParam.setOptionState(OptionStateEnum.NONE.getValue());
        appParam.setCreateTime(new Date());
        appParam.setModifyTime(new Date());
        appParam.setDefaultModeIngress(settingService.getIngressModeDefault());

        boolean success = validateQueueIfNeeded(appParam);
        ApiAlertException.throwIfFalse(
                success,
                String.format(ERROR_APP_QUEUE_HINT, appParam.getYarnQueue(), appParam.getTeamId()));

        appParam.doSetHotParams();
        if (appParam.isUploadJob()) {
            String jarPath = String.format(
                    "%s/%d/%s", Workspace.local().APP_UPLOADS(), appParam.getTeamId(), appParam.getJar());
            if (!new File(jarPath).exists()) {
                Resource resource = resourceService.findByResourceName(appParam.getTeamId(), appParam.getJar());
                if (resource != null && StringUtils.isNotBlank(resource.getFilePath())) {
                    jarPath = resource.getFilePath();
                }
            }
            appParam.setJarCheckSum(org.apache.commons.io.FileUtils.checksumCRC32(new File(jarPath)));
        }

        if (save(appParam)) {
            if (appParam.isSparkSqlJob()) {
                FlinkSql flinkSql = new FlinkSql(appParam);
                flinkSqlService.create(flinkSql);
            }
            if (appParam.getConfig() != null) {
                // configService.create(appParam, true);
            }
            return true;
        } else {
            throw new ApiAlertException("create application failed");
        }
    }

    private boolean existsByJobName(String jobName) {
        return baseMapper.exists(
                new LambdaQueryWrapper<SparkApplication>().eq(SparkApplication::getJobName, jobName));
    }

    @SuppressWarnings("checkstyle:WhitespaceAround")
    @Override
    @SneakyThrows
    public Long copy(SparkApplication appParam) {
        boolean existsByJobName = this.existsByJobName(appParam.getJobName());
        ApiAlertException.throwIfFalse(
                !existsByJobName,
                "[StreamPark] Application names can't be repeated, copy application failed.");

        SparkApplication oldApp = getById(appParam.getId());
        SparkApplication newApp = new SparkApplication();
        String jobName = appParam.getJobName();

        newApp.setJobName(jobName);
        newApp.setClusterId(jobName);
        newApp.setArgs(appParam.getArgs() != null ? appParam.getArgs() : oldApp.getArgs());
        newApp.setVersionId(oldApp.getVersionId());

        newApp.setSparkClusterId(oldApp.getSparkClusterId());
        newApp.setRestartSize(oldApp.getRestartSize());
        newApp.setJobType(oldApp.getJobType());
        newApp.setOptions(oldApp.getOptions());
        newApp.setDynamicProperties(oldApp.getDynamicProperties());
        newApp.setResolveOrder(oldApp.getResolveOrder());
        newApp.setExecutionMode(oldApp.getExecutionMode());
        newApp.setSparkImage(oldApp.getSparkImage());
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
        newApp.setUserId(serviceHelper.getUserId());
        newApp.setState(FlinkAppStateEnum.ADDED.getValue());
        newApp.setRelease(ReleaseStateEnum.NEED_RELEASE.get());
        newApp.setOptionState(OptionStateEnum.NONE.getValue());
        newApp.setCreateTime(new Date());
        newApp.setModifyTime(new Date());
        newApp.setHotParams(oldApp.getHotParams());

        newApp.setJar(oldApp.getJar());
        newApp.setJarCheckSum(oldApp.getJarCheckSum());
        newApp.setTags(oldApp.getTags());
        newApp.setTeamId(oldApp.getTeamId());
        newApp.setHadoopUser(oldApp.getHadoopUser());

        boolean saved = save(newApp);
        if (saved) {
            if (newApp.isSparkSqlJob()) {
                FlinkSql copyFlinkSql = flinkSqlService.getLatestFlinkSql(appParam.getId(), true);
                newApp.setSparkSql(copyFlinkSql.getSql());
                newApp.setTeamResource(copyFlinkSql.getTeamResource());
                newApp.setDependency(copyFlinkSql.getDependency());
                FlinkSql flinkSql = new FlinkSql(newApp);
                flinkSqlService.create(flinkSql);
            }
            ApplicationConfig copyConfig = configService.getEffective(appParam.getId());
            if (copyConfig != null) {
                ApplicationConfig config = new ApplicationConfig();
                config.setAppId(newApp.getId());
                config.setFormat(copyConfig.getFormat());
                config.setContent(copyConfig.getContent());
                config.setCreateTime(new Date());
                config.setVersion(1);
                configService.save(config);
                configService.setLatestOrEffective(true, config.getId(), newApp.getId());
            }
            return newApp.getId();
        } else {
            throw new ApiAlertException(
                    "create application from copy failed, copy source app: " + oldApp.getJobName());
        }
    }

    @Override
    public boolean update(SparkApplication appParam) {
        SparkApplication application = getById(appParam.getId());

        /* If the original mode is remote, k8s-session, yarn-session, check cluster status */
        SparkExecutionMode sparkExecutionMode = application.getSparkExecutionMode();

        boolean success = validateQueueIfNeeded(application, appParam);
        ApiAlertException.throwIfFalse(
                success,
                String.format(ERROR_APP_QUEUE_HINT, appParam.getYarnQueue(), appParam.getTeamId()));

        application.setRelease(ReleaseStateEnum.NEED_RELEASE.get());

        // 1) jar job jar file changed
        if (application.isUploadJob()) {
            if (!Objects.equals(application.getJar(), appParam.getJar())) {
                application.setBuild(true);
            } else {
                File jarFile = new File(WebUtils.getAppTempDir(), appParam.getJar());
                if (jarFile.exists()) {
                    try {
                        long checkSum = org.apache.commons.io.FileUtils.checksumCRC32(jarFile);
                        if (!Objects.equals(checkSum, application.getJarCheckSum())) {
                            application.setBuild(true);
                        }
                    } catch (IOException e) {
                        log.error("Error in checksumCRC32 for {}.", jarFile);
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        // 3) flink version changed
        if (!application.getBuild()
                && !Objects.equals(application.getVersionId(), appParam.getVersionId())) {
            application.setBuild(true);
        }

        // 4) yarn application mode change
        if (!application.getBuild() && isYarnApplicationModeChange(application, appParam)) {
            application.setBuild(true);
        }

        appParam.setJobType(application.getJobType());
        // changes to the following parameters need to be re-release to take effect
        application.setJobName(appParam.getJobName());
        application.setVersionId(appParam.getVersionId());
        application.setArgs(appParam.getArgs());
        application.setOptions(appParam.getOptions());
        application.setDynamicProperties(appParam.getDynamicProperties());
        application.setResolveOrder(appParam.getResolveOrder());
        application.setExecutionMode(appParam.getExecutionMode());
        application.setClusterId(appParam.getClusterId());
        application.setSparkImage(appParam.getSparkImage());
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
        application.setTags(appParam.getTags());

        switch (appParam.getSparkExecutionMode()) {
            case YARN_CLUSTER:
            case YARN_CLIENT:
                application.setHadoopUser(appParam.getHadoopUser());
                break;
            case REMOTE:
                application.setSparkClusterId(appParam.getSparkClusterId());
                break;
            default:
                break;
        }

        // Flink Sql job...
        if (application.isSparkSqlJob()) {
            updateFlinkSqlJob(application, appParam);
            return true;
        }

        if (application.isStreamParkJob()) {
            // configService.update(appParam, application.isRunning());
        } else {
            application.setJar(appParam.getJar());
            application.setMainClass(appParam.getMainClass());
        }
        this.updateById(application);
        return true;
    }

    /**
     * update FlinkSql type jobs, there are 3 aspects to consider<br>
     * 1. flink sql has changed <br>
     * 2. dependency has changed<br>
     * 3. parameter has changed<br>
     *
     * @param application
     * @param appParam
     */
    private void updateFlinkSqlJob(SparkApplication application, SparkApplication appParam) {
        FlinkSql effectiveFlinkSql = flinkSqlService.getEffective(application.getId(), true);
        if (effectiveFlinkSql == null) {
            effectiveFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateTypeEnum.NEW);
            flinkSqlService.removeById(effectiveFlinkSql.getId());
            FlinkSql sql = new FlinkSql(appParam);
            flinkSqlService.create(sql);
            application.setBuild(true);
        } else {
            // get previous flink sql and decode
            FlinkSql copySourceFlinkSql = flinkSqlService.getById(appParam.getSqlId());
            ApiAlertException.throwIfNull(
                    copySourceFlinkSql, "Flink sql is null, update flink sql job failed.");
            copySourceFlinkSql.decode();

            // get submit flink sql
            FlinkSql targetFlinkSql = new FlinkSql(appParam);

            // judge sql and dependency has changed
            ChangeTypeEnum changeTypeEnum = copySourceFlinkSql.checkChange(targetFlinkSql);

            log.info("updateFlinkSqlJob changeTypeEnum: {}", changeTypeEnum);

            // if has been changed
            if (changeTypeEnum.hasChanged()) {
                // check if there is a candidate version for the newly added record
                FlinkSql newFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateTypeEnum.NEW);
                // If the candidate version of the new record exists, it will be deleted directly,
                // and only one candidate version will be retained. If the new candidate version is not
                // effective,
                // if it is edited again and the next record comes in, the previous candidate version will
                // be deleted.
                if (newFlinkSql != null) {
                    // delete all records about candidates
                    flinkSqlService.removeById(newFlinkSql.getId());
                }
                FlinkSql historyFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateTypeEnum.HISTORY);
                // remove candidate flags that already exist but are set as candidates
                if (historyFlinkSql != null) {
                    flinkSqlService.cleanCandidate(historyFlinkSql.getId());
                }
                FlinkSql sql = new FlinkSql(appParam);
                flinkSqlService.create(sql);
                if (changeTypeEnum.isDependencyChanged()) {
                    application.setBuild(true);
                }
            } else {
                // judge version has changed
                boolean versionChanged = !effectiveFlinkSql.getId().equals(appParam.getSqlId());
                if (versionChanged) {
                    // sql and dependency not changed, but version changed, means that rollback to the version
                    CandidateTypeEnum type = CandidateTypeEnum.HISTORY;
                    flinkSqlService.setCandidate(type, appParam.getId(), appParam.getSqlId());
                    application.setRelease(ReleaseStateEnum.NEED_ROLLBACK.get());
                    application.setBuild(true);
                }
            }
        }
        this.updateById(application);
        // this.configService.update(appParam, application.isRunning());
    }

    @Override
    public void updateRelease(SparkApplication appParam) {
        LambdaUpdateWrapper<SparkApplication> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(SparkApplication::getId, appParam.getId());
        updateWrapper.set(SparkApplication::getRelease, appParam.getRelease());
        updateWrapper.set(SparkApplication::getBuild, appParam.getBuild());
        if (appParam.getOptionState() != null) {
            updateWrapper.set(SparkApplication::getOptionState, appParam.getOptionState());
        }
        this.update(updateWrapper);
    }

    @Override
    public List<SparkApplication> listByProjectId(Long id) {
        return baseMapper.selectAppsByProjectId(id);
    }

    @Override
    public List<SparkApplication> listByTeamId(Long teamId) {
        return baseMapper.selectAppsByTeamId(teamId);
    }

    @Override
    public List<SparkApplication> listByTeamIdAndExecutionModes(
                                                                Long teamId,
                                                                Collection<SparkExecutionMode> executionModeEnums) {
        return getBaseMapper()
                .selectList(
                        new LambdaQueryWrapper<SparkApplication>()
                                .eq((SFunction<SparkApplication, Long>) SparkApplication::getTeamId, teamId)
                                .in(
                                        SparkApplication::getExecutionMode,
                                        executionModeEnums.stream()
                                                .map(SparkExecutionMode::getMode)
                                                .collect(Collectors.toSet())));
    }

    @Override
    public boolean checkBuildAndUpdate(SparkApplication appParam) {
        boolean build = appParam.getBuild();
        if (!build) {
            LambdaUpdateWrapper<SparkApplication> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.eq(SparkApplication::getId, appParam.getId());
            if (appParam.isRunning()) {
                updateWrapper.set(SparkApplication::getRelease, ReleaseStateEnum.NEED_RESTART.get());
            } else {
                updateWrapper.set(SparkApplication::getRelease, ReleaseStateEnum.DONE.get());
                updateWrapper.set(SparkApplication::getOptionState, OptionStateEnum.NONE.getValue());
            }
            this.update(updateWrapper);

            // If the current task is not running, or the task has just been added,
            // directly set the candidate version to the official version
            FlinkSql flinkSql = flinkSqlService.getEffective(appParam.getId(), false);
            if (!appParam.isRunning() || flinkSql == null) {
                this.toEffective(appParam);
            }
        }
        return build;
    }

    @Override
    public void clean(SparkApplication appParam) {
        appParam.setRelease(ReleaseStateEnum.DONE.get());
        this.updateRelease(appParam);
    }

    @Override
    public SparkApplication getApp(Long id) {
        SparkApplication application = this.baseMapper.selectApp(id);
        ApplicationConfig config = configService.getEffective(id);
        config = config == null ? configService.getLatest(id) : config;
        if (config != null) {
            config.setToApplication(application);
        }
        if (application.isSparkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), true);
            if (flinkSql == null) {
                flinkSql = flinkSqlService.getCandidate(application.getId(), CandidateTypeEnum.NEW);
                flinkSql.setSql(DeflaterUtils.unzipString(flinkSql.getSql()));
            }
            flinkSql.setToApplication(application);
        } else {
            if (application.isCICDJob()) {
                String path = this.projectService.getAppConfPath(application.getProjectId(), application.getModule());
                application.setConfPath(path);
            }
        }

        application.setYarnQueueByHotParams();

        return application;
    }

    /**
     * Check queue label validation when create the application if needed.
     *
     * @param appParam the app to create.
     * @return <code>true</code> if validate it successfully, <code>false</code> else.
     */
    @VisibleForTesting
    public boolean validateQueueIfNeeded(SparkApplication appParam) {
        yarnQueueService.checkQueueLabel(appParam.getSparkExecutionMode(), appParam.getYarnQueue());
        if (!isYarnNotDefaultQueue(appParam)) {
            return true;
        }
        return yarnQueueService.existByTeamIdQueueLabel(appParam.getTeamId(), appParam.getYarnQueue());
    }

    /**
     * Check queue label validation when update the application if needed.
     *
     * @param oldApp the old app to update.
     * @param newApp the new app payload.
     * @return <code>true</code> if validate it successfully, <code>false</code> else.
     */
    @VisibleForTesting
    public boolean validateQueueIfNeeded(SparkApplication oldApp, SparkApplication newApp) {
        yarnQueueService.checkQueueLabel(newApp.getSparkExecutionMode(), newApp.getYarnQueue());
        if (!isYarnNotDefaultQueue(newApp)) {
            return true;
        }

        oldApp.setYarnQueueByHotParams();
        if (SparkExecutionMode.isYarnMode(newApp.getSparkExecutionMode())
                && StringUtils.equals(oldApp.getYarnQueue(), newApp.getYarnQueue())) {
            return true;
        }
        return yarnQueueService.existByTeamIdQueueLabel(newApp.getTeamId(), newApp.getYarnQueue());
    }

    /**
     * Judge the execution mode whether is the Yarn PerJob or Application mode with not default or
     * empty queue label.
     *
     * @param application application entity.
     * @return If the executionMode is (Yarn PerJob or application mode) and the queue label is not
     *     (empty or default), return true, false else.
     */
    private boolean isYarnNotDefaultQueue(SparkApplication application) {
        return SparkExecutionMode.isYarnMode(application.getSparkExecutionMode())
                && !yarnQueueService.isDefaultQueue(application.getYarnQueue());
    }

    private boolean isYarnApplicationModeChange(
                                                SparkApplication application, SparkApplication appParam) {
        return !application.getExecutionMode().equals(appParam.getExecutionMode())
                && (SparkExecutionMode.YARN_CLIENT == appParam.getSparkExecutionMode()
                        || SparkExecutionMode.YARN_CLUSTER == application.getSparkExecutionMode());
    }
}
