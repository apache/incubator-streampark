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
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.SparkExecutionMode;
import org.apache.streampark.common.fs.LfsOperator;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.enums.AppExistsStateEnum;
import org.apache.streampark.console.core.enums.SparkAppStateEnum;
import org.apache.streampark.console.core.mapper.SparkApplicationMapper;
import org.apache.streampark.console.core.runner.EnvInitializer;
import org.apache.streampark.console.core.service.SparkEnvService;
import org.apache.streampark.console.core.service.application.SparkApplicationInfoService;
import org.apache.streampark.console.core.watcher.SparkAppHttpWatcher;
import org.apache.streampark.flink.core.conf.ParameterCli;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.streampark.common.enums.StorageType.LFS;

@Slf4j
@Service
public class SparkApplicationInfoServiceImpl
    extends
        ServiceImpl<SparkApplicationMapper, SparkApplication>
    implements
        SparkApplicationInfoService {

    private static final int DEFAULT_HISTORY_RECORD_LIMIT = 25;

    private static final int DEFAULT_HISTORY_CONTAINER_IMAGE_RECORD_LIMIT = 5;

    private static final Pattern JOB_NAME_PATTERN = Pattern.compile("^[.\\x{4e00}-\\x{9fa5}A-Za-z\\d_\\-\\s]+$");

    private static final Pattern SINGLE_SPACE_PATTERN = Pattern.compile("^\\S+(\\s\\S+)*$");

    @Autowired
    private SparkEnvService sparkEnvService;

    @Autowired
    private EnvInitializer envInitializer;

    @Override
    public Map<String, Serializable> getDashboardDataMap(Long teamId) {

        // result json
        Long totalNumTasks = 0L;
        Long totalNumCompletedTasks = 0L;
        Long totalNumStages = 0L;
        Long totalNumCompletedStages = 0L;
        Long totalUsedMemory = 0L;
        Long totalUsedVCores = 0L;
        Integer runningApplication = 0;

        for (SparkApplication app : SparkAppHttpWatcher.getWatchingApps()) {
            if (!teamId.equals(app.getTeamId())) {
                continue;
            }
            if (app.getState() == SparkAppStateEnum.RUNNING.getValue()) {
                runningApplication++;
            }
            if (app.getNumTasks() != null) {
                totalNumTasks += app.getNumTasks();
            }
            if (app.getNumCompletedTasks() != null) {
                totalNumCompletedTasks += app.getNumCompletedTasks();
            }
            if (app.getNumStages() != null) {
                totalNumStages += app.getNumStages();
            }
            if (app.getNumCompletedStages() != null) {
                totalNumCompletedStages += app.getNumCompletedStages();
            }
            if (app.getUsedMemory() != null) {
                totalUsedMemory += app.getUsedMemory();
            }
            if (app.getUsedVCores() != null) {
                totalUsedVCores += app.getUsedVCores();
            }
        }

        // result json
        return constructDashboardMap(
            runningApplication, totalNumTasks, totalNumCompletedTasks, totalNumStages, totalNumCompletedStages,
            totalUsedMemory, totalUsedVCores);
    }

    @Nonnull
    private Map<String, Serializable> constructDashboardMap(
                                                            Integer runningApplication,
                                                            Long totalNumTasks,
                                                            Long totalNumCompletedTasks,
                                                            Long totalNumStages,
                                                            Long totalNumCompletedStages,
                                                            Long totalUsedMemory,
                                                            Long totalUsedVCores) {
        Map<String, Serializable> dashboardDataMap = new HashMap<>(8);
        dashboardDataMap.put("runningApplication", runningApplication);
        dashboardDataMap.put("numTasks", totalNumTasks);
        dashboardDataMap.put("numCompletedTasks", totalNumCompletedTasks);
        dashboardDataMap.put("numStages", totalNumStages);
        dashboardDataMap.put("numCompletedStages", totalNumCompletedStages);
        dashboardDataMap.put("usedMemory", totalUsedMemory);
        dashboardDataMap.put("usedVCores", totalUsedVCores);

        return dashboardDataMap;
    }

    @Override
    public boolean checkEnv(SparkApplication appParam) throws ApplicationException {
        SparkApplication application = getById(appParam.getId());
        try {
            SparkEnv sparkEnv;
            if (application.getVersionId() != null) {
                sparkEnv = sparkEnvService.getByIdOrDefault(application.getVersionId());
            } else {
                sparkEnv = sparkEnvService.getDefault();
            }
            if (sparkEnv == null) {
                return false;
            }
            envInitializer.checkSparkEnv(application.getStorageType(), sparkEnv);
            envInitializer.storageInitialize(application.getStorageType());
            return true;
        } catch (Exception e) {
            log.error(ExceptionUtils.stringifyException(e));
            throw new ApiDetailException(e);
        }
    }

    @Override
    public boolean checkAlter(SparkApplication appParam) {
        Long appId = appParam.getId();
        if (SparkAppStateEnum.KILLED != appParam.getStateEnum()) {
            return false;
        }
        long cancelUserId = SparkAppHttpWatcher.getCanceledJobUserId(appId);
        long appUserId = appParam.getUserId();
        return cancelUserId != -1 && cancelUserId != appUserId;
    }

    @Override
    public boolean existsByTeamId(Long teamId) {
        return baseMapper.exists(
            new LambdaQueryWrapper<SparkApplication>().eq(SparkApplication::getTeamId, teamId));
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return baseMapper.exists(
            new LambdaQueryWrapper<SparkApplication>().eq(SparkApplication::getUserId, userId));
    }

    @Override
    public boolean existsBySparkEnvId(Long sparkEnvId) {
        return baseMapper.exists(
            new LambdaQueryWrapper<SparkApplication>().eq(SparkApplication::getVersionId, sparkEnvId));
    }

    @Override
    public List<String> listRecentK8sNamespace() {
        return baseMapper.selectRecentK8sNamespaces(DEFAULT_HISTORY_RECORD_LIMIT);
    }

    @Override
    public List<String> listRecentK8sContainerImage() {
        return baseMapper.selectRecentK8sPodTemplates(DEFAULT_HISTORY_CONTAINER_IMAGE_RECORD_LIMIT);
    }

    @Override
    public List<String> listHistoryUploadJars() {
        return Arrays.stream(LfsOperator.listDir(Workspace.of(LFS).APP_UPLOADS()))
            .filter(File::isFile)
            .sorted(Comparator.comparingLong(File::lastModified).reversed())
            .map(File::getName)
            .filter(fn -> fn.endsWith(Constants.JAR_SUFFIX))
            .limit(DEFAULT_HISTORY_RECORD_LIMIT)
            .collect(Collectors.toList());
    }

    @Override
    public AppExistsStateEnum checkStart(Long id) {
        SparkApplication application = getById(id);
        if (application == null) {
            return AppExistsStateEnum.INVALID;
        }
        if (SparkExecutionMode.isYarnMode(application.getExecutionMode())) {
            boolean exists = !getYarnAppReport(application.getAppName()).isEmpty();
            return exists ? AppExistsStateEnum.IN_YARN : AppExistsStateEnum.NO;
        }
        // todo on k8s check...
        return AppExistsStateEnum.NO;
    }

    @Override
    public List<ApplicationReport> getYarnAppReport(String appName) {
        try {
            YarnClient yarnClient = HadoopUtils.yarnClient();
            Set<String> types = Sets.newHashSet(
                ApplicationType.STREAMPARK_SPARK.getName(), ApplicationType.APACHE_SPARK.getName());
            EnumSet<YarnApplicationState> states = EnumSet.of(
                YarnApplicationState.NEW,
                YarnApplicationState.NEW_SAVING,
                YarnApplicationState.SUBMITTED,
                YarnApplicationState.ACCEPTED,
                YarnApplicationState.RUNNING);
            Set<String> yarnTag = Sets.newHashSet("streampark");
            List<ApplicationReport> applications = yarnClient.getApplications(types, states, yarnTag);
            return applications.stream()
                .filter(report -> report.getName().equals(appName))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(
                "getYarnAppReport failed. Ensure that yarn is running properly. ", e);
        }
    }

    @Override
    public String getYarnName(String appConfig) {
        String[] args = new String[2];
        args[0] = "--name";
        args[1] = appConfig;
        return ParameterCli.read(args);
    }

    /**
     * Check if the current jobName and other key identifiers already exist in the database and
     * yarn/k8s.
     *
     * @param appParam The application to check for existence.
     * @return The state of the application's existence.
     */
    @Override
    public AppExistsStateEnum checkExists(SparkApplication appParam) {

        if (!checkJobName(appParam.getAppName())) {
            return AppExistsStateEnum.INVALID;
        }

        boolean existsByJobName = this.existsByAppName(appParam.getAppName());

        if (appParam.getId() != null) {
            SparkApplication app = getById(appParam.getId());
            if (app.getAppName().equals(appParam.getAppName())) {
                return AppExistsStateEnum.NO;
            }

            if (existsByJobName) {
                return AppExistsStateEnum.IN_DB;
            }

            // has stopped status
            if (SparkAppStateEnum.isEndState(app.getState())) {
                // check whether jobName exists on yarn
                if (SparkExecutionMode.isYarnMode(appParam.getExecutionMode())
                    && YarnUtils.isContains(appParam.getAppName())) {
                    return AppExistsStateEnum.IN_YARN;
                }
            }
        } else {
            if (existsByJobName) {
                return AppExistsStateEnum.IN_DB;
            }

            // check whether jobName exists on yarn
            if (SparkExecutionMode.isYarnMode(appParam.getExecutionMode())
                && YarnUtils.isContains(appParam.getAppName())) {
                return AppExistsStateEnum.IN_YARN;
            }
        }
        return AppExistsStateEnum.NO;
    }

    private boolean existsByAppName(String jobName) {
        return baseMapper.exists(
            new LambdaQueryWrapper<SparkApplication>().eq(SparkApplication::getAppName, jobName));
    }

    @Override
    public String readConf(String appConfig) throws IOException {
        File file = new File(appConfig);
        String conf = org.apache.streampark.common.util.FileUtils.readFile(file);
        return Base64.getEncoder().encodeToString(conf.getBytes());
    }

    @Override
    public String getMain(SparkApplication appParam) {
        File jarFile = null;
        if (appParam.getProjectId() == null) {
            jarFile = new File(appParam.getJar());
        }
        return Utils.getJarManClass(jarFile);
    }

    private Boolean checkJobName(String jobName) {
        if (!StringUtils.isBlank(jobName.trim())) {
            return JOB_NAME_PATTERN.matcher(jobName).matches()
                && SINGLE_SPACE_PATTERN.matcher(jobName).matches();
        }
        return false;
    }
}
