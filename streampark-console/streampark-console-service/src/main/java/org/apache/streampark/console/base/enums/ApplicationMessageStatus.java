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

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ApplicationMessageStatus implements Status {

    HANDLER_UPLOAD_FILE_IS_NULL_ERROR(10840, "File to upload can't be null. Upload file failed.", "要上传的文件不能为空，上传文件失败"),
    HANDLER_UPLOAD_FILE_TYPE_ILLEGAL_ERROR(10850,
        "Illegal file type, Only support standard jar or python files. Upload file failed.",
        "文件类型非法，仅支持标准jar或python文件 上传文件失败。"),

    APP_CREATE_FAILED(10860, "create application failed.", "创建应用程序失败"),
    APP_ID_NOT_EXISTS_ERROR(10870, "The application id={0} can't be found.", "找不到应用程序 id={0}"),
    APP_ID_NOT_EXISTS_REVOKE_FAILED(10880, "The application id={0} can't be found, revoke failed.",
        "找不到应用程序 id={0}, 撤销失败"),
    APP_EXECUTE_MODE_NOT_EXISTS_ERROR(10890, "ExecutionMode can't be null.", "执行模式不能为空"),
    APP_EXECUTE_MODE_OPERATION_DISABLE_ERROR(10900, "The FlinkExecutionMode [{0}] can't [{1}]!",
        "Flink执行模式[{0}]无法{1}！"),
    APP_NOT_EXISTS_ERROR(10920, "[StreamPark] {0} The application cannot be started repeatedly.",
        "[StreamPark] {0} 应用程序无法重复启动。"),
    APP_ACTION_REPEAT_START_ERROR(10930, "[StreamPark] {0} The application cannot be started repeatedly.",
        "[StreamPark] {0} 应用程序无法重复启动。"),
    APP_ACTION_SAME_TASK_IN_ALREADY_RUN_ERROR(10940,
        "[StreamPark] The same task name is already running in the yarn queue",
        "[StreamPark] 相同的任务名称已在Yarn队列中运行"),
    APP_ACTION_YARN_CLUSTER_STATE_CHECK(10950, "[StreamPark] The yarn cluster service state is {0}, please check it",
        "[StreamPark] Yarn 集群服务状态为 {0}，请检查一下"),
    APP_CONFIG_FILE_TYPE_ILLEGALLY(10960, "application' config error. must be (.properties|.yaml|.yml |.conf)",
        "应用程序配置错误，必须是（.properties.yaml|.YML|.conf）"),

    APP_JOB_IS_INVALID(10970, "The job is invalid, or the job cannot be built while it is running",
        "作业无效，或者在作业运行时无法生成作业"),
    APP_JOB_EXECUTION_MODE_ILLEGALLY(10980, "Job executionMode must be kubernetes-session|kubernetes-application.",
        "Job 执行模式必须是 Kubernetes-session 或 Kubernetes-application"),
    APP_PY_FLINK_FILE_IS_NULL(10990, "pyflink file can't be null, start application failed.",
        "PyFlink 文件不能为空，启动应用程序失败"),
    APP_PY_FLINK_FILE_TYPE_ILLEGALLY(101000,
        "pyflink format error, must be a \".py\" suffix, start application failed.",
        "PyFlink格式错误，必须是 \".py\" 后缀，启动应用程序失败"),

    APP_QUEUE_LABEL_IN_TEAM_ILLEGALLY(101010,
        "Queue label [{0}] isn't available for teamId [{1}], please add it into the team first.",
        "队列标签 [{0}] 不适用于 teamId [{1}]，请先将其添加到团队中。"),

    APP_QUEUE_LABEL_IN_DATABASE_ILLEGALLY(101020,
        "Queue label [{0}] isn't available in database, please add it first.",
        "队列标签[{0}]在数据库中不可用，请先添加它"),

    APP_NAME_REPEAT_COPY_FAILED(101030,
        "Application names can't be repeated, copy application failed.",
        "应用程序名称不能重复，复制应用程序失败。"),

    APP_FLINK_CLUSTER_NOT_RUNNING_UPDATE_FAILED(101040,
        "update failed, because bind flink cluster not running",
        "更新失败，因为绑定Flink集群未运行"),

    APP_BUILD_RESOURCE_GROUP_FAILED(101050, "Parse resource group failed", "分析资源组失败"),

    EXTERNAL_LINK_PARAM_EXISTING_ERROR(101060, "{0}:{1} is already existing.", "{0}:{1}已经能存在"),

    API_NOT_SUPPORT(101150, "current api unsupported: {0}", "当前API不受支持: {0}"),
    ;
    private final int code;
    private final String enMsg;
    private final String zhMsg;

}
