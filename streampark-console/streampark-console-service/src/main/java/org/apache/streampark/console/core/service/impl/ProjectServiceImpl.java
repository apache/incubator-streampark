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
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.CompletableFutureUtils;
import org.apache.streampark.common.util.FileUtils;
import org.apache.streampark.console.base.domain.ResponseCode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.GZipUtils;
import org.apache.streampark.console.base.util.GitUtils;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.BuildStateEnum;
import org.apache.streampark.console.core.enums.GitAuthorizedErrorEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.mapper.ProjectMapper;
import org.apache.streampark.console.core.service.ProjectService;
import org.apache.streampark.console.core.service.application.FlinkApplicationManageService;
import org.apache.streampark.console.core.task.ProjectBuildTask;
import org.apache.streampark.console.core.watcher.FlinkAppHttpWatcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.MemorySize;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project>
    implements
        ProjectService {

    @Autowired
    private FlinkApplicationManageService applicationManageService;

    @Autowired
    private FlinkAppHttpWatcher flinkAppHttpWatcher;

    @Qualifier("streamparkBuildExecutor")
    @Autowired
    private Executor executorService;

    @Value("${streampark.project.max-build:6}")
    private Long maxProjectBuildNum;

    @Override
    public RestResponse create(Project project) {
        RestResponse response = RestResponse.success();
        project.setId(null);
        ApiAlertException.throwIfTrue(
            checkExists(project), "project name already exists, add project failed");
        Date date = new Date();
        project.setCreateTime(date);
        project.setModifyTime(date);
        boolean status = save(project);
        if (status) {
            return response.message("Add project successfully").data(true);
        } else {
            return response.message("Add project failed").data(false);
        }
    }

    @Override
    public boolean checkExists(Project project) {
        return this.lambdaQuery()
            .eq(Project::getName, project.getName())
            .eq(Project::getTeamId, project.getTeamId())
            .exists();
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean update(Project projectParam) {
        Project project = getById(projectParam.getId());
        AssertUtils.notNull(project);
        ApiAlertException.throwIfFalse(
            project.getTeamId().equals(projectParam.getTeamId()),
            "Team can't be changed, update project failed.");
        ApiAlertException.throwIfFalse(
            !project.getBuildState().equals(BuildStateEnum.BUILDING.get()),
            "The project is being built, update project failed.");
        project.setName(projectParam.getName());
        project.setUrl(projectParam.getUrl());
        project.setRefs(projectParam.getRefs());
        project.setPrvkeyPath(projectParam.getPrvkeyPath());
        project.setUserName(projectParam.getUserName());
        project.setPassword(projectParam.getPassword());
        project.setPom(projectParam.getPom());
        project.setDescription(projectParam.getDescription());
        project.setBuildArgs(projectParam.getBuildArgs());
        project.setModifyTime(new Date());
        if (GitUtils.isSshRepositoryUrl(project.getUrl())) {
            project.setUserName(null);
        } else {
            project.setPrvkeyPath(null);
        }
        if (projectParam.getBuildState() != null) {
            project.setBuildState(projectParam.getBuildState());
            if (BuildStateEnum.of(projectParam.getBuildState()).equals(BuildStateEnum.NEED_REBUILD)) {
                List<FlinkApplication> applications = listApps(project);
                // Update deployment status
                applications.forEach(
                    (app) -> {
                        log.info(
                            "update deploy by project: {}, appName:{}", project.getName(), app.getJobName());
                        app.setRelease(ReleaseStateEnum.NEED_CHECK.get());
                        applicationManageService.updateRelease(app);
                    });
            }
        }
        baseMapper.updateById(project);
        return true;
    }

    @Override
    public boolean removeById(Long id) {
        Project project = getById(id);
        AssertUtils.notNull(project);
        LambdaQueryWrapper<FlinkApplication> queryWrapper = new LambdaQueryWrapper<FlinkApplication>()
            .eq(FlinkApplication::getProjectId, id);
        long count = applicationManageService.count(queryWrapper);
        if (count > 0) {
            return false;
        }
        try {
            project.delete();
            super.removeById(id);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public IPage<Project> getPage(Project project, RestRequest request) {
        Page<Project> page = MybatisPager.getPage(request);
        this.lambdaQuery()
            .eq(Project::getTeamId, project.getTeamId())
            .like(StringUtils.isNotBlank(project.getName()), Project::getName, project.getName())
            .eq(project.getBuildState() != null, Project::getBuildState, project.getBuildState())
            .page(page);
        return page;
    }

    @Override
    public Boolean existsByTeamId(Long teamId) {
        return this.lambdaQuery()
            .eq(Project::getTeamId, teamId)
            .exists();
    }

    @Override
    public List<Project> listByTeamId(Long teamId) {
        return this.lambdaQuery().eq(Project::getTeamId, teamId)
            .list();
    }

    @Override
    public void build(Long id) throws Exception {

        Long currentBuildCount = this.lambdaQuery()
            .eq(Project::getBuildState, BuildStateEnum.BUILDING.get())
            .count();

        ApiAlertException.throwIfTrue(
            maxProjectBuildNum > -1 && currentBuildCount > maxProjectBuildNum,
            String.format(
                "The number of running Build projects exceeds the maximum number: %d of max-build-num",
                maxProjectBuildNum));
        Project project = getById(id);

        this.updateBuildState(project.getId(), BuildStateEnum.BUILDING.get());

        String logPath = getBuildLogPath(id);
        ProjectBuildTask projectBuildTask = new ProjectBuildTask(
            logPath,
            project,
            buildStateEnum -> {
                this.updateBuildState(id, buildStateEnum.get());
                if (buildStateEnum == BuildStateEnum.SUCCESSFUL) {
                    this.updateBuildTime(id);
                }
                flinkAppHttpWatcher.init();
            },
            fileLogger -> {
                List<FlinkApplication> applications =
                    this.applicationManageService.listByProjectId(project.getId());
                applications.forEach(
                    (app) -> {
                        fileLogger.info(
                            "update deploy by project: {}, appName:{}",
                            project.getName(),
                            app.getJobName());
                        app.setRelease(ReleaseStateEnum.NEED_RELEASE.get());
                        app.setBuild(true);
                        this.applicationManageService.updateRelease(app);
                    });
                flinkAppHttpWatcher.init();
            });
        CompletableFuture<Void> buildTask = CompletableFuture.runAsync(projectBuildTask, executorService);
        // TODO May need to define parameters to set the build timeout in the future.
        CompletableFutureUtils.runTimeout(buildTask, 20, TimeUnit.MINUTES);
    }

    @Override
    public List<String> listModules(Long id) {
        Project project = getById(id);
        AssertUtils.notNull(project);

        if (BuildStateEnum.SUCCESSFUL != BuildStateEnum.of(project.getBuildState())
            || !project.getDistHome().exists()) {
            return Collections.emptyList();
        }

        File[] files = project.getDistHome().listFiles();
        return files == null
            ? Collections.emptyList()
            : Stream.of(files).map(File::getName).collect(Collectors.toList());
    }

    @Override
    public List<String> listJars(Project project) {
        ApiAlertException.throwIfNull(
            project.getModule(), "Project module can't be null, please check.");
        File projectModuleDir = new File(project.getDistHome(), project.getModule());
        return Arrays.stream(Objects.requireNonNull(projectModuleDir.listFiles()))
            .map(File::getName)
            .filter(name -> name.endsWith(Constants.JAR_SUFFIX))
            .collect(Collectors.toList());
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
    public List<FlinkApplication> listApps(Project project) {
        return this.applicationManageService.listByProjectId(project.getId());
    }

    @Override
    public boolean exists(Project project) {
        if (project.getId() != null) {
            Project proj = getById(project.getId());
            if (proj.getName().equals(project.getName())) {
                return false;
            }
        }
        return this.lambdaQuery().eq(Project::getName, project.getName())
            .eq(Project::getTeamId, project.getTeamId()).exists();
    }

    @Override
    public List<Map<String, Object>> listConf(Project project) {
        try {
            File file = new File(project.getDistHome(), project.getModule());
            File unzipFile = new File(file.getAbsolutePath().replaceAll(".tar.gz", ""));
            if (!unzipFile.exists()) {
                GZipUtils.deCompress(file.getAbsolutePath(), file.getParentFile().getAbsolutePath());
            }
            List<Map<String, Object>> confList = new ArrayList<>();
            File[] files = unzipFile.listFiles(x -> "conf".equals(x.getName()));
            AssertUtils.notNull(files);
            for (File item : files) {
                eachFile(item, confList, true);
            }
            return confList;
        } catch (Exception e) {
            log.error("List project conf failed", e);
        }
        return null;
    }

    private void eachFile(File file, List<Map<String, Object>> list, Boolean isRoot) {
        if (file != null && file.exists() && file.listFiles() != null) {
            if (isRoot) {
                Map<String, Object> fileMap = new HashMap<>(0);
                fileMap.put("key", file.getName());
                fileMap.put("title", file.getName());
                fileMap.put("value", file.getAbsolutePath());
                List<Map<String, Object>> children = new ArrayList<>();
                eachFile(file, children, false);
                if (!children.isEmpty()) {
                    fileMap.put("children", children);
                }
                list.add(fileMap);
            } else {
                for (File item : Objects.requireNonNull(file.listFiles())) {
                    String title = item.getName();
                    String value = item.getAbsolutePath();
                    Map<String, Object> fileMap = new HashMap<>(0);
                    fileMap.put("key", title);
                    fileMap.put("title", title);
                    fileMap.put("value", value);
                    List<Map<String, Object>> children = new ArrayList<>();
                    eachFile(item, children, false);
                    if (!children.isEmpty()) {
                        fileMap.put("children", children);
                    }
                    list.add(fileMap);
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
            long maxSize = MemorySize.parse(InternalConfigHolder.get(CommonConfig.READ_LOG_MAX_SIZE())).getBytes();
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
            return RestResponse.fail(ResponseCode.CODE_FAIL, error + e.getMessage());
        }
    }

    private String getBuildLogPath(Long projectId) {
        return String.format("%s/%s/build.log", Workspace.PROJECT_BUILD_LOG_PATH(), projectId);
    }

    @Override
    public List<String> getAllBranches(Project project) {
        try {
            GitUtils.GitGetRequest request = new GitUtils.GitGetRequest();
            request.setUrl(project.getUrl());
            request.setUsername(project.getUserName());
            request.setPassword(project.getPassword());
            request.setPrivateKey(project.getPrvkeyPath());
            return GitUtils.getBranches(request);
        } catch (Exception e) {
            throw new ApiDetailException(e);
        }
    }

    @Override
    public GitAuthorizedErrorEnum gitCheck(Project project) {
        try {
            GitUtils.GitGetRequest request = new GitUtils.GitGetRequest();
            request.setUrl(project.getUrl());
            request.setUsername(project.getUserName());
            request.setPassword(project.getPassword());
            request.setPrivateKey(project.getPrvkeyPath());
            GitUtils.getBranches(request);
            return GitAuthorizedErrorEnum.SUCCESS;
        } catch (Exception e) {
            String err = e.getMessage();
            if (err.contains("not authorized")) {
                return GitAuthorizedErrorEnum.ERROR;
            } else if (err.contains("Authentication is required")) {
                return GitAuthorizedErrorEnum.REQUIRED;
            }
            return GitAuthorizedErrorEnum.UNKNOW;
        }
    }

    @Override
    public List<String> getAllTags(Project project) {
        try {
            GitUtils.GitGetRequest request = new GitUtils.GitGetRequest();
            request.setUrl(project.getUrl());
            request.setUsername(project.getUserName());
            request.setPassword(project.getPassword());
            request.setPrivateKey(project.getPrvkeyPath());
            return GitUtils.getTags(request);
        } catch (Exception e) {
            throw new ApiDetailException(e);
        }
    }

    @Override
    public void updateBuildTime(Long id) {
        this.lambdaUpdate()
            .eq(Project::getId, id)
            .set(Project::getLastBuild, new Date())
            .update();
    }

    @Override
    public void updateBuildState(Long id, int state) {
        this.lambdaUpdate()
            .eq(Project::getId, id)
            .set(Project::getBuildState, state)
            .update();
    }

    private Project remakeProject(Project project) {
        if (Objects.nonNull(project.getId())) {
            return this.baseMapper.selectById(project.getId());
        }
        return project;
    }
}
