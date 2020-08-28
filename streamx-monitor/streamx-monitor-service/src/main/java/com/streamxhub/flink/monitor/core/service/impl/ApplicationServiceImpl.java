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
package com.streamxhub.flink.monitor.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.streamxhub.common.conf.ConfigConst;
import com.streamxhub.common.conf.ParameterCli;
import com.streamxhub.common.util.HdfsUtils;
import com.streamxhub.common.util.HttpClientUtils;
import com.streamxhub.common.util.ThreadUtils;
import com.streamxhub.common.util.YarnUtils;
import com.streamxhub.flink.monitor.base.domain.Constant;
import com.streamxhub.flink.monitor.base.domain.RestRequest;
import com.streamxhub.flink.monitor.base.properties.StreamXProperties;
import com.streamxhub.flink.monitor.base.utils.SortUtil;
import com.streamxhub.flink.monitor.core.dao.ApplicationMapper;
import com.streamxhub.flink.monitor.core.entity.Application;
import com.streamxhub.flink.monitor.core.entity.ApplicationBackUp;
import com.streamxhub.flink.monitor.core.entity.Project;
import com.streamxhub.flink.monitor.core.enums.AppExistsState;
import com.streamxhub.flink.monitor.core.service.ApplicationBackUpService;
import com.streamxhub.flink.monitor.core.service.ApplicationService;
import com.streamxhub.flink.monitor.core.service.ProjectService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.flink.monitor.core.enums.FlinkAppState;
import com.streamxhub.flink.monitor.system.authentication.ServerUtil;
import com.streamxhub.flink.submit.FlinkSubmit;
import com.streamxhub.flink.submit.SubmitInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.yarn.configuration.YarnDeploymentTarget;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service("applicationService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements ApplicationService {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private ServerUtil serverUtil;

    @Autowired
    private StreamXProperties properties;


    @Override
    public IPage<Application> list(Application app, RestRequest request) {
        Page<Application> page = new Page<>();
        SortUtil.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
        return this.baseMapper.findApplication(page, app);
    }

    @Override
    public String getYarnName(Application app) {
        String[] args = new String[2];
        args[0] = "--name";
        args[1] = app.getConfig();
        return ParameterCli.read(args);
    }

    /**
     * 检查当前的appName在表和yarn中是否已经存在
     *
     * @param app
     * @return
     */
    @Override
    public AppExistsState checkExists(Application app) {
        QueryWrapper<Application> queryWrapper = new QueryWrapper();
        queryWrapper.eq("app_name", app.getAppName());
        int count = this.baseMapper.selectCount(queryWrapper);
        boolean exists = YarnUtils.isContains(app.getAppName());
        if (count == 0 && !exists) {
            return AppExistsState.NO;
        }
        return exists ? AppExistsState.IN_YARN : AppExistsState.IN_DB;
    }

    @Override
    public boolean create(Application app) {
        if (app.getConfig() != null && app.getConfig().trim().length() > 0) {
            try {
                String config = URLDecoder.decode(app.getConfig(), "UTF-8");
                app.setConfig(config);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        //配置文件中配置的yarnName..
        app.setUserId(serverUtil.getUser().getUserId());
        app.setState(FlinkAppState.CREATED.getValue());
        app.setCreateTime(new Date());
        app.setModule(app.getModule().replace(app.getAppBase().getAbsolutePath() + "/", ""));
        app.setConfig(app.getConfig().replace(app.getAppBase().getAbsolutePath() + "/".concat(app.getModule()).concat("/"), ""));
        boolean saved = save(app);
        if (saved) {
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    deploy(app, false);
                    return true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return false;
    }

    @Override
    public void deploy(Application app, boolean backUp) throws IOException {
        //先停止原有任务..
        Application application = getById(app.getId());
        application.setBackUpDescription(app.getBackUpDescription());
        if (application.getState() == FlinkAppState.RUNNING.getValue()) {
            cancel(application);
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
    public void updateState(Application application) {
        this.baseMapper.updateState(application);
    }

    @Override
    public void cancel(Application app) {
        Application application = getById(app.getId());
        application.setState(FlinkAppState.CANCELLING.getValue());
        this.baseMapper.updateById(application);
        String yarn = properties.getYarn();
        String url = String.format("%s/proxy/%s/jobs/%s/yarn-cancel", yarn, application.getAppId(), application.getJobId());
        HttpClientUtils.httpGetRequest(url);
    }

    @Override
    public boolean startUp(String id) throws Exception {
        final Application application = getById(id);
        assert application != null;
        Project project = projectService.getById(application.getProjectId());
        assert project != null;
        String workspaceWithSchemaAndNameService = "hdfs://".concat(properties.getNameService()).concat(ConfigConst.APP_WORKSPACE());
        String appConf = String.format("%s/%s/%s/%s", workspaceWithSchemaAndNameService, id, application.getModule(), application.getConfig());
        String classPath = String.format("%s/%s/%s/lib", workspaceWithSchemaAndNameService, id, application.getModule());
        String flinkUserJar = String.format("%s/%s.jar", classPath, application.getModule());
        String[] overrideOption = application.getShortOptions().split("\\s+");
        String[] dynamicOption = application.getDynamicOptions().split("\\s+");

        SubmitInfo submitInfo = new SubmitInfo(
                YarnDeploymentTarget.valueOf(application.getDeployMode().toUpperCase()),
                properties.getNameService(),
                classPath,
                flinkUserJar,
                application.getAppName(),
                appConf,
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
                    List<ApplicationId> idList = YarnUtils.getAppId(application.getAppName());
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
