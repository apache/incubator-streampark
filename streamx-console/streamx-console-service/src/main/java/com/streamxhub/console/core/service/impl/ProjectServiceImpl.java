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
import com.streamxhub.common.util.CommandUtils;
import com.streamxhub.common.util.Utils;
import com.streamxhub.console.base.domain.Constant;
import com.streamxhub.console.base.domain.RestRequest;
import com.streamxhub.console.base.domain.RestResponse;
import com.streamxhub.console.base.utils.GZipUtil;
import com.streamxhub.console.base.utils.SortUtil;
import com.streamxhub.console.core.dao.ProjectMapper;
import com.streamxhub.console.core.entity.Project;
import com.streamxhub.console.core.service.ProjectService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * @author benjobs
 */
@Slf4j
@Service("projectService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final Map<Long, Long> tailOutMap = new HashMap<>();

    private final Map<Long, StringBuilder> tailBuffer = new HashMap<>();

    private final Map<Long, Boolean> tailBeginning = new HashMap<>();

    @Autowired
    private SimpMessageSendingOperations simpMessageSendingOperations;

    @Override
    public RestResponse create(Project project) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Project::getName, project.getName());
        int count = count(queryWrapper);
        RestResponse response = RestResponse.create();
        if (count == 0) {
            project.setDate(new Date());
            boolean status = save(project);
            if (status) {
                return response.message("添加项目成功").data(true);
            } else {
                return response.message("添加项目失败").data(false);
            }
        } else {
            return response.message("该名称的项目已存在,添加任务失败").data(false);
        }
    }

    @Override
    public boolean delete(String id) {
        return false;
    }

    @Override
    public IPage<Project> page(Project project, RestRequest request) {
        Page<Project> page = new Page<>();
        SortUtil.handlePageSort(request, page, "date", Constant.ORDER_DESC, false);
        return this.baseMapper.findProject(page, project);
    }

    private void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public RestResponse build(Long id) {
        Project project = getById(id);
        this.baseMapper.startBuild(project);
        tailBuffer.put(id, new StringBuilder());
        boolean success = cloneOrPull(project);
        if (success) {
            Executors.newSingleThreadExecutor().submit(() -> {
                boolean build = ProjectServiceImpl.this.mavenBuild(project);
                if (build) {
                    ProjectServiceImpl.this.baseMapper.successBuild(project);
                    //发布到apps下
                    ProjectServiceImpl.this.deploy(project);
                    //更新application的发布状态.
                    this.baseMapper.deploy(project.getId());
                } else {
                    ProjectServiceImpl.this.baseMapper.failureBuild(project);
                }
            });
            return RestResponse.create().message("[StreamX] git clone and pull success. begin maven install");
        } else {
            return RestResponse.create().message("[StreamX] clone or pull error.");
        }
    }

    private void deploy(Project project) {
        File path = project.getAppSource();
        List<File> apps = new ArrayList<>();
        // 在项目路径下寻找编译完成的tar.gz(StreamX项目)文件或jar(普通,官方标准的flink工程)...
        findTarAndJar(apps, path);
        apps.forEach((app) -> {
            String appPath = app.getAbsolutePath();
            // 1). tar.gz文件....
            if (appPath.endsWith("tar.gz")) {
                File deployPath = project.getAppBase();
                if (!deployPath.exists()) {
                    deployPath.mkdirs();
                }
                //将项目解包到app下.
                if (app.exists()) {
                    String cmd = String.format("tar -xzvf %s -C %s", app.getAbsolutePath(), deployPath.getAbsolutePath());
                    CommandUtils.execute(cmd);
                }
            } else {
                try {
                    //2) .jar文件(普通,官方标准的flink工程)
                    Utils.checkJarFile(app.toURI().toURL());
                    String moduleName = app.getName().replace(".jar", "");
                    File appBase = project.getAppBase();
                    File targetDir = new File(appBase, moduleName);
                    if (!targetDir.exists()) {
                        targetDir.mkdirs();
                    }
                    File targetJar = new File(targetDir, app.getName());
                    app.renameTo(targetJar);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        });
    }

    private void findTarAndJar(List<File> list, File path) {
        for (File file : Objects.requireNonNull(path.listFiles())) {
            //定位到target目录下:
            if (file.isDirectory() && file.getName().equals("target")) {
                //在target路径下找tar.gz的文件或者jar文件,注意:两者只选其一,不能同时满足,
                File tar = null, jar = null;
                for (File targetFile : Objects.requireNonNull(file.listFiles())) {
                    //1) 一旦找到tar.gz文件则退出.
                    if (targetFile.getName().endsWith("tar.gz")) {
                        tar = targetFile;
                        break;
                    }
                    //2) 尝试寻找jar文件...可能存在发现多个jar.
                    if (!targetFile.getName().startsWith("original-") &&
                            !targetFile.getName().endsWith("-sources.jar") &&
                            targetFile.getName().endsWith(".jar")) {
                        if (jar == null) {
                            jar = targetFile;
                        } else {
                            //可能存在会找到多个jar,这种情况下,选择体积最大的那个jar返回...(不要问我为什么.)
                            if (targetFile.getTotalSpace() > jar.getTotalSpace()) {
                                jar = targetFile;
                            }
                        }
                    }
                }
                File target = tar == null ? jar : tar;
                if (target == null) {
                    throw new RuntimeException("[StreamX] can't find tar.gz or jar in " + file.getAbsolutePath());
                }
                list.add(target);
            }

            if (file.isDirectory()) {
                findTarAndJar(list, file);
            }
        }
    }

    @Override
    public List<Map<String, String>> modules(Long id) {
        Project project = getById(id);
        File appHome = project.getAppBase();
        List<Map<String, String>> list = new ArrayList<>();
        Arrays.stream(Objects.requireNonNull(appHome.listFiles())).forEach((x) -> {
            Map<String, String> map = new HashMap<>();
            map.put("name", x.getName());
            map.put("path", x.getAbsolutePath());
            list.add(map);
        });
        return list;
    }

    @Override
    public List<String> jars(Project project) {
        List<String> list = new ArrayList<>(0);
        File apps = new File(project.getModule());
        for (File file : apps.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                list.add(file.getAbsolutePath());
            }
        }
        return list;
    }

    @Override
    public String getAppConfPath(Long id, String module) {
        Project project = getById(id);
        File appHome = project.getAppBase();
        Optional<File> fileOptional = Arrays.stream(Objects.requireNonNull(appHome.listFiles())).filter((x) -> x.getName().equals(module)).findFirst();
        if (fileOptional.isPresent()) {
            return fileOptional.get().getAbsolutePath();
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> listConf(String module) {
        try {
            File file = new File(module);
            File unzipFile = new File(file.getAbsolutePath().replaceAll(".tar.gz", ""));
            if (!unzipFile.exists()) {
                GZipUtil.decompress(file.getAbsolutePath(), file.getParentFile().getAbsolutePath());
            }
            List<Map<String, Object>> list = new ArrayList<>();
            //只过滤conf这个目录
            File[] files = unzipFile.listFiles(x -> x.getName().equals("conf"));
            assert files != null;
            for (File item : files) {
                eachFile(item, list, true);
            }
            return list;
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return null;
    }

    private boolean cloneOrPull(Project project) {
        boolean isCloned = project.isCloned();
        try {
            if (isCloned) {
                FileRepository fileRepository = new FileRepository(project.getGitRepository());
                Git git = new Git(fileRepository);
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef(project.getBranches()).call();

                log.info("[StreamX] pull starting...");

                tailBuffer.get(project.getId()).append(project.getLog4PullStart());

                PullResult result = git
                        .pull()
                        .setRemote("origin")
                        .setRemoteBranchName(project.getBranches())
                        .setCredentialsProvider(project.getCredentialsProvider())
                        .call();

                tailBuffer.get(project.getId())
                        .append(result.getMergeResult().toString())
                        .append("\n");
                git.close();

                tailBuffer.get(project.getId()).append(String.format("[StreamX] project [%s] git pull successful!\n", project.getName()));

            } else {
                tailBuffer.get(project.getId()).append(project.getLog4CloneStart());
                Git git = Git.cloneRepository()
                        .setURI(project.getUrl())
                        .setDirectory(project.getAppSource())
                        .setBranch(project.getBranches())
                        .setCredentialsProvider(project.getCredentialsProvider())
                        .call();

                File workTree = git.getRepository().getWorkTree();

                gitWorkTree(project.getId(), workTree, "");

                tailBuffer.get(project.getId()).append(String.format("[StreamX] project [%s] git clone successful!\n", project.getName()));
            }
            return true;
        } catch (Exception e) {
            String errorLog = String.format(
                    "[StreamX] project [%s] branch [%s] git %s failure, err: %s",
                    project.getName(),
                    project.getBranches(),
                    isCloned ? "pull " : "clone",
                    e
            );
            tailBuffer.get(project.getId()).append(errorLog);
            e.printStackTrace();
        }
        return false;
    }

    private void gitWorkTree(Long id, File workTree, String spance) {
        File[] files = workTree.listFiles();
        for (File file : Objects.requireNonNull(files)) {
            if (!file.getName().startsWith(".git")) {
                if (file.isFile()) {
                    tailBuffer.get(id)
                            .append(spance)
                            .append("/")
                            .append(file.getName())
                            .append("\n");
                } else if (file.isDirectory()) {
                    tailBuffer.get(id)
                            .append(spance)
                            .append("/")
                            .append(file.getName())
                            .append("\n");
                    gitWorkTree(id, file, spance.concat("/").concat(file.getName()));
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
                for (File item : file.listFiles()) {
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

    /**
     * @param project
     * @return
     */
    private boolean mavenBuild(Project project) {
        StringBuilder builder = tailBuffer.get(project.getId());
        builder.append(project.getLog4BuildStart());
        tailBuffer.put(project.getId(), builder);
        try {
            Process process = Runtime.getRuntime().exec("/bin/bash", null, null);
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream())), true);
            project.getMavenBuildCmd().forEach(out::println);
            Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (tailOutMap.containsKey(project.getId())) {
                    if (tailBeginning.containsKey(project.getId())) {
                        tailBeginning.remove(project.getId());
                        Arrays.stream(builder.toString().split("\n")).forEach(x -> simpMessageSendingOperations.convertAndSend("/resp/tail", x));
                    } else {
                        simpMessageSendingOperations.convertAndSend("/resp/tail", line);
                    }
                }
                builder.append(line).append("\n");
            }
            process.waitFor();
            scanner.close();
            process.getErrorStream().close();
            process.getInputStream().close();
            process.getOutputStream().close();
            process.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String out = builder.toString();
        tailCleanUp(project.getId());
        System.out.println(out);
        return out.contains("BUILD SUCCESS");
    }

    @Override
    public void tailBuildLog(Long id) {
        this.tailOutMap.put(id, id);
        //首次会从buffer里从头读取数据.有且仅有一次.
        this.tailBeginning.put(id, true);
    }

    private void tailCleanUp(Long id) {
        tailOutMap.remove(id);
        tailBeginning.remove(id);
        tailBuffer.remove(id);
    }

}

