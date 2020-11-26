/**
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
package com.streamxhub.console.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.streamxhub.common.conf.ConfigConst;
import com.streamxhub.common.conf.ParameterCli;
import com.streamxhub.common.util.DeflaterUtils;
import com.streamxhub.common.util.HdfsUtils;
import com.streamxhub.common.util.Utils;
import com.streamxhub.common.util.YarnUtils;
import com.streamxhub.console.base.domain.Constant;
import com.streamxhub.console.base.domain.RestRequest;
import com.streamxhub.console.base.utils.CommonUtil;
import com.streamxhub.console.base.utils.SortUtil;
import com.streamxhub.console.core.annotation.Tracking;
import com.streamxhub.console.core.dao.ApplicationMapper;
import com.streamxhub.console.core.entity.*;
import com.streamxhub.console.core.enums.*;
import com.streamxhub.console.core.service.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.console.core.task.FlinkTrackingTask;
import com.streamxhub.console.system.authentication.ServerUtil;
import com.streamxhub.flink.submit.FlinkSubmit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.Manifest;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements ApplicationService {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private ApplicationConfigService configService;

    @Autowired
    private SavePointService savePointService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private ServerUtil serverUtil;

    @Override
    public IPage<Application> page(Application appParam, RestRequest request) {
        Page<Application> page = new Page<>();
        SortUtil.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
        this.baseMapper.page(page, appParam);
        /**
         * 瞒天过海,暗度陈仓,偷天换日,鱼目混珠.
         */
        List<Application> records = page.getRecords();
        List<Application> newRecords = new ArrayList<>(records.size());
        records.stream().forEach((x) -> {
            Application app = FlinkTrackingTask.getTracking(x.getId());
            if (app != null) {
                app.setProjectName(x.getProjectName());
            }
            newRecords.add(app == null ? x : app);
        });
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
     * 检查当前的jobName在表和yarn中是否已经存在
     *
     * @param appParam
     * @return
     */
    @Override
    public AppExistsState checkExists(Application appParam) {
        QueryWrapper<Application> queryWrapper = new QueryWrapper();
        queryWrapper.eq("job_name", appParam.getJobName());
        int count = this.baseMapper.selectCount(queryWrapper);
        boolean exists = YarnUtils.isContains(appParam.getJobName());
        if (count == 0 && !exists) {
            return AppExistsState.NO;
        }
        return exists ? AppExistsState.IN_YARN : AppExistsState.IN_DB;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean create(Application appParam) {
        //配置文件中配置的yarnName..
        appParam.setUserId(serverUtil.getUser().getUserId());
        appParam.setState(FlinkAppState.CREATED.getValue());
        appParam.setCreateTime(new Date());
        boolean saved = save(appParam);
        if (saved) {
            if (appParam.getAppType() == ApplicationType.STREAMX_FLINK.getType()) {
                configService.create(appParam);
            }
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    appParam.setBackUp(false);
                    appParam.setRestart(false);
                    deploy(appParam);
                    return true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    @Tracking
    public boolean update(Application appParam) {
        //update other...
        Application application = getById(appParam.getId());
        application.setJobName(appParam.getJobName());
        application.setArgs(appParam.getArgs());
        application.setOptions(appParam.getOptions());
        application.setDynamicOptions(appParam.getDynamicOptions());
        application.setDescription(appParam.getDescription());
        //update config...
        if (application.getAppType() == ApplicationType.STREAMX_FLINK.getType()) {
            configService.update(appParam);
        } else {
            application.setJar(appParam.getJar());
            application.setMainClass(appParam.getMainClass());
        }
        /**
         * 配置文件已更新
         */
        application.setDeploy(DeployState.CONF_UPDATED.get());
        this.baseMapper.updateById(application);
        return true;
    }

    @Override
    public void deploy(Application appParam) throws Exception {
        Application application = getById(appParam.getId());
        Boolean isRunning = application.getState() == FlinkAppState.RUNNING.getValue();
        //1) 需要重启的先停止服务
        if (appParam.getRestart()) {
            cancel(appParam);
        } else if (!isRunning) {
            //不需要重启的并且未正在运行的,则更改状态为发布中....
            application.setState(FlinkAppState.DEPLOYING.getValue());
            updateState(application);
        }

        //2) deploying...
        application.setBackUpDescription(appParam.getBackUpDescription());
        String workspaceWithModule = application.getWorkspace(true);
        if (HdfsUtils.exists(workspaceWithModule)) {
            ApplicationBackUp applicationBackUp = new ApplicationBackUp(application);
            //3) 需要备份的做备份...
            if (appParam.getBackUp()) {
                backUpService.save(applicationBackUp);
                HdfsUtils.mkdirs(applicationBackUp.getPath());
                HdfsUtils.movie(workspaceWithModule, applicationBackUp.getPath());
            }
        }
        String workspace = application.getWorkspace(false);
        if (!HdfsUtils.exists(workspace)) {
            HdfsUtils.mkdirs(workspace);
        }
        File needUpFile = new File(application.getAppBase(), application.getModule());
        HdfsUtils.upload(needUpFile.getAbsolutePath(), workspace);

        //4) 更新发布状态,需要重启的应用则重新启动...
        if (appParam.getRestart()) {
            //重新启动.
            start(appParam);
            //将"需要重新发布"状态清空...
            application.setDeploy(DeployState.NONE.get());
            updateDeploy(application);
        } else {
            application.setDeploy(DeployState.NEED_START.get());
            updateDeploy(application);
            if (!isRunning) {
                application.setState(FlinkAppState.DEPLOYED.getValue());
                updateState(application);
            }
        }
    }

    @Override
    @Tracking
    public void updateDeploy(Application appParam) {
        this.baseMapper.updateDeploy(appParam);
    }

    @Override
    @Tracking
    public void clean(Application appParam) {
        appParam.setDeploy(DeployState.NONE.get());
        this.baseMapper.updateDeploy(appParam);
    }

    @Override
    public String readConf(Application appParam) throws IOException {
        File file = new File(appParam.getConfig());
        String conf = FileUtils.readFileToString(file, "utf-8");
        return Base64.getEncoder().encodeToString(conf.getBytes());
    }

    @Override
    public Application getApp(Application appParam) {
        FlinkTrackingTask.flushTracking(appParam.getId());
        Application application = this.baseMapper.getApp(appParam);
        if (application.getConfig() != null) {
            String unzipString = DeflaterUtils.unzipString(application.getConfig());
            String encode = Base64.getEncoder().encodeToString(unzipString.getBytes());
            application.setConfig(encode);
        }
        String path = this.projectService.getAppConfPath(application.getProjectId(), application.getModule());
        application.setConfPath(path);
        return application;
    }

    @Override
    public String getMain(Application application) {
        Project project = new Project();
        project.setId(application.getProjectId());
        String modulePath = project.getAppBase().getAbsolutePath().concat("/").concat(application.getModule());
        File jarFile = new File(modulePath, application.getJar());
        Manifest manifest = Utils.getJarManifest(jarFile);
        String mainClass = manifest.getMainAttributes().getValue("Main-Class");
        return mainClass;
    }

    @Override
    @Tracking
    public boolean mapping(Application appParam) {
        boolean mapping = this.baseMapper.mapping(appParam);
        Application application = getById(appParam.getId());
        FlinkTrackingTask.addTracking(application);
        return mapping;
    }

    @Override
    @Tracking
    public void updateState(Application appParam) {
        this.baseMapper.updateState(appParam);
    }

    @Override
    @Tracking
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Application appParam) {
        Application application = getById(appParam.getId());
        application.setState(FlinkAppState.CANCELLING.getValue());
        this.baseMapper.updateById(application);
        //准备停止...
        FlinkTrackingTask.addStopping(appParam.getId());
        String savePointDir = FlinkSubmit.stop(application.getAppId(), application.getJobId(), appParam.getSavePointed(), appParam.getDrain());
        if (appParam.getSavePointed()) {
            SavePoint savePoint = new SavePoint();
            savePoint.setAppId(application.getId());
            savePoint.setLastest(true);
            savePoint.setSavePoint(savePointDir);
            savePoint.setCreateTime(new Date());
            //之前的配置设置为已过期
            this.savePointService.obsolete(application.getId());
            this.savePointService.save(savePoint);
        }
    }

    @Override
    public void updateTracking(Application appParam) {
        this.baseMapper.updateTracking(appParam);
    }

    @Override
    public boolean start(Application appParam) throws Exception {
        final Application application = getById(appParam.getId());
        assert application != null;
        Project project = projectService.getById(application.getProjectId());
        assert project != null;
        String workspaceWithSchemaAndNameService = HdfsUtils.getDefaultFS().concat(ConfigConst.APP_WORKSPACE());

        String appConf, flinkUserJar;
        switch (application.getApplicationType()) {
            case STREAMX_FLINK:
                ApplicationConfig applicationConfig = configService.getActived(application.getId());
                String confContent = applicationConfig.getContent();
                String format = applicationConfig.getFormat() == 1 ? "yaml" : "prop";
                appConf = String.format("%s://%s", format, confContent);
                String classPath = String.format("%s/%s/%s/lib", workspaceWithSchemaAndNameService, application.getId(), application.getModule());
                flinkUserJar = String.format("%s/%s.jar", classPath, application.getModule());
                break;
            case APACHE_FLINK:
                appConf = String.format(
                        "json://{\"%s\":\"%s\"}",
                        ConfigConst.KEY_FLINK_APP_MAIN(),
                        application.getMainClass()
                );
                classPath = String.format("%s/%s/%s", workspaceWithSchemaAndNameService, application.getId(), application.getModule());
                flinkUserJar = String.format("%s/%s", classPath, application.getJar());
                break;
            default:
                throw new IllegalArgumentException("[StreamX] ApplicationType must be (StreamX flink | Apache flink)... ");
        }

        String savePointDir = null;
        if (appParam.getSavePointed()) {
            if (appParam.getSavePoint() == null) {
                SavePoint savePoint = savePointService.getLastest(appParam.getId());
                if (savePoint != null) {
                    savePointDir = savePoint.getSavePoint();
                }
            } else {
                savePointDir = appParam.getSavePoint();
            }
        }

        Map<String, Object> overrideOption = application.getOptionMap();

        if (CommonUtil.notEmpty(overrideOption)) {
            if (appParam.getAllowNonRestored()) {
                overrideOption.put("allowNonRestoredState", true);
            }
        } else {
            if (appParam.getAllowNonRestored()) {
                overrideOption = new HashMap<>(1);
                overrideOption.put("allowNonRestoredState", true);
            }
        }

        String[] dynamicOption = CommonUtil.notEmpty(application.getDynamicOptions())
                ? application.getDynamicOptions().split("\\s+")
                : new String[0];

        FlinkSubmit.SubmitInfo submitInfo = new FlinkSubmit.SubmitInfo(
                flinkUserJar,
                application.getJobName(),
                appConf,
                application.getApplicationType().getName(),
                savePointDir,
                overrideOption,
                dynamicOption,
                application.getArgs()
        );

        ApplicationLog log = new ApplicationLog();
        log.setAppId(application.getId());
        log.setStartTime(new Date());

        try {
            ApplicationId appId = FlinkSubmit.submit(submitInfo);
            /**
             * 一定要在flink job提交完毕才置状态...
             */
            application.setState(FlinkAppState.STARTING.getValue());
            application.setAppId(appId.toString());
            application.setEndTime(null);
            this.baseMapper.updateById(application);
            //加入到跟踪监控中...
            FlinkTrackingTask.addTracking(application);
            log.setYarnAppId(appId.toString());
            log.setSuccess(true);
            applicationLogService.save(log);
            return true;
        } catch (Exception e) {
            String exception = ExceptionUtils.getStackTrace(e);
            log.setException(exception);
            log.setSuccess(false);
            applicationLogService.save(log);
            return false;
        }

    }

}
