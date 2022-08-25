/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

-- ----------------------------
-- Table structure for t_app_backup
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_app_backup` (
`id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
`app_id` bigint DEFAULT NULL,
`sql_id` bigint DEFAULT NULL,
`config_id` bigint DEFAULT NULL,
`version` int DEFAULT NULL,
`path` varchar(255)  DEFAULT NULL,
`description` varchar(255) DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for t_flame_graph
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flame_graph` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint DEFAULT NULL,
`profiler` varchar(255) DEFAULT NULL,
`timeline` datetime DEFAULT NULL,
`content` text ,
PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for t_flink_app
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flink_app` (
`id` bigint NOT NULL AUTO_INCREMENT,
`job_type` tinyint DEFAULT NULL,
`execution_mode` tinyint DEFAULT NULL,
`resource_from` tinyint DEFAULT NULL,
`project_id` varchar(64)  DEFAULT NULL,
`job_name` varchar(255)  DEFAULT NULL,
`module` varchar(255)  DEFAULT NULL,
`jar` varchar(255)  DEFAULT NULL,
`jar_check_sum` bigint DEFAULT NULL,
`main_class` varchar(255)  DEFAULT NULL,
`args` text,
`options` text,
`hot_params` text ,
`user_id` bigint DEFAULT NULL,
`app_id` varchar(255)  DEFAULT NULL,
`app_type` tinyint DEFAULT NULL,
`duration` bigint DEFAULT NULL,
`job_id` varchar(64)  DEFAULT NULL,
`version_id` bigint DEFAULT NULL,
`cluster_id` varchar(255)  DEFAULT NULL,
`k8s_namespace` varchar(255)  DEFAULT NULL,
`flink_image` varchar(255)  DEFAULT NULL,
`state` varchar(50)  DEFAULT NULL,
`restart_size` int DEFAULT NULL,
`restart_count` int DEFAULT NULL,
`cp_threshold` int DEFAULT NULL,
`cp_max_failure_interval` int DEFAULT NULL,
`cp_failure_rate_interval` int DEFAULT NULL,
`cp_failure_action` tinyint DEFAULT NULL,
`dynamic_options` text ,
`description` varchar(255)  DEFAULT NULL,
`resolve_order` tinyint DEFAULT NULL,
`k8s_rest_exposed_type` tinyint DEFAULT NULL,
`flame_graph` tinyint DEFAULT '0',
`jm_memory` int DEFAULT NULL,
`tm_memory` int DEFAULT NULL,
`total_task` int DEFAULT NULL,
`total_tm` int DEFAULT NULL,
`total_slot` int DEFAULT NULL,
`available_slot` int DEFAULT NULL,
`option_state` tinyint DEFAULT NULL,
`tracking` tinyint DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
`modify_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`option_time` datetime DEFAULT NULL,
`launch` tinyint DEFAULT '1',
`build` tinyint DEFAULT '1',
`start_time` datetime DEFAULT NULL,
`end_time` datetime DEFAULT NULL,
`alert_id` bigint DEFAULT NULL,
`k8s_pod_template` text ,
`k8s_jm_pod_template` text ,
`k8s_tm_pod_template` text ,
`k8s_hadoop_integration` tinyint DEFAULT '0',
`flink_cluster_id` bigint DEFAULT NULL,
`ingress_template` text ,
`default_mode_ingress` text ,
`team_id` bigint not null default 1 comment '任务所属组',
PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for t_flink_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flink_config` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint NOT NULL,
`format` tinyint NOT NULL DEFAULT '0',
`version` int NOT NULL,
`latest` tinyint NOT NULL DEFAULT '0',
`content` text  NOT NULL,
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for t_flink_effective
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flink_effective` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint NOT NULL,
`target_type` tinyint NOT NULL COMMENT '1) config 2) flink sql',
`target_id` bigint NOT NULL COMMENT 'configId or sqlId',
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE (`app_id`,`target_type`)
);

-- ----------------------------
-- Table structure for t_flink_env
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flink_env` (
`id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
`flink_name` varchar(128)  NOT NULL COMMENT 'Flink实例名称',
`flink_home` varchar(255)  NOT NULL COMMENT 'Flink Home路径',
`version` varchar(50)  NOT NULL COMMENT 'Flink对应的版本号',
`scala_version` varchar(50)  NOT NULL COMMENT 'Flink对应的scala版本号',
`flink_conf` text  NOT NULL COMMENT 'flink-conf配置内容',
`is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否为默认版本',
`description` varchar(255)  DEFAULT NULL COMMENT '描述信息',
`create_time` datetime NOT NULL COMMENT '创建时间',
PRIMARY KEY (`id`),
UNIQUE (`flink_name`)
);

-- ----------------------------
-- Table structure for t_flink_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flink_log` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint DEFAULT NULL,
`yarn_app_id` varchar(50)  DEFAULT NULL,
`success` tinyint DEFAULT NULL,
`exception` text ,
`option_time` datetime DEFAULT NULL,
 PRIMARY KEY (`id`)
);


-- ----------------------------
-- Table structure for t_flink_project
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flink_project` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255)  DEFAULT NULL,
`url` varchar(1000)  DEFAULT NULL,
`branches` varchar(1000)  DEFAULT NULL,
`user_name` varchar(255)  DEFAULT NULL,
`password` varchar(255)  DEFAULT NULL,
`pom` varchar(255)  DEFAULT NULL,
`build_args` varchar(255) DEFAULT NULL,
`type` tinyint DEFAULT NULL,
`repository` tinyint DEFAULT NULL,
`date` datetime DEFAULT NULL,
`last_build` datetime DEFAULT NULL,
`description` varchar(255)  DEFAULT NULL,
`build_state` tinyint DEFAULT '-1',
`team_id` bigint not null DEFAULT 1 comment '项目所属组',
PRIMARY KEY (`id`)
);


