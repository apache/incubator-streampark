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
package com.streamxhub.monitor.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.streamxhub.common.conf.ConfigConst;
import com.streamxhub.common.conf.ParameterCli;
import com.streamxhub.common.util.DeflaterUtils;
import com.streamxhub.common.util.HdfsUtils;
import com.streamxhub.common.util.ThreadUtils;
import com.streamxhub.common.util.YarnUtils;
import com.streamxhub.monitor.base.domain.Constant;
import com.streamxhub.monitor.base.domain.RestRequest;
import com.streamxhub.monitor.base.properties.StreamXProperties;
import com.streamxhub.monitor.base.utils.CommonUtil;
import com.streamxhub.monitor.base.utils.SortUtil;
import com.streamxhub.monitor.core.dao.ApplicationMapper;
import com.streamxhub.monitor.core.entity.Application;
import com.streamxhub.monitor.core.entity.ApplicationBackUp;
import com.streamxhub.monitor.core.entity.ApplicationConfig;
import com.streamxhub.monitor.core.entity.Project;
import com.streamxhub.monitor.core.enums.AppExistsState;
import com.streamxhub.monitor.core.service.ApplicationBackUpService;
import com.streamxhub.monitor.core.service.ApplicationConfigService;
import com.streamxhub.monitor.core.service.ApplicationService;
import com.streamxhub.monitor.core.service.ProjectService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.monitor.core.enums.FlinkAppState;
import com.streamxhub.monitor.system.authentication.ServerUtil;
import com.streamxhub.flink.submit.FlinkSubmit;
import com.streamxhub.flink.submit.SubmitInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.flink.yarn.configuration.YarnDeploymentTarget;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author benjobs
 */
