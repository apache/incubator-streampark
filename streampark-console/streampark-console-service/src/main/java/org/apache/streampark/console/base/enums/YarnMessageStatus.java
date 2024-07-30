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

import org.apache.streampark.console.base.spi.Status;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum YarnMessageStatus implements Status {

    SYSTEM_USER_LOGIN_TYPE_CONSTRAINTS(10000, "user {0} can only sign in with [{1}]", "用户{0}只能使用 [{1}] 登录"),
    SYSTEM_USER_LOGIN_TYPE_NOT_SUPPORT(10010, "The login type [{0}] is not supported", "不支持登录类型[{0}]"),
    SYSTEM_USER_ALLOW_LOGIN_TYPE(10020, "user {0} can not login with {1}", "用户{0}无法使用{1}登录"),
    SYSTEM_USER_NOT_LOGIN(10030, "Permission denied, please login first.", "权限被拒绝，请先登录"),
    SYSTEM_USER_NOT_BELONG_TEAM_LOGIN(10040,
        "The current user does not belong to any team, please contact the administrator!", "当前用户不属于任何团队，请联系管理员！"),
    SYSTEM_USER_NOT_EXIST(10050, "User {0} does not exist", "用户{0}不存在"),
    SYSTEM_USER_ID_NOT_EXIST(10060, "User ID {0} does not exist", "用户ID {0}不存在"),
    SYSTEM_USER_CURRENT_LOGIN_NULL_SET_TEAM_FAILED(10070, "Current login user is null, set team failed.",
        "当前登录用户为空，设置团队失败"),

    SYSTEM_USER_UPDATE_PASSWORD_FAILED(10080, "Can only update password for user who sign in with PASSWORD",
        "只能为使用密码登录的用户更新密码"),
    SYSTEM_USER_OLD_PASSWORD_INCORRECT_UPDATE_PASSWORD_FAILED(10090, "Old password error. Update password failed.",
        "旧密码错误，更新密码失败。"),
    SYSTEM_USER_LOGIN_PASSWORD_INCORRECT(10100, "Incorrect password", "密码不正确"),

    SYSTEM_PERMISSION_LOGIN_USER_PERMISSION_MISMATCH(10160,
        "Permission denied, operations can only be performed with the permissions of the currently logged-in user.",
        "权限被拒绝，只能使用当前登录用户的权限进行操作"),
    SYSTEM_PERMISSION_TEAM_NO_PERMISSION(10170,
        "Permission denied, only members of this team can access this permission.", "权限被拒绝，只有此团队的成员才能访问此权限"),
    SYSTEM_PERMISSION_JOB_OWNER_MISMATCH(10180,
        "Permission denied, this job not created by the current user, And the job cannot be found in the current user's team.",
        "权限被拒绝，此作业不是由当前用户创建的，并且在当前用户的团队中找不到该作业"),

    SYSTEM_TEAM_ALREADY_EXIST(10190, "The team {0} already exist.", "团队{0}已经存在。"),
    SYSTEM_TEAM_NOT_EXIST(10200, "The team {0} doesn't exist.", "团队{0} 不存在。"),
    SYSTEM_TEAM_ID_CANNOT_NULL(10210, "The team id is cannot null.", "团队ID不能为空"),
    SYSTEM_TEAM_ID_NOT_EXIST(10220, "The team id {0} doesn't exist.", "团队ID {0}不存在"),
    SYSTEM_TEAM_NAME_CAN_NOT_CHANGE(10230, "Team name can't be changed. Update team failed.", "团队名称不能更改"),
    SYSTEM_LDAP_NOT_ENABLE(10240, "ldap is not enabled, Please check the configuration: ldap.enable",
        "LDAP未启用，请检查配置：ldap.enable"),

    SYSTEM_TEAM_ID_NULL_ERROR(10250, "Team id mustn't be null.", "团队ID不能为空"),

    SYSTEM_TEAM_EXIST_MODULE_USE_DELETE_ERROR(10260, "Please delete the {1} under the team[{0}] first!",
        "请先删除团队[{0}]下的{1}！"),

    SYSTEM_ROLE_NOT_EXIST(10270, "Role {0} not found.",
        "角色{0}不存在"),
    SYSTEM_ROLE_ID_NOT_EXIST(10280, "Role ID {0} not found.",
        "角色ID{0}不存在"),
    SYSTEM_ROLE_EXIST_USED_DELETE_ERROR(10290,
        "There are some users of role {0}, delete role failed, please unbind it first.",
        "有一些用户的角色{0}，删除角色失败，请先解绑"),

    MEMBER_USER_TEAM_ALREADY_ERROR(10390, "The user [{0}] has been added the team [{1}], please don't add it again.",
        "用户 [{0}] 已添加到团队 [{1}]，请不要再次添加"),
    MEMBER_ID_NOT_EXIST(10400, "The member id {0} doesn't exist.",
        "成员ID {0}不存在"),
    MEMBER_TEAM_ID_CHANGE_ERROR(10410, "Team id cannot be changed.",
        "团队ID无法更改。"),
    MEMBER_USER_ID_CHANGE_ERROR(10420, "User id cannot be changed.",
        "用户 ID 无法更改。"),

    YARN_QUEUE_NOT_EXIST(10740, "The yarn queue doesn't exist.", "Yarn队列不存在"),
    YARN_QUEUE_NULL(10750, "Yarn queue mustn't be empty.", "Yarn队列不能为空"),
    YARN_QUEUE_ID_NULL(10760, "Yarn queue id mustn't be empty.", "Yarn队列ID不能为空"),
    YARN_QUEUE_LABEL_EXIST(10770, "The queue label existed already. Try on a new queue label, please.",
        "队列标签已存在，请尝试使用新的队列标签。"),
    YARN_QUEUE_LABEL_NULL(10780, "Yarn queue label mustn't be empty.", "Yarn队列标签不能为空"),
    YARN_QUEUE_LABEL_AVAILABLE(10790, "The queue label is availableThe queue label is available.", "队列标签可用队列标签可用"),
    YARN_QUEUE_LABEL_FORMAT(10800,
        "Yarn queue label format should be in format '{'queue'}' or '{'queue'}'@'{'label1，label2'}'",
        "Yarn队列标签格式应为格式 '{'queue'}' 或 '{'queue'}'@'{'label1，label2'}'"),
    YARN_QUEUE_QUERY_PARAMS_NULL(10810, "Yarn queue query params mustn't be null.", "Yarn队列查询参数不能为空"),
    YARN_QUEUE_QUERY_PARAMS_TEAM_ID_NULL(10820, "Team id of yarn queue query params mustn't be null.",
        "Yarn队列查询参数的团队ID不能为空"),
    YARN_QUEUE_USED_FORMAT(10830, "Please remove the yarn queue for {0} referenced it before {1}.",
        "请在{1}之前删除{0}引用的Yarn队列"),

    ;
    private final int code;
    private final String enMsg;
    private final String zhMsg;
}
