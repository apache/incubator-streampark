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
import com.streamxhub.common.util.CommandUtils;
import com.streamxhub.flink.monitor.base.domain.Constant;
import com.streamxhub.flink.monitor.base.domain.RestRequest;
import com.streamxhub.flink.monitor.base.domain.RestResponse;
import com.streamxhub.flink.monitor.base.utils.GZipUtil;
import com.streamxhub.flink.monitor.base.utils.SortUtil;
import com.streamxhub.flink.monitor.core.dao.ProjectMapper;
import com.streamxhub.flink.monitor.core.entity.Project;
import com.streamxhub.flink.monitor.core.service.ProjectService;
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

@Slf4j
@Service("projectService")
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final Map<Long, Long> tailOutMap = new HashMap<>();

    private final Map<Long, StringBuilder> tailBuffer = new HashMap<>();

    private final Map<Long, Boolean> tailBegining = new HashMap<>();

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
                return response.message("添加任务成功");
            } else {
                return response.message("添加任务失败");
            }
        } else {
            return response.message("该名称的项目已存在,添加任务失败");
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
            new Thread(() -> {
                boolean build = mavenBuild(project);
                if (build) {
                    this.baseMapper.successBuild(project);
                    //发布到apps下
                    this.deploy(project);
                } else {
                    this.baseMapper.failureBuild(project);
                }
            }).start();
            return RestResponse.create().message("[StreamX] git clone and pull success. begin maven install");
        } else {
            return RestResponse.create().message("[StreamX] clone or pull error.");
        }
    }

    private void deploy(Project project) {
        List<String> apps = this.scanApp(project);
        apps.forEach((y) -> {
            File unzipFile = new File(y);
            File deployPath = project.getAppBase();
            if (!deployPath.exists()) {
                deployPath.mkdirs();
            }
            //将项目解包到app下.
            if (unzipFile.exists()) {
                String cmd = String.format("tar -xzvf %s -C %s", unzipFile.getAbsolutePath(), deployPath.getAbsolutePath());
                CommandUtils.execute(cmd);
            }
        });
    }

    @Override
    public List<Map<String, String>> listApp(Long id) {
        Project project = getById(id);
        File appHome = project.getAppBase();
        List<Map<String, String>> list = new ArrayList<>();
        Arrays.stream(Objects.requireNonNull(appHome.listFiles())).forEach((x)->{
            Map<String,String> map = new HashMap<>();
            map.put("name",x.getName());
            map.put("path",x.getAbsolutePath());
            list.add(map);
        });
        return list;
    }

    private List<String> scanApp(Project project) {
        File path = project.getAppSource();
        List<String> list = new ArrayList<>();
        findApp(list, path);
        return list;
    }

    private void findApp(List<String> list, File path) {
        for (File file : Objects.requireNonNull(path.listFiles())) {
            if (file.isFile() &&
                    file.getParentFile().isDirectory() &&
                    file.getParentFile().getName().equals("target") &&
                    file.getName().endsWith("tar.gz")) {
                list.add(file.getAbsolutePath());
            }
            if (file.isDirectory()) {
                findApp(list, file);
            }
        }
    }

    @Override
    public List<Map<String, Object>> listConf(String path) {
        try {
            File file = new File(path);
            File unzipFile = new File(file.getAbsolutePath().replaceAll(".tar.gz", ""));
            if (!unzipFile.exists()) {
                GZipUtil.decompress(file.getAbsolutePath(), file.getParentFile().getAbsolutePath());
            }
            List<Map<String, Object>> list = new ArrayList<>();
            //只过滤conf这个目录
            File[] files = unzipFile.listFiles(item -> item.getName().equals("conf"));
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
                String title = file.getName();
                String value = file.getAbsolutePath();
                Map<String, Object> map = new HashMap<>(0);
                map.put("key", title);
                map.put("title", title);
                map.put("value", value);
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
                    if (tailBegining.containsKey(project.getId())) {
                        tailBegining.remove(project.getId());
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
        //首次会从buffer里从头读取数据.只有一次.
        this.tailBegining.put(id, true);
    }

    private void tailCleanUp(Long id) {
        tailOutMap.remove(id);
        tailBegining.remove(id);
        tailBuffer.remove(id);
    }

}