-- ----------------------------
-- Table structure for t_flink_savepoint
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flink_savepoint` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint NOT NULL,
`type` tinyint DEFAULT NULL,
`path` varchar(255)  DEFAULT NULL,
`latest` tinyint NOT NULL,
`trigger_time` datetime DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
 PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for t_flink_sql
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flink_sql` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint DEFAULT NULL,
`sql` text ,
`dependency` text ,
`version` int DEFAULT NULL,
`candidate` tinyint NOT NULL DEFAULT '1',
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`)
);


-- ----------------------------
-- Table structure for t_flink_tutorial
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flink_tutorial` (
`id` int NOT NULL AUTO_INCREMENT,
`type` tinyint DEFAULT NULL,
`name` varchar(255)  DEFAULT NULL,
`content` text ,
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for t_menu
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_menu` (
`menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单/按钮ID',
`parent_id` bigint NOT NULL COMMENT '上级菜单ID',
`menu_name` varchar(50)  NOT NULL COMMENT '菜单/按钮名称',
`path` varchar(255)  DEFAULT NULL COMMENT '对应路由path',
`component` varchar(255)  DEFAULT NULL COMMENT '对应路由组件component',
`perms` varchar(50)  DEFAULT NULL COMMENT '权限标识',
`icon` varchar(50)  DEFAULT NULL COMMENT '图标',
`type` char(2)  DEFAULT NULL COMMENT '类型 0菜单 1按钮',
`display` char(2)  NOT NULL DEFAULT '1' COMMENT '菜单是否显示',
`order_num` int DEFAULT NULL COMMENT '排序',
`create_time` datetime NOT NULL COMMENT '创建时间',
`modify_time` datetime DEFAULT NULL COMMENT '修改时间',
PRIMARY KEY (`menu_id`)
);

-- ----------------------------
-- Table structure for t_message
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_message` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint DEFAULT NULL,
`user_id` bigint DEFAULT NULL,
`type` tinyint DEFAULT NULL,
`title` varchar(255)  DEFAULT NULL,
`context` text ,
`readed` tinyint DEFAULT '0',
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`)
);

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_role` (
`role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
`role_name` varchar(50)  NOT NULL COMMENT '角色名称',
`remark` varchar(100)  DEFAULT NULL COMMENT '角色描述',
`create_time` datetime NOT NULL COMMENT '创建时间',
`modify_time` datetime DEFAULT NULL COMMENT '修改时间',
`role_code` varchar(255)  DEFAULT NULL COMMENT '角色标识',
PRIMARY KEY (`role_id`)
);

-- ----------------------------
-- Table structure for t_role_menu
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_role_menu` (
`id` bigint NOT NULL AUTO_INCREMENT,
`role_id` bigint NOT NULL,
`menu_id` bigint NOT NULL,
PRIMARY KEY (`id`),
UNIQUE (`role_id`,`menu_id`)
);

-- ----------------------------
-- Table structure for t_setting
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_setting` (
`order_num` int DEFAULT NULL,
`setting_key` varchar(50)  NOT NULL,
`setting_value` text  DEFAULT NULL,
`setting_name` varchar(255)  DEFAULT NULL,
`description` varchar(255)  DEFAULT NULL,
`type` tinyint NOT NULL COMMENT '1: input 2: boolean 3: number',
PRIMARY KEY (`setting_key`)
);

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_user` (
`user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
`username` varchar(255)  DEFAULT NULL COMMENT '登录用户名',
`nick_name` varchar(50)  NOT NULL COMMENT '昵称',
`salt` varchar(255)  DEFAULT NULL COMMENT '密码加盐',
`password` varchar(128)  NOT NULL COMMENT '密码',
`email` varchar(128)  DEFAULT NULL COMMENT '邮箱',
`status` char(1)  NOT NULL COMMENT '状态 0锁定 1有效',
`create_time` datetime NOT NULL COMMENT '创建时间',
`modify_time` datetime DEFAULT NULL COMMENT '修改时间',
`last_login_time` datetime DEFAULT NULL COMMENT '最近访问时间',
`sex` char(1)  DEFAULT NULL COMMENT '性别 0男 1女 2保密',
`avatar` varchar(100)  DEFAULT NULL COMMENT '用户头像',
`description` varchar(100)  DEFAULT NULL COMMENT '描述',
PRIMARY KEY (`user_id`),
UNIQUE (`nick_name`)
);

