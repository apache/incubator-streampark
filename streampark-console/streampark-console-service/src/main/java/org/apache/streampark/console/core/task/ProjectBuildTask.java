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

package org.apache.streampark.console.core.task;

import org.apache.streampark.common.util.CommandUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.util.GitUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.BuildState;
import org.apache.streampark.console.core.enums.LaunchState;
import org.apache.streampark.console.core.mapper.ProjectMapper;
import org.apache.streampark.console.core.service.ApplicationService;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ProjectBuildTask extends AbstractLogFileTask {

    final Project project;

    final ProjectMapper baseMapper;

    final ApplicationService applicationService;

    public ProjectBuildTask(
        String logPath,
        Project project,
        ProjectMapper baseMapper,
        ApplicationService applicationService) {
        super(logPath, true);
        this.project = project;
        this.baseMapper = baseMapper;
        this.applicationService = applicationService;
    }

    @Override
    protected void doRun() throws Throwable {
        log.info("Project {} start build", project.getName());
        fileLogger.info(project.getLog4BuildStart());
        boolean cloneSuccess = cloneSourceCode(project);
        if (!cloneSuccess) {
            fileLogger.error("[StreamPark] clone or pull error.");
            this.baseMapper.updateBuildState(project.getId(), BuildState.FAILED.get());
            return;
        }
        boolean build = projectBuild(project);
        if (!build) {
            this.baseMapper.updateBuildState(project.getId(), BuildState.FAILED.get());
            fileLogger.error("build error, project name: {} ", project.getName());
            return;
        }
        this.baseMapper.updateBuildState(project.getId(), BuildState.SUCCESSFUL.get());
        this.baseMapper.updateBuildTime(project.getId());
        this.deploy(project);
        List<Application> applications = this.applicationService.getByProjectId(project.getId());
        // Update the deployment state
        FlinkTrackingTask.refreshTracking(() -> applications.forEach((app) -> {
            fileLogger.info("update deploy by project: {}, appName:{}", project.getName(), app.getJobName());
            app.setLaunch(LaunchState.NEED_LAUNCH.get());
            app.setBuild(true);
            this.applicationService.updateLaunch(app);
        }));
    }

    @Override
    protected void processException(Throwable t) {
        this.baseMapper.updateBuildState(project.getId(), BuildState.FAILED.get());
        fileLogger.error("Build error, project name: {}", project.getName(), t);
    }

    @Override
    protected void doFinally() {
    }

    private boolean cloneSourceCode(Project project) {
        try {
            project.cleanCloned();
            fileLogger.info("clone {}, {} starting...", project.getName(), project.getUrl());
            fileLogger.info(project.getLog4CloneStart());
            Git git = GitUtils.clone(project);
            StoredConfig config = git.getRepository().getConfig();
            config.setBoolean("http", project.getUrl(), "sslVerify", false);
            config.setBoolean("https", project.getUrl(), "sslVerify", false);
            config.save();
            File workTree = git.getRepository().getWorkTree();
            printWorkTree(workTree, "");
            String successMsg = String.format(
                "[StreamPark] project [%s] git clone successful!\n",
                project.getName()
            );
            fileLogger.info(successMsg);
            git.close();
            return true;
        } catch (Exception e) {
            fileLogger.error(String.format(
                "[StreamPark] project [%s] branch [%s] git clone failure, err: %s",
                project.getName(),
                project.getBranches(),
                e
            ));
            fileLogger.error(String.format("project %s clone error ", project.getName()), e);
            return false;
        }
    }

    private void printWorkTree(File workTree, String space) {
        File[] files = workTree.listFiles();
        for (File file : Objects.requireNonNull(files)) {
            if (!file.getName().startsWith(".git")) {
                continue;
            }
            if (file.isFile()) {
                fileLogger.info("{} / {}", space, file.getName());
            } else if (file.isDirectory()) {
                fileLogger.info("{} / {}", space, file.getName());
                printWorkTree(file, space.concat("/").concat(file.getName()));
            }
        }
    }

    private boolean projectBuild(Project project) {
        int code = CommandUtils.execute(project.getMavenWorkHome(),
            Collections.singletonList(project.getMavenArgs()),
            (line) -> fileLogger.info(line));
        return code == 0;
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
                    fileLogger.warn("[StreamPark] can't find tar.gz or jar in {}", file.getAbsolutePath());
                } else {
                    list.add(target);
                }
            }
            if (file.isDirectory()) {
                findTarOrJar(list, file);
            }
        }
    }
}
