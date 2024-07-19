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

package org.apache.streampark.console.base.enums;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public enum MessageStatus {

    SUCCESS(0, "success", "成功"),
    UNKNOWN_ERROR(100, "unknown error: {0}", "未知错误: {0}"),

    PROJECT(2, "Project", "项目"),
    TEAM(2, "Team", "团队"),
    VARIABLES(3, "Variables", "变量"),
    APPLICATION(3, "Application", "应用程序"),

    SYSTEM_USER_LOGIN_TYPE_CONSTRAINTS(1000, "user {0} can only sign in with [{1}]", "用户{0}只能使用 [{1}] 登录"),
    SYSTEM_USER_ALLOW_LOGIN_TYPE(1000, "user {0} can not login with {1}", "用户{0}无法使用{1}登录"),
    SYSTEM_USER_NOT_LOGIN(1000, "Permission denied, please login first.", "权限被拒绝，请先登录"),
    SYSTEM_USER_NOT_BELONG_TEAM_LOGIN(1000,
        "The current user does not belong to any team, please contact the administrator!", "当前用户不属于任何团队，请联系管理员！"),
    SYSTEM_USER_NOT_EXIST(1000, "User {0} does not exist", "用户{0}不存在"),

    SYSTEM_VARIABLES_EXIST_USE(1000, "The variable is actually used.", "该变量实际上是在用的"),
    SYSTEM_VARIABLES_ALREADY_EXIST(1000, "The variable code already exists", "变量代码已存在"),

    SYSTEM_PERMISSION_LOGIN_USER_PERMISSION_MISMATCH(1000,
        "Permission denied, operations can only be performed with the permissions of the currently logged-in user.",
        "权限被拒绝，只能使用当前登录用户的权限进行操作"),
    SYSTEM_PERMISSION_TEAM_NO_PERMISSION(1000,
        "Permission denied, only members of this team can access this permission.", "权限被拒绝，只有此团队的成员才能访问此权限"),
    SYSTEM_PERMISSION_JOB_OWNER_MISMATCH(1000,
        "Permission denied, this job not created by the current user, And the job cannot be found in the current user's team.",
        "权限被拒绝，此作业不是由当前用户创建的，并且在当前用户的团队中找不到该作业"),

    SYSTEM_TEAM_NOT_EXIST(1, "The team {0} doesn't exist.", "团队{0} 不存在。"),
    SYSTEM_TEAM_EXIST_MODULE_USE_DELETE_ERROR(1, "Please delete the {1} under the team[{0}] first!",
        "请先删除团队[{0}]下的{1}！"),

    RESOURCE_ALREADY_ERROR(10000, "the resource {0} already exists, please check.", "资源{0}已经存在，请检查"),
    RESOURCE_NOT_EXIST_ERROR(10000, "the resource {0} doesn't exists, please check.", "资源{0}不存在，请检查"),
    RESOURCE_STILL_USE_DELETE_ERROR(10000, "The resource is still in use, cannot be removed.", "资源仍在使用中，无法删除。"),
    RESOURCE_POM_JAR_EMPTY(10000, "Please add pom or jar resource.", "请添加pom或jar资源。"),
    RESOURCE_FLINK_APP_JAR_EMPTY_ERROR(10000, "Please upload jar for Flink App resource", "请上传 jar 以获取Flink App资源"),
    RESOURCE_MULTI_FILE_ERROR(10000, "Please do not add multi dependency at one time.", "请不要一次添加多个依赖项"),
    RESOURCE_NAME_MODIFY_ERROR(10000, "Please make sure the resource name is not changed.", "请确保未更改资源名称"),
    RESOURCE_FLINK_JAR_NULL(10000, "flink app jar must exist.", "Flink App Jar 必须存在"),

    MEMBER_USER_TEAM_ALREADY_ERROR(20000, "The user [{0}] has been added the team [{1}], please don't add it again.",
        "用户 [{0}] 已添加到团队 [{1}]，请不要再次添加"),

    FLINK_ENV_SQL_CLIENT_JAR_NOT_EXIST(1, "[StreamPark] can't found streampark-flink-sqlclient jar in {0}",
        "[StreamPark] 在{0}中找不到 streampark-flink-sqlclient jar"),
    FLINK_ENV_SQL_CLIENT_JAR_MULTIPLE_EXIST(1, "[StreamPark] found multiple streampark-flink-sqlclient jar in {0}",
        "[StreamPark] 在 {0} 中发现多个 streampark-flink-sqlclient jar"),
    FLINK_ENV_FILE_OR_DIR_NOT_EXIST(1, "[StreamPark] file or directory [{0}] no exists. please check.",
        "[StreamPark] 文件或目录 [{0}] 不存在，请检查"),
    FLINK_ENV_FLINK_VERSION_NOT_FOUND(1, "[StreamPark] can no found flink {0} version",
        "[StreamPark] 无法找到Flink {0} 版本"),
    FLINK_ENV_FLINK_VERSION_UNSUPPORT(1, "[StreamPark] Unsupported flink version: {0}", "[StreamPark] 不支持的Flink版本：{0}"),
    FLINK_ENV_HOME_NOT_EXIST(1, "The flink home does not exist, please check.", "Flink Home 不存在，请检查"),
    FLINK_ENV_HOME_EXIST_CLUSTER_USE(1, "The flink home is still in use by some flink cluster, please check.",
        "Flink Home 还在被一些Flink集群使用 请检查"),
    FLINK_ENV_HOME_EXIST_APP_USE(30000, "The flink home is still in use by some application, please check.",
        "Flink Home 仍在被某些应用程序使用 请检查"),

    FLINK_CLUSTER_CLUSTER_ID_EMPTY(1, "[StreamPark] The clusterId can not be empty!", "[StreamPark] 集群Id不能为空!"),
    FLINK_CLUSTER_CLOSE_FAILED(1, "[StreamPark] Shutdown cluster failed: {0}", "[StreamPark] 关闭群集失败: {0}"),
    FLINK_CLUSTER_DELETE_RUNNING_CLUSTER_FAILED(1,
        "[StreamPark] Flink cluster is running, cannot be delete, please check.", "[StreamPark] Flink集群正在运行，无法删除 请检查。"),
    FLINK_CLUSTER_EXIST_APP_DELETE_FAILED(1,
        "[StreamPark] Some app exist on this cluster, the cluster cannot be delete, please check.",
        "[StreamPark] 此集群上存在某些应用程序，无法删除该集群 请检查"),
    FLINK_CLUSTER_EXIST_RUN_TASK_CLOSE_FAILED(1,
        "[StreamPark] Some app is running on this cluster, the cluster cannot be shutdown",
        "[StreamPark] 某些应用程序正在此集群上运行，无法关闭集群"),

    FLINK_GATEWAY_NAME_EXIST(1, "gateway name already exists", "网关名称已存在"),

    FLINk_APP_IS_NULL(1, "Invalid operation, application is null.", "操作无效，应用程序为空"),
    FLINk_SQL_APPID_OR_TEAMID_IS_NULL(1, "Permission denied, appId and teamId cannot be null.", "权限被拒绝，应用Id和团队Id不能为空"),

    HANDLER_UPLOAD_FILE_IS_NULL_ERROR(1, "File to upload can't be null. Upload file failed.", "要上传的文件不能为空，上传文件失败"),
    HANDLER_UPLOAD_FILE_TYPE_ILLEGAL_ERROR(1,
        "Illegal file type, Only support standard jar or python files. Upload file failed.",
        "文件类型非法，仅支持标准jar或python文件 上传文件失败。"),

    APP_NOT_EXISTS_ERROR(1, "[StreamPark] {0} The application cannot be started repeatedly.",
        "[StreamPark] {0} 应用程序无法重复启动。"),
    APP_ACTION_REPEAT_START_ERROR(1, "[StreamPark] {0} The application cannot be started repeatedly.",
        "[StreamPark] {0} 应用程序无法重复启动。"),
    APP_ACTION_SAME_TASK_IN_ALREADY_RUN_ERROR(1, "[StreamPark] The same task name is already running in the yarn queue",
        "[StreamPark] 相同的任务名称已在Yarn队列中运行"),
    APP_CONFIG_FILE_TYPE_ILLEGALLY(1, "application' config error. must be (.properties|.yaml|.yml |.conf)",
        "应用程序配置错误，必须是（.properties.yaml|.YML|.conf）"),

    APP_JOB_IS_INVALID(1, "The job is invalid, or the job cannot be built while it is running", "作业无效，或者在作业运行时无法生成作业"),

    EXTERNAL_LINK_PARAM_EXISTING_ERROR(1, "{0}:{1} is already existing.", "{0}:{1}已经能存在"),

    PROJECT_NAME_EXIST(1, "project name already exists", "项目名称已存在"),
    PROJECT_GIT_PASSWORD_DECRYPT_FAILED(1, "Project Github/Gitlab password decrypt failed", "项目 Github/Gitlab 密码解密失败"),
    PROJECT_TEAM_ID_MODIFY_ERROR(1, "TeamId can't be changed", "无法更改TeamId"),
    PROJECT_BUILDING_STATE(1, "The project is being built", "该项目正在建设中"),
    PROJECT_RUNNING_BUILDING_EXCEED_LIMIT(1, "The number of running Build projects exceeds the maximum number: {0}",
        "正在运行的Build项目数超过最大数量: {0}"),

    SSO_SINGLE_SIGN_NOT_AVAILABLE(1,
        "Single Sign On (SSO) is not available, please contact the administrator to enable", "单点登录（SSO）不可用，请联系管理员以启用"),
    SSO_CONFIG_PRINCIPAL_NAME_ERROR(1, "Please configure the correct Principal Name Attribute", "请配置正确的主体名称属性"),

    ;

    private final int code;
    private final String enMsg;
    private final String zhMsg;

    MessageStatus(int code, String enMsg, String zhMsg) {
        this.code = code;
        this.enMsg = enMsg;
        this.zhMsg = zhMsg;
    }

    public int getCode() {
        return this.code;
    }

    public String getMsg() {
        if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage())) {
            return this.zhMsg;
        } else {
            return this.enMsg;
        }
    }
}