-- ----------------------------
-- Table structure for t_user_role
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_user_role` (
`id` bigint NOT NULL AUTO_INCREMENT,
`user_id` bigint DEFAULT NULL COMMENT '用户ID',
`role_id` bigint DEFAULT NULL COMMENT '角色ID',
PRIMARY KEY (`id`),
UNIQUE (`user_id`,`role_id`)
);

-- ----------------------------
-- Table of t_app_build_pipe
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_app_build_pipe`(
`app_id` BIGINT AUTO_INCREMENT,
`pipe_type` TINYINT,
`pipe_status` TINYINT,
`cur_step` SMALLINT,
`total_step` SMALLINT,
`steps_status` TEXT,
`steps_status_ts` TEXT,
`error` TEXT,
`build_result` TEXT,
`update_time` DATETIME,
 PRIMARY KEY (`app_id`)
);

-- ----------------------------
-- Table of t_flink_cluster
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_flink_cluster` (
`id` bigint NOT NULL AUTO_INCREMENT,
`address` varchar(255) DEFAULT NULL COMMENT 'jobmanager的url地址',
`cluster_id` varchar(255) DEFAULT NULL COMMENT 'session模式的clusterId(yarn-session:application-id,k8s-session:cluster-id)',
`cluster_name` varchar(255) NOT NULL COMMENT '集群名称',
`options` text COMMENT '参数集合json形式',
`yarn_queue` varchar(100) DEFAULT NULL COMMENT '任务所在yarn队列',
`execution_mode` tinyint NOT NULL DEFAULT '1' COMMENT 'session类型(1:remote,3:yarn-session,5:kubernetes-session)',
`version_id` bigint NOT NULL COMMENT 'flink对应id',
`k8s_namespace` varchar(255) DEFAULT 'default' COMMENT 'k8s namespace',
`service_account` varchar(50) DEFAULT NULL COMMENT 'k8s service account',
`description` varchar(255) DEFAULT NULL,
`user_id` bigint DEFAULT NULL,
`flink_image` varchar(255) DEFAULT NULL COMMENT 'flink使用镜像',
`dynamic_options` text COMMENT '动态参数',
`k8s_rest_exposed_type` tinyint DEFAULT '2' COMMENT 'k8s 暴露类型(0:LoadBalancer,1:ClusterIP,2:NodePort)',
`k8s_hadoop_integration` tinyint DEFAULT '0',
`flame_graph` tinyint DEFAULT '0' COMMENT '是否开启火焰图，默认不开启',
`k8s_conf` varchar(255) DEFAULT NULL COMMENT 'k8s配置文件所在路径',
`resolve_order` tinyint DEFAULT NULL,
`exception` text COMMENT '异常信息',
`cluster_state` tinyint DEFAULT '0' COMMENT '集群状态(0:创建未启动,1:已启动,2:停止)',
`create_time` datetime DEFAULT NULL,
primary key (`id`,`cluster_name`),
unique (`cluster_id`,`address`,`execution_mode`)
);


-- ----------------------------
-- Table of t_access_token definition
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_access_token` (
`id` int NOT NULL AUTO_INCREMENT COMMENT 'key',
`user_id` bigint,
`token` varchar(1024) DEFAULT NULL COMMENT 'TOKEN',
`expire_time` datetime DEFAULT NULL COMMENT '过期时间',
`description` varchar(512) DEFAULT NULL COMMENT '使用场景描述',
`status` tinyint DEFAULT NULL COMMENT '1:enable,0:disable',
`create_time` datetime DEFAULT NULL COMMENT 'create time',
`modify_time` datetime DEFAULT NULL COMMENT 'modify time',
 PRIMARY KEY (`id`)
);


-- ----------------------------
-- Table of t_alert_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_alert_config` (
`id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
`user_id` bigint DEFAULT NULL,
`alert_name` varchar(128)  DEFAULT NULL COMMENT '报警组名称',
`alert_type` int DEFAULT 0 COMMENT '报警类型',
`email_params` varchar(255)  COMMENT '邮件报警配置信息',
`sms_params` text  COMMENT '短信报警配置信息',
`ding_talk_params` text  COMMENT '钉钉报警配置信息',
`we_com_params` varchar(255)  COMMENT '企微报警配置信息',
`http_callback_params` text  COMMENT '报警http回调配置信息',
`lark_params` text  COMMENT '飞书报警配置信息',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`modify_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
);


-- ----------------------------
-- Table of t_team
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_team` (
`team_id` bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
`team_code` varchar(255) NOT NULL COMMENT '团队标识 后续可以用于队列 资源隔离相关',
`team_name` varchar(255) NOT NULL COMMENT '团队名',
`create_time` datetime     NOT NULL COMMENT '创建时间',
PRIMARY KEY (`team_id`)
);

-- ----------------------------
-- Table of t_team_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_team_user` (
`team_id` bigint NOT NULL COMMENT 'teamId',
`user_id` bigint NOT NULL COMMENT 'userId',
`create_time` datetime NOT NULL COMMENT '创建时间',
unique (`team_id`,`user_id`)
);
