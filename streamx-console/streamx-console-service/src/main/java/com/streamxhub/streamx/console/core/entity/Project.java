/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.entity;

import com.streamxhub.streamx.common.conf.Workspace;
import com.streamxhub.streamx.common.util.CommandUtils;
import com.streamxhub.streamx.console.base.util.CommonUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.enums.GitAuthorizedError;
import com.streamxhub.streamx.console.core.service.SettingService;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_project")
public class Project implements Serializable {
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;
    private Long teamId;

    private String name;

    private String url;

    /**
     * 分支
     */
    private String branches;

    private Date lastBuild;

    private String userName;

    private String password;
    /**
     * 1:git 2:svn
     */
    private Integer repository;

    private String pom;

    private String buildArgs;

    private Date date;

    private String description;
    /**
     * 构建状态: -2:发生变更,需重新build -1:未构建 0:正在构建 1:构建成功 2:构建失败
     */
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

    private transient List<Long> teamIdList;

    @JsonIgnore
    private transient SettingService settingService;

    private transient String teamName;

    /**
     * 获取项目源码路径
     *
     * @return
     */
    @JsonIgnore
    public File getAppSource() {
        if (appSource == null) {
            appSource = Workspace.local().PROJECT_LOCAL_DIR();
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
    public File getDistHome() {
        return new File(Workspace.local().APP_LOCAL_DIST(), id.toString());
    }

    @JsonIgnore
    public CredentialsProvider getCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider(this.userName, this.password);
    }

    @JsonIgnore
    public File getGitRepository() {
        File home = getAppSource();
        return new File(home, ".git");
    }

    @JsonIgnore
    public void delete() throws IOException {
        FileUtils.deleteDirectory(getAppSource());
        FileUtils.deleteDirectory(getDistHome());
    }

    @JsonIgnore
    public List<String> getAllBranches() {
        try {
            Collection<Ref> refList;
            if (CommonUtils.notEmpty(userName, password)) {
                UsernamePasswordCredentialsProvider pro = new UsernamePasswordCredentialsProvider(userName, password);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @JsonIgnore
    public GitAuthorizedError gitCheck() {
        try {
            if (CommonUtils.notEmpty(userName, password)) {
                UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(userName, password);
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
            this.delete();
        }
    }

    @JsonIgnore
    public List<String> getMavenArgs() {
        String mvn = "mvn";
        try {
            if (CommonUtils.isWindows()) {
                CommandUtils.execute("mvn.cmd --version");
            } else {
                CommandUtils.execute("mvn --version");
            }
        } catch (Exception e) {
            if (CommonUtils.isWindows()) {
                mvn = WebUtils.getAppHome().concat("/bin/mvnw.cmd");
            } else {
                mvn = WebUtils.getAppHome().concat("/bin/mvnw");
            }
        }
        return Arrays.asList(mvn.concat(" clean package -DskipTests ").concat(StringUtils.isEmpty(this.buildArgs) ? "" : this.buildArgs.trim()));
    }

    @JsonIgnore
    public String getMavenWorkHome() {
        String buildHome = this.getAppSource().getAbsolutePath();
        if (CommonUtils.notEmpty(this.getPom())) {
            buildHome = new File(buildHome.concat("/")
                .concat(this.getPom()))
                .getParentFile()
                .getAbsolutePath();
        }
        return buildHome;
    }

    @JsonIgnore
    public String getLog4BuildStart() {
        return String.format(
            "%sproject : %s\nbranches: %s\ncommand : %s\n\n",
            getLogHeader("maven install"),
            getName(),
            getBranches(),
            getMavenArgs()
        );
    }

    @JsonIgnore
    public String getLog4CloneStart() {
        return String.format(
            "%sproject  : %s\nbranches : %s\nworkspace: %s\n\n",
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
