/*
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
package com.streamxhub.streamx.console.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.streamxhub.streamx.console.base.util.CommonUtils;
import com.streamxhub.streamx.console.base.util.SpringContextUtils;
import com.streamxhub.streamx.console.core.enums.GitAuthorizedError;
import com.streamxhub.streamx.console.core.service.SettingService;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_project")
public class Project implements Serializable {
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    private String name;

    private String url;

    /**
     * 分支
     */
    private String branches;

    @TableField("LASTBUILD")
    private Date lastBuild;

    private String username;

    private String password;
    /**
     * 1:git 2:svn
     */
    private Integer repository;

    private String pom;

    private Date date;

    private String description;
    /**
     * 构建状态: -1:未构建 0:正在构建 1:构建成功 2:构建失败
     */
    @TableField("BUILDSTATE")
    private Integer buildState;

    /**
     * 1) flink
     * 2) spark
     */
    private Integer type;

    private transient String module;

    private transient String dateFrom;

    private transient String dateTo;

    /**
     * 项目源码路径
     */
    private transient String appSource;

    @JsonIgnore
    private transient SettingService settingService;

    private String getStreamXWorkspace() {
        if (settingService == null) {
            settingService = SpringContextUtils.getBean(SettingService.class);
        }
        return settingService.getStreamXWorkspace();
    }

    /**
     * 获取项目源码路径
     *
     * @return
     */
    @JsonIgnore
    public File getAppSource() {
        if (appSource == null) {
            appSource = getStreamXWorkspace().concat("/project");
        }
        File sourcePath = new File(appSource);
        if (!sourcePath.exists()) {
            sourcePath.mkdirs();
        }
        if (sourcePath.isFile()) {
            throw new IllegalArgumentException("[StreamX] sourcePath must be directory");
        }
        String branches = this.getBranches() == null ? "main" : this.getBranches();
        String rootName = url.replaceAll(".*/|\\.git|\\.svn", "");
        String fullName = rootName.concat("-").concat(branches);
        String path = String.format("%s/%s/%s", sourcePath.getAbsolutePath(), getName(), fullName);
        return new File(path);
    }

    @JsonIgnore
    public File getAppBase() {
        String appBase = getStreamXWorkspace().concat("/app/");
        return new File(appBase.concat(id.toString()));
    }

    @JsonIgnore
    public CredentialsProvider getCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider(this.username, this.password);
    }

    @JsonIgnore
    public File getGitRepository() {
        File home = getAppSource();
        return new File(home, ".git");
    }

    @JsonIgnore
    public void delete() throws IOException {
        File file = getGitRepository();
        FileUtils.deleteDirectory(file);
    }

    @JsonIgnore
    public List<String> getAllBranches() {
        try {
            Collection<Ref> refList;
            if (CommonUtils.notEmpty(username, password)) {
                UsernamePasswordCredentialsProvider pro = new UsernamePasswordCredentialsProvider(username, password);
                refList = Git.lsRemoteRepository().setRemote(url).setCredentialsProvider(pro).call();
            } else {
                refList = Git.lsRemoteRepository().setRemote(url).call();
            }
            List<String> branchList = new ArrayList<>(4);
            for (Ref ref : refList) {
                String refName = ref.getName();
                if (refName.startsWith("refs/heads/")) {
                    String branchName = refName.replace("refs/heads/", "");
                    branchList.add(branchName);
                }
            }
            return branchList;
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    @JsonIgnore
    public GitAuthorizedError gitCheck() {
        try {
            if (CommonUtils.notEmpty(username, password)) {
                UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
                Git.lsRemoteRepository().setRemote(url).setCredentialsProvider(credentialsProvider).call();
            } else {
                Git.lsRemoteRepository().setRemote(url).call();
            }
            return GitAuthorizedError.SUCCESS;
        } catch (Exception e) {
            String err = e.getMessage();
            if (err.contains("not authorized")) {
                return GitAuthorizedError.ERROR;
            } else if (err.contains("Authentication is required")) {
                return GitAuthorizedError.REQUIRED;
            }
            return GitAuthorizedError.UNKNOW;
        }
    }

    @JsonIgnore
    public boolean isCloned() {
        File repository = getGitRepository();
        return repository.exists();
    }

    /**
     * 如果检查到项目已经存在被clone过,则先删除,
     * 主要是解决: 如果最新拉取的代码里有文件删除等,本地不会自动删除
     * 可能会引发不可预知的错误
     */
    public void cleanCloned() throws IOException {
        if (isCloned()) {
            FileUtils.deleteDirectory(getAppSource());
            FileUtils.deleteDirectory(getAppBase());
        }
    }

    @JsonIgnore
    public List<String> getMavenBuildCmd() {
        String buildHome = this.getAppSource().getAbsolutePath();
        if (CommonUtils.notEmpty(this.getPom())) {
            buildHome = new File(buildHome.concat("/").concat(this.getPom()))
                .getParentFile()
                .getAbsolutePath();
        }
        return Arrays.asList("cd ".concat(buildHome), "mvn clean install -DskipTests");
    }

    @JsonIgnore
    public String getLog4BuildStart() {
        return String.format(
            "%s project [%s] branches [%s],maven install beginning! cmd: %s\n\n",
            getLogHeader("maven"),
            getName(),
            getBranches(),
            getMavenBuildCmd()
        );
    }

    @JsonIgnore
    public String getLog4PullStart() {
        return String.format(
            "%s project [%s] branches [%s] remote [origin],git pull beginning!\n\n",
            getLogHeader("git pull"),
            getName(),
            getBranches()
        );
    }

    @JsonIgnore
    public String getLog4CloneStart() {
        return String.format(
            "%s project [%s] branches [%s], clone into [%s],git clone beginning!\n\n",
            getLogHeader("git clone"),
            getName(),
            getBranches(),
            getAppSource()
        );
    }

    @JsonIgnore
    private String getLogHeader(String header) {
        return "---------------------------------[ " + header + " ]---------------------------------\n";
    }

}
