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

import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.CommandUtils;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.CommonUtils;
import org.apache.streampark.console.base.util.GZipUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.BuildState;
import org.apache.streampark.console.core.enums.LaunchState;
import org.apache.streampark.console.core.mapper.ProjectMapper;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.ProjectService;
import org.apache.streampark.console.core.task.FlinkTrackingTask;
import org.apache.streampark.console.core.websocket.WebSocketEndpoint;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project>
    implements ProjectService {

    private final Map<Long, Byte> tailOutMap = new ConcurrentHashMap<>();

    private final Map<Long, StringBuilder> tailBuffer = new ConcurrentHashMap<>();

    private final Map<Long, Byte> tailBeginning = new ConcurrentHashMap<>();

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
        LambdaQueryWrapper<Project> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Project::getName, project.getName());
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
            return response.message("A project with this name already exists, adding a task failed").data(false);
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean update(Project projectParam) {
        try {
            Project project = getById(projectParam.getId());
            AssertUtils.state(project != null);
            project.setName(projectParam.getName());
            project.setUrl(projectParam.getUrl());
            project.setBranches(projectParam.getBranches());
            project.setUserName(projectParam.getUserName());
            project.setPassword(projectParam.getPassword());
            project.setPom(projectParam.getPom());
            project.setDescription(projectParam.getDescription());
            project.setBuildArgs(projectParam.getBuildArgs());
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
        LambdaQueryWrapper<Application> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Application::getProjectId, id);
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
    public void build(Long id, String socketId) throws Exception {
        Project project = getById(id);
        this.baseMapper.startBuild(project);
        StringBuilder builder = new StringBuilder();
        tailBuffer.put(id, builder.append(project.getLog4BuildStart()));
        boolean cloneSuccess = cloneSourceCode(project, socketId);
        if (!cloneSuccess) {
            log.error("[StreamPark] clone or pull error.");
            tailBuffer.remove(project.getId());
            this.baseMapper.failureBuild(project);
            return;
        }
        executorService.execute(() -> {
            boolean build = projectBuild(project, socketId);
            if (!build) {
                this.baseMapper.failureBuild(project);
                log.error("build error, project name: {} ", project.getName());
                return;
            }
            this.baseMapper.successBuild(project);
            try {
                this.deploy(project);
                List<Application> applications = getApplications(project);
                // Update the deploy state
                FlinkTrackingTask.refreshTracking(() -> applications.forEach((app) -> {
                    log.info("update deploy by project: {}, appName:{}", project.getName(), app.getJobName());
                    app.setLaunch(LaunchState.NEED_LAUNCH.get());
                    app.setBuild(true);
                    this.applicationService.updateLaunch(app);
                }));
            } catch (Exception e) {
                this.baseMapper.failureBuild(project);
                log.error("deploy error, project name: {}, detail: {}", project.getName(), e.getMessage());
            }
        });
    }

    private void deploy(Project project) throws Exception {
        File path = project.getAppSource();
        List<File> apps = new ArrayList<>();
        // find the compiled tar.gz (Stream Park project) file or jar (normal or official standard flink project) under the project path
        findTarOrJar(apps, path);
        if (apps.isEmpty()) {
            throw new RuntimeException("[StreamPark] can't find tar.gz or jar in " + path.getAbsolutePath());
        }
        for (File app : apps) {
            String appPath = app.getAbsolutePath();
            // 1). tar.gz file
            if (appPath.endsWith("tar.gz")) {
                File deployPath = project.getDistHome();
                if (!deployPath.exists()) {
                    deployPath.mkdirs();
                }
                // xzvf jar
                if (app.exists()) {
                    String cmd = String.format(
                        "tar -xzvf %s -C %s",
                        app.getAbsolutePath(),
                        deployPath.getAbsolutePath()
                    );
                    CommandUtils.execute(cmd);
                }
            } else {
                // 2) .jar file(normal or official standard flink project)
                Utils.checkJarFile(app.toURI().toURL());
                String moduleName = app.getName().replace(".jar", "");
                File distHome = project.getDistHome();
                File targetDir = new File(distHome, moduleName);
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                File targetJar = new File(targetDir, app.getName());
                app.renameTo(targetJar);
            }
        }
    }

    private void findTarOrJar(List<File> list, File path) {
        for (File file : Objects.requireNonNull(path.listFiles())) {
            // navigate to the target directory:
            if (file.isDirectory() && "target".equals(file.getName())) {
                // find the tar.gz file or the jar file in the target path.
                // note: only one of the two can be selected, which cannot be satisfied at the same time.
                File tar = null;
                File jar = null;
                for (File targetFile : Objects.requireNonNull(file.listFiles())) {
                    // 1) exit once the tar.gz file is found.
                    if (targetFile.getName().endsWith("tar.gz")) {
                        tar = targetFile;
                        break;
                    }
                    // 2) try look for jar files, there may be multiple jars found.
                    if (!targetFile.getName().startsWith("original-")
                        && !targetFile.getName().endsWith("-sources.jar")
                        && targetFile.getName().endsWith(".jar")) {
                        if (jar == null) {
                            jar = targetFile;
                        } else {
                            // there may be multiple jars found, in this case, select the jar with the largest and return
                            if (targetFile.length() > jar.length()) {
                                jar = targetFile;
                            }
                        }
                    }
                }
                File target = tar == null ? jar : tar;
                if (target == null) {
                    log.warn("[StreamPark] can't find tar.gz or jar in {}", file.getAbsolutePath());
                } else {
                    list.add(target);
                }
            }

            if (file.isDirectory()) {
                findTarOrJar(list, file);
            }
        }
    }

    @Override
    public List<String> modules(Long id) {
        Project project = getById(id);
        File appHome = project.getDistHome();
        List<String> list = new ArrayList<>();
        Arrays.stream(Objects.requireNonNull(appHome.listFiles())).forEach((x) -> list.add(x.getName()));
        return list;
    }

    @Override
    public List<String> jars(Project project) {
        List<String> list = new ArrayList<>(0);
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
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
            .eq(Project::getName, project.getName());
        return this.baseMapper.selectCount(wrapper) > 0;
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

    private boolean cloneSourceCode(Project project, String socketId) {
        try {
            project.cleanCloned();
            log.info("clone {}, {} starting...", project.getName(), project.getUrl());

            WebSocketEndpoint.writeMessage(socketId, String.format("clone %s starting..., url: %s", project.getName(), project.getUrl()));

            tailBuffer.get(project.getId()).append(project.getLog4CloneStart());
            CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(project.getUrl())
                .setDirectory(project.getAppSource())
                .setBranch(project.getBranches());

            if (CommonUtils.notEmpty(project.getUserName(), project.getPassword())) {
                cloneCommand.setCredentialsProvider(project.getCredentialsProvider());
            }

            Future<Git> future = executorService.submit(cloneCommand);
            Git git = future.get(60, TimeUnit.SECONDS);

            StoredConfig config = git.getRepository().getConfig();
            config.setBoolean("http", project.getUrl(), "sslVerify", false);
            config.setBoolean("https", project.getUrl(), "sslVerify", false);
            config.save();

            File workTree = git.getRepository().getWorkTree();
            gitWorkTree(project.getId(), workTree, "");
            String successMsg = String.format(
                "[StreamPark] project [%s] git clone successful!\n",
                project.getName()
            );
            tailBuffer.get(project.getId()).append(successMsg);
            WebSocketEndpoint.writeMessage(socketId, successMsg);
            git.close();
            return true;
        } catch (Exception e) {
            String errorLog = String.format(
                "[StreamPark] project [%s] branch [%s] git clone failure, err: %s",
                project.getName(),
                project.getBranches(),
                e
            );
            tailBuffer.get(project.getId()).append(errorLog);
            WebSocketEndpoint.writeMessage(socketId, errorLog);
            log.error(String.format("project %s clone error ", project.getName()), e);
            return false;
        }
    }

    private void gitWorkTree(Long id, File workTree, String space) {
        File[] files = workTree.listFiles();
        for (File file : Objects.requireNonNull(files)) {
            if (!file.getName().startsWith(".git")) {
                if (file.isFile()) {
                    tailBuffer.get(id).append(space).append("/").append(file.getName()).append("\n");
                } else if (file.isDirectory()) {
                    tailBuffer.get(id).append(space).append("/").append(file.getName()).append("\n");
                    gitWorkTree(id, file, space.concat("/").concat(file.getName()));
                }
            }
        }
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

    private boolean projectBuild(Project project, String socketId) {
        StringBuilder builder = tailBuffer.get(project.getId());
        int code = CommandUtils.execute(project.getMavenWorkHome(),
            Collections.singletonList(project.getMavenArgs()),
            (line) -> {
                builder.append(line).append("\n");
                if (tailOutMap.containsKey(project.getId())) {
                    if (tailBeginning.containsKey(project.getId())) {
                        tailBeginning.remove(project.getId());
                        Arrays.stream(builder.toString().split("\n"))
                            .forEach(out -> WebSocketEndpoint.writeMessage(socketId, out));
                    }
                    WebSocketEndpoint.writeMessage(socketId, line);
                }
            });
        closeBuildLog(project.getId());
        log.info(builder.toString());
        tailBuffer.remove(project.getId());
        return code == 0;
    }

    @Override
    public void tailBuildLog(Long id) {
        this.tailOutMap.put(id, Byte.valueOf("0"));
        this.tailBeginning.put(id, Byte.valueOf("0"));
    }

    @Override
    public void closeBuildLog(Long id) {
        tailOutMap.remove(id);
        tailBeginning.remove(id);
    }

}
