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
public enum FlinkMessageStatus implements Status {

    FLINK_ENV_SQL_CLIENT_JAR_NOT_EXIST(10430, "[StreamPark] can't found streampark-flink-sqlclient jar in {0}",
        "[StreamPark] 在{0}中找不到 streampark-flink-sqlclient jar"),
    FLINK_ENV_SQL_CLIENT_JAR_MULTIPLE_EXIST(10440, "[StreamPark] found multiple streampark-flink-sqlclient jar in {0}",
        "[StreamPark] 在 {0} 中发现多个 streampark-flink-sqlclient jar"),
    FLINK_ENV_FILE_OR_DIR_NOT_EXIST(10450, "[StreamPark] file or directory [{0}] no exists. please check.",
        "[StreamPark] 文件或目录 [{0}] 不存在，请检查"),
    FLINK_ENV_FLINK_VERSION_NOT_FOUND(10460, "[StreamPark] can no found flink {0} version",
        "[StreamPark] 无法找到Flink {0} 版本"),
    FLINK_ENV_FLINK_VERSION_UNSUPPORT(10470, "[StreamPark] Unsupported flink version: {0}",
        "[StreamPark] 不支持的Flink版本：{0}"),
    FLINK_ENV_HOME_NOT_EXIST(10480, "The flink home does not exist, please check.", "Flink Home 不存在，请检查"),
    FLINK_ENV_HOME_EXIST_CLUSTER_USE(10490, "The flink home is still in use by some flink cluster, please check.",
        "Flink Home 还在被一些Flink集群使用 请检查"),
    FLINK_ENV_HOME_EXIST_APP_USE(10500, "The flink home is still in use by some application, please check.",
        "Flink Home 仍在被某些应用程序使用 请检查"),

    FLINK_ENV_HOME_IS_DEFAULT_SET(10510, "The flink home is set as default, please change it first.",
        "Flink Home 设置为默认设置，请先更改"),

    FLINK_ENV_CONNECTOR_NULL_ERROR(10520, "The flink connector is null.",
        "Flink连接器为空"),

    FLINK_ENV_DIRECTORY_NOT_CONFIG_FILE(10530, "cannot find {0} in flink/conf ",
        "在 flink/conf 中找不到{0}"),

    FLINK_CLUSTER_UNAVAILABLE(10540, "[StreamPark] The target cluster is unavailable, please check!, please check it",
        "[StreamPark] 目标集群不可用，请检查！"),
    FLINK_CLUSTER_NOT_EXIST(10550, "[StreamPark] The flink cluster don't exist, please check it",
        "[StreamPark] Flink 集群不存在，请检查"),
    FLINK_CLUSTER_NOT_RUNNING(10560, "[StreamPark] The flink cluster not running, please start it",
        "[StreamPark] Flink集群未运行，请启动它"),
    FLINK_CLUSTER_NOT_ACTIVE(10570, "[StreamPark] Current cluster is not active, please check!",
        "[StreamPark] 当前集群未处于活动状态，请检查"),
    FLINK_CLUSTER_DEPLOY_FAILED(10580,
        "[StreamPark] Deploy cluster failed, unknown reason，please check you params or StreamPark error log.",
        "[StreamPark] 部署集群失败，原因不明，请检查您的参数或StreamPark错误日志"),
    FLINK_CLUSTER_ID_CANNOT_FIND_ERROR(10590,
        "The [clusterId={0}] cannot be find, maybe the clusterId is wrong or the cluster has been deleted. Please contact the Admin.",
        "找不到[集群ID={0}]，可能是集群Id错误或集群已被删除。请联系管理员。"),
    FLINK_CLUSTER_ID_EMPTY_ERROR(10600, "[StreamPark] The clusterId can not be empty!", "[StreamPark] 集群Id不能为空!"),
    FLINK_CLUSTER_CLOSE_FAILED(10610, "[StreamPark] Shutdown cluster failed: {0}", "[StreamPark] 关闭群集失败: {0}"),
    FLINK_CLUSTER_DELETE_RUNNING_CLUSTER_FAILED(10620,
        "[StreamPark] Flink cluster is running, cannot be delete, please check.", "[StreamPark] Flink集群正在运行，无法删除 请检查。"),
    FLINK_CLUSTER_EXIST_APP_DELETE_FAILED(10630,
        "[StreamPark] Some app exist on this cluster, the cluster cannot be delete, please check.",
        "[StreamPark] 此集群上存在某些应用程序，无法删除该集群 请检查"),
    FLINK_CLUSTER_EXIST_RUN_TASK_CLOSE_FAILED(10640,
        "[StreamPark] Some app is running on this cluster, the cluster cannot be shutdown",
        "[StreamPark] 某些应用程序正在此集群上运行，无法关闭集群"),

    FLINK_CLUSTER_SHUTDOWN_RESPONSE_FAILED(10650,
        "Get shutdown response failed",
        "获取关机响应失败"),

    FLINK_GATEWAY_NAME_EXIST(10660, "gateway name already exists", "网关名称已存在"),
    FLINK_GATEWAY_GET_VERSION_FAILED(10670, "get gateway version failed", "获取网关版本失败"),

    FLINk_APP_IS_NULL(10671, "Invalid operation, application is null.", "操作无效，应用程序为空"),
    FLINk_SQL_APPID_OR_TEAM_ID_IS_NULL(10680, "Permission denied, appId and teamId cannot be null.",
        "权限被拒绝，应用Id和团队Id不能为空"),
    FLINK_SQL_IS_NULL_UPDATE_FAILED(10690, "Flink sql is null, update flink sql job failed.",
        "FlinkSql为空，更新FlinkSQL作业失败"),
    FLINK_SQL_BACKUP_IS_NULL_ROLLBACK_FAILED(10700, "Application backup can't be null. Rollback flink sql failed.",
        "应用程序备份不能为为空，回滚FlinkSql失败"),

    ;
    private final int code;
    private final String enMsg;
    private final String zhMsg;
}
