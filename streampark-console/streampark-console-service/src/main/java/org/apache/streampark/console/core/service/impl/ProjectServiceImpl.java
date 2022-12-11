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

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.domain.FlinkMemorySize;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.CompletableFutureUtils;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.console.base.domain.ResponseCode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.CommonUtils;
import org.apache.streampark.console.base.util.FileUtils;
import org.apache.streampark.console.base.util.GZipUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.BuildState;
import org.apache.streampark.console.core.enums.GitProtocol;
import org.apache.streampark.console.core.enums.LaunchState;
import org.apache.streampark.console.core.mapper.ProjectMapper;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.ProjectService;
import org.apache.streampark.console.core.task.FlinkTrackingTask;
import org.apache.streampark.console.core.task.ProjectBuildTask;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project>
    implements ProjectService {

    @Autowired
    private ApplicationService applicationService;

    private final ExecutorService executorService = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 5,
        Runtime.getRuntime().availableProcessors() * 10,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        ThreadUtils.threadFactory("streampark-build-executor"),
        new ThreadPoolExecutor.AbortPolicy()
    );

    @Override
    public RestResponse create(Project project) {
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<Project>()
            .eq(Project::getName, project.getName());
        long count = count(queryWrapper);
        RestResponse response = RestResponse.success();
        if (count == 0) {
            project.setCreateTime(new Date());
            boolean status = save(project);
            if (status) {
                return response.message("Add project successfully").data(true);
            } else {
                return response.message("Add project failed").data(false);
            }
        } else {
            throw new ApiAlertException("project name already exists,add project failed");
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean update(Project projectParam) {
        try {
            Project project = getById(projectParam.getId());
            AssertUtils.state(project != null);
            AssertUtils.checkArgument(project.getTeamId().equals(projectParam.getTeamId()),
                "TeamId cannot be changed.");
            project.setName(projectParam.getName());
            project.setUrl(projectParam.getUrl());
            project.setBranches(projectParam.getBranches());
            project.setGitProtocol(projectParam.getGitProtocol());
            project.setRsaPath(projectParam.getRsaPath());
            project.setUserName(projectParam.getUserName());
            project.setPassword(projectParam.getPassword());
            project.setPom(projectParam.getPom());
            project.setDescription(projectParam.getDescription());
            project.setBuildArgs(projectParam.getBuildArgs());
            if (GitProtocol.SSH.equals(GitProtocol.of(project.getGitProtocol()))) {
                project.setUserName(null);
            } else {
                project.setRsaPath(null);
            }
            if (projectParam.getBuildState() != null) {
                project.setBuildState(projectParam.getBuildState());
                if (BuildState.of(projectParam.getBuildState()).equals(BuildState.NEED_REBUILD)) {
                    List<Application> applications = getApplications(project);
                    // Update deployment status
                    FlinkTrackingTask.refreshTracking(() -> applications.forEach((app) -> {
                        log.info("update deploy by project: {}, appName:{}", project.getName(), app.getJobName());
                        app.setLaunch(LaunchState.NEED_CHECK.get());
                        applicationService.updateLaunch(app);
                    }));
                }
            }
            baseMapper.updateById(project);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean delete(Long id) {
        Project project = getById(id);
        AssertUtils.state(project != null);
        LambdaQueryWrapper<Application> queryWrapper = new LambdaQueryWrapper<Application>()
            .eq(Application::getProjectId, id);
        long count = applicationService.count(queryWrapper);
        if (count > 0) {
            return false;
        }
        try {
            project.delete();
            removeById(id);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public IPage<Project> page(Project project, RestRequest request) {
        Page<Project> page = new MybatisPager<Project>().getDefaultPage(request);
        return this.baseMapper.page(page, project);
    }

    @Override
    public Boolean existsByTeamId(Long teamId) {
        return this.baseMapper.existsByTeamId(teamId);
    }

    @Override
    public void build(Long id) throws Exception {
        Project project = getById(id);
        this.baseMapper.updateBuildState(project.getId(), BuildState.BUILDING.get());
        ProjectBuildTask projectBuildTask = new ProjectBuildTask(getBuildLogPath(id), project, baseMapper, applicationService);
        CompletableFuture<Void> buildTask = CompletableFuture.runAsync(projectBuildTask, executorService);
        // TODO May need to define parameters to set the build timeout in the future.
        CompletableFutureUtils.runTimeout(buildTask, 20, TimeUnit.MINUTES);
    }

    @Override
    public List<String> modules(Long id) {
        Project project = getById(id);
        AssertUtils.state(project != null);
        BuildState buildState = BuildState.of(project.getBuildState());
        if (BuildState.SUCCESSFUL.equals(buildState)) {
            File appHome = project.getDistHome();
            if (appHome.exists()) {
                List<String> list = new ArrayList<>();
                File[] files = appHome.listFiles();
                if (CommonUtils.notEmpty(files)) {
                    for (File file : files) {
                        list.add(file.getName());
                    }
                }
                return list;
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> jars(Project project) {
        List<String> list = new ArrayList<>(0);
        if (project.getModule() == null) {
            throw new ApiAlertException("project module can not be null, please check");
        }
        File apps = new File(project.getDistHome(), project.getModule());
        for (File file : Objects.requireNonNull(apps.listFiles())) {
            if (file.getName().endsWith(".jar")) {
                list.add(file.getName());
            }
        }
        return list;
    }

    @Override
    public String getAppConfPath(Long id, String module) {
        Project project = getById(id);
        File appHome = project.getDistHome();
        Optional<File> fileOptional = Arrays.stream(Objects.requireNonNull(appHome.listFiles()))
            .filter((x) -> x.getName().equals(module))
            .findFirst();
        return fileOptional.map(File::getAbsolutePath).orElse(null);
    }

    @Override
    public List<Application> getApplications(Project project) {
        return this.applicationService.getByProjectId(project.getId());
    }

    @Override
    public boolean checkExists(Project project) {
        if (project.getId() != null) {
            Project proj = getById(project.getId());
            if (proj.getName().equals(project.getName())) {
                return false;
            }
        }
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<Project>()
            .eq(Project::getName, project.getName());
        return this.baseMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public List<Map<String, Object>> listConf(Project project) {
        try {
            File file = new File(project.getDistHome(), project.getModule());
            File unzipFile = new File(file.getAbsolutePath().replaceAll(".tar.gz", ""));
            if (!unzipFile.exists()) {
                GZipUtils.decompress(file.getAbsolutePath(), file.getParentFile().getAbsolutePath());
            }
            List<Map<String, Object>> list = new ArrayList<>();
            File[] files = unzipFile.listFiles(x -> "conf".equals(x.getName()));
            AssertUtils.state(files != null);
            for (File item : files) {
                eachFile(item, list, true);
            }
            return list;
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return null;
    }

    private void eachFile(File file, List<Map<String, Object>> list, Boolean isRoot) {
        if (file != null && file.exists() && file.listFiles() != null) {
            if (isRoot) {
                Map<String, Object> map = new HashMap<>(0);
                map.put("key", file.getName());
                map.put("title", file.getName());
                map.put("value", file.getAbsolutePath());
                List<Map<String, Object>> children = new ArrayList<>();
                eachFile(file, children, false);
                if (!children.isEmpty()) {
                    map.put("children", children);
                }
                list.add(map);
            } else {
                for (File item : Objects.requireNonNull(file.listFiles())) {
                    String title = item.getName();
                    String value = item.getAbsolutePath();
                    Map<String, Object> map = new HashMap<>(0);
                    map.put("key", title);
                    map.put("title", title);
                    map.put("value", value);
                    List<Map<String, Object>> children = new ArrayList<>();
                    eachFile(item, children, false);
                    if (!children.isEmpty()) {
                        map.put("children", children);
                    }
                    list.add(map);
                }
            }
        }
    }

    @Override
    public RestResponse getBuildLog(Long id, Long startOffset) {
        File logFile = Paths.get(getBuildLogPath(id)).toFile();
        if (!logFile.exists()) {
            String errorMsg = String.format("Build log file(fileName=%s) not found, please build first.", logFile);
            log.warn(errorMsg);
            return RestResponse.success().data(errorMsg);
        }
        boolean isBuilding = this.getById(id).getBuildState() == 0;
        byte[] fileContent;
        long endOffset = 0L;
        boolean readFinished = true;
        // Read log from earliest when project is building
        if (startOffset == null && isBuilding) {
            startOffset = 0L;
        }
        try {
            long maxSize = FlinkMemorySize.parse(InternalConfigHolder.get(CommonConfig.READ_LOG_MAX_SIZE())).getBytes();
            if (startOffset == null) {
                fileContent = FileUtils.readEndOfFile(logFile, maxSize);
            } else {
                fileContent = FileUtils.readFileFromOffset(logFile, startOffset, maxSize);
                endOffset = startOffset + fileContent.length;
                readFinished = logFile.length() == endOffset && !isBuilding;
            }
            return RestResponse.success()
                .data(new String(fileContent, StandardCharsets.UTF_8))
                .put("offset", endOffset)
                .put("readFinished", readFinished);
        } catch (IOException e) {
            String error = String.format("Read build log file(fileName=%s) caused an exception: ", logFile);
            log.error(error, e);
            return RestResponse.fail(error + e.getMessage(), ResponseCode.CODE_FAIL);
        }
    }

    private String getBuildLogPath(Long projectId) {
        return String.format("%s/%s/build.log", Workspace.PROJECT_BUILD_LOG_PATH(), projectId);
    }

}