@Slf4j
@Service("applicationService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements ApplicationService {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private ApplicationConfigService configService;

    @Autowired
    private ServerUtil serverUtil;

    @Autowired
    private StreamXProperties properties;


    @Override
    public IPage<Application> list(Application paramOfApp, RestRequest request) {
        Page<Application> page = new Page<>();
        SortUtil.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
        return this.baseMapper.findApplication(page, paramOfApp);
    }

    @Override
    public String getYarnName(Application paramOfApp) {
        String[] args = new String[2];
        args[0] = "--name";
        args[1] = paramOfApp.getConfig();
        return ParameterCli.read(args);
    }

    /**
     * 检查当前的jobName在表和yarn中是否已经存在
     *
     * @param paramOfApp
     * @return
     */
    @Override
    public AppExistsState checkExists(Application paramOfApp) {
        QueryWrapper<Application> queryWrapper = new QueryWrapper();
        queryWrapper.eq("job_name", paramOfApp.getJobName());
        int count = this.baseMapper.selectCount(queryWrapper);
        boolean exists = YarnUtils.isContains(paramOfApp.getJobName());
        if (count == 0 && !exists) {
            return AppExistsState.NO;
        }
        return exists ? AppExistsState.IN_YARN : AppExistsState.IN_DB;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean create(Application paramOfApp) {
        //配置文件中配置的yarnName..
        paramOfApp.setUserId(serverUtil.getUser().getUserId());
        paramOfApp.setState(FlinkAppState.CREATED.getValue());
        paramOfApp.setCreateTime(new Date());
        paramOfApp.setModule(paramOfApp.getModule().replace(paramOfApp.getAppBase().getAbsolutePath() + "/", ""));
        boolean saved = save(paramOfApp);
        configService.create(paramOfApp);
        if (saved) {
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    deploy(paramOfApp, false);
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
    public boolean update(Application paramOfApp) {
        //update config...
        configService.update(paramOfApp);

        //update other...
        Application application = getById(paramOfApp.getId());
        application.setJobName(paramOfApp.getJobName());
        application.setArgs(paramOfApp.getArgs());
        application.setOptions(paramOfApp.getOptions());
        application.setShortOptions(paramOfApp.getShortOptions());
        application.setDynamicOptions(paramOfApp.getDynamicOptions());
        application.setDescription(paramOfApp.getDescription());
        /**
         * 配置文件已更新
         */
        application.setDeploy(2);
        this.baseMapper.updateById(application);
        return true;
    }

    @Override
    public void deploy(Application paramOfApp, boolean backUp) throws IOException {
        //先停止原有任务..
        Application application = getById(paramOfApp.getId());
        application.setBackUpDescription(paramOfApp.getBackUpDescription());
        if (application.getState() == FlinkAppState.RUNNING.getValue()) {
            stop(application);
        }

        //更改状态为发布中....
        application.setState(FlinkAppState.DEPLOYING.getValue());
        updateState(application);

        if (!application.getModule().startsWith(application.getAppBase().getAbsolutePath())) {
            application.setModule(application.getAppBase().getAbsolutePath().concat("/").concat(application.getModule()));
        }

        String workspaceWithModule = application.getWorkspace(true);
        if (HdfsUtils.exists(workspaceWithModule)) {
            ApplicationBackUp applicationBackUp = new ApplicationBackUp(application);
            backUpService.save(applicationBackUp);
            HdfsUtils.mkdirs(applicationBackUp.getPath());
            HdfsUtils.movie(workspaceWithModule, applicationBackUp.getPath());
        }

        String workspace = application.getWorkspace(false);
        if (!HdfsUtils.exists(workspace)) {
            HdfsUtils.mkdirs(workspace);
        }
        HdfsUtils.upload(application.getModule(), workspace);
        //更新发布状态...
        application.setDeploy(0);
        updateDeploy(application);

        //更改状态为发布完成....
        application.setState(FlinkAppState.DEPLOYED.getValue());
        updateState(application);
    }

    @Override
    public void updateDeploy(Application application) {
        this.baseMapper.updateDeploy(application);
    }

    @Override
    public void closeDeploy(Application paramOfApp) {
        paramOfApp.setDeploy(0);
        this.baseMapper.updateDeploy(paramOfApp);
    }

    @Override
    public String readConf(Application paramOfApp) throws IOException {
        File file = new File(paramOfApp.getConfig());
        String conf = FileUtils.readFileToString(file, "utf-8");
        return Base64.getEncoder().encodeToString(conf.getBytes());
    }

    @Override
    public Application getApp(Application paramOfApp) {
        Application application = this.baseMapper.getApp(paramOfApp);
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
    public void updateState(Application application) {
        this.baseMapper.updateState(application);
    }

    @Override
    public void stop(Application paramOfApp) {
        Application application = getById(paramOfApp.getId());
        application.setState(FlinkAppState.CANCELLING.getValue());
        this.baseMapper.updateById(application);
        CommonUtil.localCache.put(paramOfApp.getId(), Long.valueOf(System.currentTimeMillis()));
        FlinkSubmit.stop(application.getAppId(), application.getJobId(), paramOfApp.getSavePoint(), paramOfApp.getDrain());
    }

    @Override
    public void updateMonitor(Application paramOfApp) {
        this.baseMapper.updateMonitor(paramOfApp);
    }

    @Override
    public boolean start(Application paramOfApp) throws Exception {
        final Application application = getById(paramOfApp.getId());
        assert application != null;
        Project project = projectService.getById(application.getProjectId());
        assert project != null;
        String workspaceWithSchemaAndNameService = "hdfs://".concat(properties.getNameService()).concat(ConfigConst.APP_WORKSPACE());

        ApplicationConfig applicationConfig = configService.getActived(application.getId());
        String confContent = applicationConfig.getContent();
        String format = applicationConfig.getFormat() == 1 ? "yaml" : "prop";
        String appConf = String.format("%s://%s", format, confContent);
        String classPath = String.format("%s/%s/%s/lib", workspaceWithSchemaAndNameService, paramOfApp.getId(), application.getModule());
        String flinkUserJar = String.format("%s/%s.jar", classPath, application.getModule());

        String[] overrideOption = CommonUtil.notEmpty(application.getShortOptions())
                ? application.getShortOptions().split("\\s+")
                : new String[0];

        String[] dynamicOption = CommonUtil.notEmpty(application.getDynamicOptions())
                ? application.getDynamicOptions().split("\\s+")
                : new String[0];

        SubmitInfo submitInfo = new SubmitInfo(
                YarnDeploymentTarget.valueOf(application.getDeployMode().toUpperCase()),
                properties.getNameService(),
                flinkUserJar,
                application.getJobName(),
                appConf,
                paramOfApp.getSavePoint(),
                overrideOption,
                dynamicOption,
                application.getArgs()
        );

        ApplicationId appId = FlinkSubmit.submit(submitInfo);
        application.setAppId(appId.toString());
        /**
         * 一定要在flink job提交完毕才置状态...
         */
        application.setState(FlinkAppState.STARTING.getValue());
        application.setEndTime(null);
        this.baseMapper.updateById(application);
        return true;
    }

    /**
     * 2秒钟从yarn里获取一次当前任务的appId,总共获取10次,如10次都未获取到则获取失败.
     */
    @SneakyThrows
    private void getAppId(Application application) {
        ThreadFactory namedThreadFactory = ThreadUtils.threadFactory("Flink-StartUp");
        ExecutorService executorService = Executors.newSingleThreadExecutor(namedThreadFactory);
        executorService.submit(() -> {
            int index = 0;
            Long lastTime = 0L;
            while (index <= 10) {
                Long now = System.currentTimeMillis();
                if (lastTime == 0 || (now - lastTime) >= 2000) {
                    lastTime = now;
                    index++;
                    List<ApplicationId> idList = YarnUtils.getAppId(application.getJobName());
                    if (!idList.isEmpty()) {
                        if (idList.size() == 1) {
                            ApplicationId applicationId = idList.get(0);
                            application.setAppId(applicationId.toString());
                        } else {
                            //表示有多个重复的任务.
                            application.setState(5);
                        }
                        updateById(application);
                        break;
                    }
                }
            }
        });
        ThreadUtils.shutdownExecutorService(executorService);
    }


}
