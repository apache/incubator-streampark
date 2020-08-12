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
import com.streamxhub.flink.monitor.core.entity.Project;
import com.streamxhub.flink.monitor.core.metrics.flink.JobsOverview;
import com.streamxhub.flink.monitor.core.service.ApplicationService;
import com.streamxhub.flink.monitor.core.service.ProjectService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.flink.monitor.core.enums.AppState;
import com.streamxhub.flink.monitor.system.authentication.ServerUtil;
import com.streamxhub.flink.submit.FlinkSubmit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
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
    private ServerUtil serverUtil;

    @Autowired
    private StreamXProperties properties;

    private final Map<Long, AppState> jobStateMap = new ConcurrentHashMap<>();

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

    @Override
    public boolean checkExists(Application app) {
        return YarnUtils.isContains(app.getAppName());
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
        String yarnName = this.getYarnName(app);
        app.setYarnName(yarnName);
        app.setUserId(serverUtil.getUser().getUserId());
        app.setState(AppState.CREATED.getValue());
        app.setCreateTime(new Date());
        app.setModule(app.getModule().replace(app.getAppBase().getAbsolutePath() + "/", ""));
        app.setConfig(app.getConfig().replace(app.getAppBase().getAbsolutePath() + "/".concat(app.getModule()).concat("/"), ""));
        boolean saved = save(app);
        if (saved) {
            try {
                deploy(app);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    public void deploy(Application app) throws IOException {
        if (!app.getModule().startsWith(app.getAppBase().getAbsolutePath())) {
            app.setModule(app.getAppBase().getAbsolutePath().concat("/").concat(app.getModule()));
        }
        String workspaceWithModule = app.getWorkspace(true);
        if (HdfsUtils.exists(workspaceWithModule)) {
            String backUp = app.backupPath();
            HdfsUtils.mkdirs(backUp);
            HdfsUtils.movie(workspaceWithModule, backUp);
        }

        String workspace = app.getWorkspace(false);
        if (!HdfsUtils.exists(workspace)) {
            HdfsUtils.mkdirs(workspace);
        }
        HdfsUtils.upload(app.getModule(), workspace);
        //更新发布状态...
        app.setDeploy(0);
        updateDeploy(app);
    }

    @Override
    public void updateDeploy(Application application) {
        this.baseMapper.updateDeploy(application);
    }

    @Override
    public boolean startUp(String id) {
        final Application application = getById(id);
        assert application != null;
        application.setState(AppState.DEPLOYING.getValue());
        this.baseMapper.updateById(application);
        Project project = projectService.getById(application.getProjectId());
        assert project != null;
        String workspaceWithSchemaAndNameService = "hdfs://".concat(properties.getNameService()).concat(ConfigConst.APP_WORKSPACE());
        String appConf = String.format("%s/%s/%s/%s", workspaceWithSchemaAndNameService, id, application.getModule(), application.getConfig());
        String flinkUserJar = String.format("%s/%s/%s/lib/%s.jar", workspaceWithSchemaAndNameService, id, application.getModule(), application.getModule());
        String[] overrideOption = application.getShortOptions().split("\\s+");
        ApplicationId appId = FlinkSubmit.submit(
                properties.getNameService(),
                flinkUserJar,
                application.getYarnName(),
                appConf,
                overrideOption,
                application.getArgs()
        );
        application.setAppId(appId.toString());
        Executors.newSingleThreadExecutor().submit(() -> getJobState(application));
        return true;
    }

    @SneakyThrows
    private void getJobState(Application application) {
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(
                1,
                new BasicThreadFactory.Builder().namingPattern("Flink-GetJobState-%d").daemon(true).build()
        );
        executorService.scheduleAtFixedRate(() -> {
            try {
                JobsOverview jobsOverview = application.jobsOverview();
                /**
                 * 注意:yarnName是唯一的,不能重复...
                 */
                Optional<JobsOverview.Job> optional = jobsOverview.getJobs().stream().filter((x) -> x.getName().equals(application.getYarnName())).findFirst();
                assert optional.isPresent();
                JobsOverview.Job job = optional.get();

                if (application.getJobId() == null) {
                    application.setJobId(job.getId());
                    this.baseMapper.updateById(application);
                }
                AppState state = AppState.valueOf(job.getState());
                AppState preState = jobStateMap.get(application.getId());
                if (!state.equals(preState)) {
                    application.setState(state.getValue());
                    this.baseMapper.updateById(application);
                }
            } catch (Exception e) {

            }
        }, 0, 2, TimeUnit.SECONDS);
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
                    List<ApplicationId> idList = YarnUtils.getAppId(application.getYarnName());
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
