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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.util.CommandUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.base.util.GitUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.enums.GitAuthorizedErrorEnum;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.Constants;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
@TableName("t_flink_project")
public class Project implements Serializable {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long teamId;

  private String name;

  private String url;

  /** git branch */
  private String branches;

  private Date lastBuild;

  private Integer gitCredential;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String userName;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String password;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String prvkeyPath;

  /** 1:git 2:svn */
  private Integer repository;

  private String pom;

  private String buildArgs;

  private String description;
  /**
   * Build status: -2: Changed, need to rebuild -1: Not built 0: Building 1: Build successful 2:
   * Build failed
   */
  private Integer buildState;

  /** 1) flink 2) spark */
  private Integer type;

  private Date createTime;

  private Date modifyTime;

  private transient String module;

  private transient String dateFrom;

  private transient String dateTo;

  /** project source */
  private transient String appSource;

  /** get project source */
  @JsonIgnore
  public File getAppSource() {
    if (StringUtils.isBlank(appSource)) {
      appSource = Workspace.PROJECT_LOCAL_PATH();
    }
    File sourcePath = new File(appSource);
    if (!sourcePath.exists()) {
      sourcePath.mkdirs();
    }
    if (sourcePath.isFile()) {
      throw new IllegalArgumentException("[StreamPark] sourcePath must be directory");
    }
    String branches = StringUtils.isBlank(this.getBranches()) ? "main" : this.getBranches();
    String rootName = url.replaceAll(".*/|\\.git|\\.svn", "");
    String fullName = rootName.concat("-").concat(branches);
    String path = String.format("%s/%s/%s", sourcePath.getAbsolutePath(), getName(), fullName);
    return new File(path);
  }

  @JsonIgnore
  public File getDistHome() {
    return new File(Workspace.APP_LOCAL_DIST(), id.toString());
  }

  @JsonIgnore
  public File getGitRepository() {
    File home = getAppSource();
    return new File(home, Constants.DOT_GIT);
  }

  public void delete() throws IOException {
    FileUtils.deleteDirectory(getAppSource());
    FileUtils.deleteDirectory(getDistHome());
  }

  @JsonIgnore
  public List<String> getAllBranches() {
    try {
      return GitUtils.getBranchList(this);
    } catch (Exception e) {
      throw new ApiDetailException(e);
    }
  }

  public GitAuthorizedErrorEnum gitCheck() {
    try {
      GitUtils.getBranchList(this);
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

  @JsonIgnore
  public boolean isCloned() {
    File repository = getGitRepository();
    return repository.exists();
  }

  /**
   * If you check that the project already exists and has been cloned, delete it first, Mainly to
   * solve: if the latest pulling code in the file deletion, etc., the local will not automatically
   * delete, may cause unpredictable errors.
   */
  public void cleanCloned() throws IOException {
    if (isCloned()) {
      this.delete();
    }
  }

  @JsonIgnore
  public String getMavenArgs() {
    boolean windows = Utils.isWindows();
    String mvn = windows ? "mvn.cmd" : "mvn";

    String mavenHome = System.getenv("M2_HOME");
    if (mavenHome == null) {
      mavenHome = System.getenv("MAVEN_HOME");
    }
    if (mavenHome != null) {
      mvn = mavenHome + "/bin/" + mvn;
    }

    try {
      CommandUtils.execute(mvn + " --version");
    } catch (Exception e) {
      if (windows) {
        mvn = WebUtils.getAppHome().concat("/bin/mvnw.cmd");
      } else {
        mvn = WebUtils.getAppHome().concat("/bin/mvnw");
      }
    }

    StringBuilder cmdBuffer = new StringBuilder(mvn).append(" clean package -DskipTests ");

    if (StringUtils.isNotBlank(this.buildArgs)) {
      List<String> dangerArgs = getDangerArgs(this.buildArgs);
      if (dangerArgs.isEmpty()) {
        cmdBuffer.append(this.buildArgs.trim());
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Invalid build args, dangerous operation symbol detected: %s, in your buildArgs: %s",
                dangerArgs.stream().collect(Collectors.joining(",")), this.buildArgs));
      }
    }

    String setting = InternalConfigHolder.get(CommonConfig.MAVEN_SETTINGS_PATH());
    if (StringUtils.isNotBlank(setting)) {
      List<String> dangerArgs = getDangerArgs(setting);
      if (dangerArgs.isEmpty()) {
        File file = new File(setting);
        if (file.exists() && file.isFile()) {
          cmdBuffer.append(" --settings ").append(setting);
        } else {
          throw new IllegalArgumentException(
              String.format("Invalid maven setting path, %s no exists or not file", setting));
        }
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Invalid maven setting path, dangerous operation symbol detected: %s, in your maven setting path: %s",
                dangerArgs.stream().collect(Collectors.joining(",")), setting));
      }
    }
    return cmdBuffer.toString();
  }

  private List<String> getDangerArgs(String param) {
    String[] args = param.split("\\s+");
    List<String> dangerArgs = new ArrayList<>();
    for (String arg : args) {
      if (arg.length() == 1) {
        if (arg.equals("|")) {
          dangerArgs.add("|");
        }
        if (arg.equals("&")) {
          dangerArgs.add("&");
        }
      } else {
        arg = arg.substring(0, 2);
        if (arg.equals("||")) {
          dangerArgs.add("||");
        }
        if (arg.equals("&&")) {
          dangerArgs.add("&&");
        }
      }
    }
    return dangerArgs;
  }

  @JsonIgnore
  public String getMavenWorkHome() {
    String buildHome = this.getAppSource().getAbsolutePath();
    if (StringUtils.isNotEmpty(this.getPom())) {
      buildHome =
          new File(buildHome.concat("/").concat(this.getPom())).getParentFile().getAbsolutePath();
    }
    return buildHome;
  }

  @JsonIgnore
  public String getLog4BuildStart() {
    return String.format(
        "%sproject : %s%nbranches: %s%ncommand : %s%n%n",
        getLogHeader("maven install"), getName(), getBranches(), getMavenArgs());
  }

  @JsonIgnore
  public String getLog4CloneStart() {
    return String.format(
        "%sproject  : %s%nbranches : %s%nworkspace: %s%n%n",
        getLogHeader("git clone"), getName(), getBranches(), getAppSource());
  }

  @JsonIgnore
  private String getLogHeader(String header) {
    return "---------------------------------[ " + header + " ]---------------------------------\n";
  }
}
