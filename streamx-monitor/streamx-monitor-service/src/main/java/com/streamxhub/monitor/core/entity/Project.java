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
package com.streamxhub.monitor.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.streamxhub.monitor.base.properties.StreamXProperties;
import com.streamxhub.monitor.base.utils.CommonUtil;
import com.streamxhub.monitor.base.utils.SpringContextUtil;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_project")
@Excel("flink项目实体")
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
     * 1:git
     * 2:svn
     */
    private Integer repository;

    private String pom;

    private Date date;

    private String description;
    /**
     * 构建状态:
     * -1:未构建
     * 0:正在构建
     * 1:构建成功
     * 2:构建失败
     */
    @TableField("BUILDSTATE")
    private Integer buildState;

    private transient String dateFrom;

    private transient String dateTo;

    /**
     * 项目源码路径
     */
    private transient String appSource;

    /**
     * 获取项目源码路径
     *
     * @return
     */
    @JsonIgnore
    public File getAppSource() {
        if (appSource == null) {
            appSource = SpringContextUtil.getBean(StreamXProperties.class).getAppHome().concat("/project");
        }
        File sourcePath = new File(appSource);
        if (!sourcePath.exists()) {
            sourcePath.mkdirs();
        }
        if (sourcePath.isFile()) {
            throw new IllegalArgumentException("[StreamX] sourcePath must be directory");
        }
        String branches = this.getBranches() == null ? "master" : this.getBranches();
        String rootName = url.replaceAll(".*/|\\.git|\\.svn", "");
        String fullName = rootName.concat("-").concat(branches);
        String path = String.format("%s/%s/%s", sourcePath.getAbsolutePath(), getName(), fullName);
        return new File(path);
    }

    @JsonIgnore
    public File getAppBase() {
        String appBase = SpringContextUtil.getBean(StreamXProperties.class).getAppHome().concat("/app/");
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
    public boolean isCloned() {
        File repository = getGitRepository();
        return repository.exists();
    }

    @JsonIgnore
    public List<String> getMavenBuildCmd() {
        String buildHome = this.getAppSource().getAbsolutePath();
        if (CommonUtil.notEmpty(this.getPom())) {
            buildHome = new File(buildHome.concat("/").concat(this.getPom())).getParentFile().getAbsolutePath();
        }
        return Arrays.asList("cd ".concat(buildHome), "mvn clean install -Dmaven.test.skip=true", "exit");
    }

    @JsonIgnore
    public String getLog4BuildStart() {
        return String.format("%s[StreamX] project [%s] branches [%s],maven install beginning! cmd: %s\n\n",
                getLogHeader("maven"),
                getName(),
                getBranches(),
                getMavenBuildCmd()
        );
    }

    @JsonIgnore
    public String getLog4PullStart() {
        return String.format("%s[StreamX] project [%s] branches [%s] remote [origin],git pull beginning!\n\n",
                getLogHeader("git pull"),
                getName(),
                getBranches()
        );
    }

    @JsonIgnore
    public String getLog4CloneStart() {
        return String.format("%s[StreamX] project [%s] branches [%s], clone into [%s],git clone beginning!\n\n",
                getLogHeader("git clone"),
                getName(),
                getBranches(),
                getAppSource()
        );
    }

    @JsonIgnore
    public String getLogHeader(String header) {
        return "---------------------------------[ " + header + " ]---------------------------------\n";
    }

}
