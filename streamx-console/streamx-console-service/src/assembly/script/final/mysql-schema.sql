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

CREATE database IF NOT EXISTS streamx;
use streamx;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_app_backup
-- ----------------------------
DROP TABLE IF EXISTS `t_app_backup`;
CREATE TABLE `t_app_backup` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint DEFAULT NULL,
`sql_id` bigint DEFAULT NULL,
`confing_id` bigint DEFAULT NULL,
`version` int DEFAULT NULL,
`path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_app_backup
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_flame_graph
-- ----------------------------
DROP TABLE IF EXISTS `t_flame_graph`;
CREATE TABLE `t_flame_graph` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint DEFAULT NULL,
`profiler` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`timeline` datetime DEFAULT NULL,
`content` text COLLATE utf8mb4_general_ci,
PRIMARY KEY (`id`) USING BTREE,
KEY `INX_TIME` (`timeline`),
KEY `INX_APPID` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flame_graph
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_flink_app
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_app`;
CREATE TABLE `t_flink_app` (
`id` bigint NOT NULL AUTO_INCREMENT,
`job_type` tinyint DEFAULT NULL,
`execution_mode` tinyint DEFAULT NULL,
`resource_from` tinyint DEFAULT NULL,
`project_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
`job_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`module` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`jar` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`jar_check_sum` bigint DEFAULT NULL,
`main_class` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`args` text COLLATE utf8mb4_general_ci,
`options` text COLLATE utf8mb4_general_ci,
`hot_params` text COLLATE utf8mb4_general_ci,
`user_id` bigint DEFAULT NULL,
`app_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`app_type` tinyint DEFAULT NULL,
`duration` bigint DEFAULT NULL,
`job_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
`version_id` bigint DEFAULT NULL,
`cluster_id` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`k8s_namespace` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`flink_image` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`state` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
`restart_size` int DEFAULT NULL,
`restart_count` int DEFAULT NULL,
`cp_threshold` int DEFAULT NULL,
`cp_max_failure_interval` int DEFAULT NULL,
`cp_failure_rate_interval` int DEFAULT NULL,
`cp_failure_action` tinyint DEFAULT NULL,
`dynamic_options` text COLLATE utf8mb4_general_ci,
`description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
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
`modify_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`option_time` datetime DEFAULT NULL,
`launch` tinyint DEFAULT '1',
`build` tinyint DEFAULT '1',
`start_time` datetime DEFAULT NULL,
`end_time` datetime DEFAULT NULL,
`alert_id` bigint DEFAULT NULL,
`k8s_pod_template` text COLLATE utf8mb4_general_ci,
`k8s_jm_pod_template` text COLLATE utf8mb4_general_ci,
`k8s_tm_pod_template` text COLLATE utf8mb4_general_ci,
`k8s_hadoop_integration` tinyint DEFAULT '0',
`flink_cluster_id` bigint DEFAULT NULL,
`ingress_template` text COLLATE utf8mb4_general_ci,
`default_mode_ingress` text COLLATE utf8mb4_general_ci,
`team_id` bigint not null default 1 comment '任务所属组',
PRIMARY KEY (`id`) USING BTREE,
KEY `INX_STATE` (`state`) USING BTREE,
KEY `INX_JOB_TYPE` (`job_type`) USING BTREE,
KEY `INX_TRACK` (`tracking`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_app
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_app` VALUES (100000, 2, 4, NULL, NULL, 'Flink SQL Demo', NULL, NULL, NULL, NULL, NULL, NULL , NULL, 100000, NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, 'Flink SQL Demo', 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, NOW(), NOW(), NULL, 1, 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL,1);
COMMIT;

-- ----------------------------
-- Table structure for t_flink_config
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_config`;
CREATE TABLE `t_flink_config` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint NOT NULL,
`format` tinyint NOT NULL DEFAULT '0',
`version` int NOT NULL,
`latest` tinyint NOT NULL DEFAULT '0',
`content` text COLLATE utf8mb4_general_ci NOT NULL,
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_config
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_flink_effective
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_effective`;
CREATE TABLE `t_flink_effective` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint NOT NULL,
`target_type` tinyint NOT NULL COMMENT '1) config 2) flink sql',
`target_id` bigint NOT NULL COMMENT 'configId or sqlId',
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE KEY `UN_EFFECTIVE_INX` (`app_id`,`target_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_effective
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_effective` VALUES (100000, 100000, 2, 100000, NOW());
COMMIT;

-- ----------------------------
-- Table structure for t_flink_env
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_env`;
CREATE TABLE `t_flink_env` (
`id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
`flink_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Flink实例名称',
`flink_home` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Flink Home路径',
`version` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Flink对应的版本号',
`scala_version` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Flink对应的scala版本号',
`flink_conf` text COLLATE utf8mb4_general_ci NOT NULL COMMENT 'flink-conf配置内容',
`is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否为默认版本',
`description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述信息',
`create_time` datetime NOT NULL COMMENT '创建时间',
PRIMARY KEY (`id`) USING BTREE,
UNIQUE KEY `UN_ENV_NAME` (`flink_name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_env
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_flink_log
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_log`;
CREATE TABLE `t_flink_log` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint DEFAULT NULL,
`yarn_app_id` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
`success` tinyint DEFAULT NULL,
`exception` text COLLATE utf8mb4_general_ci,
`option_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_flink_project
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_project`;
CREATE TABLE `t_flink_project` (
`id` bigint NOT NULL AUTO_INCREMENT,
`name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`url` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
`branches` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
`user_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`password` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`pom` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`build_args` varchar(255) DEFAULT NULL,
`type` tinyint DEFAULT NULL,
`repository` tinyint DEFAULT NULL,
`date` datetime DEFAULT NULL,
`last_build` datetime DEFAULT NULL,
`description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`build_state` tinyint DEFAULT '-1',
`team_id` bigint not null DEFAULT 1 comment '项目所属组',
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_project
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_project` VALUES (100000, 'streamx-quickstart', 'https://github.com/streamxhub/streamx-quickstart.git', 'main', NULL, NULL, NULL, NULL, 1, 1, NOW(), NULL, 'streamx-quickstart', 1,1);
COMMIT;

-- ----------------------------
-- Table structure for t_flink_savepoint
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_savepoint`;
CREATE TABLE `t_flink_savepoint` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint NOT NULL,
`type` tinyint DEFAULT NULL,
`path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`latest` tinyint NOT NULL,
`trigger_time` datetime DEFAULT NULL,
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_savepoint
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_flink_sql
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_sql`;
CREATE TABLE `t_flink_sql` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint DEFAULT NULL,
`sql` text COLLATE utf8mb4_general_ci,
`dependency` text COLLATE utf8mb4_general_ci,
`version` int DEFAULT NULL,
`candidate` tinyint NOT NULL DEFAULT '1',
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_sql
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_sql` VALUES (100000, 100000, 'eNqlUUtPhDAQvu+vmFs1AYIHT5s94AaVqGxSSPZIKgxrY2mxrdGfb4GS3c0+LnJo6Mz36syapkmZQpk8vKbQMMt2KOFmAe5rK4Nf3yhrhCwvA1/TTDaqO61UxmooSprlT1PDGkgKEKpmwvIOjWVdP3W2zpG+JfQFHjfU46xxrVvYZuWztye1khJrqzSBFRCfjUwSYQiqt1xJJvyPcbWJp9WPCXvUoUEn0ZAVufcs0nIUjYn2L4s++YiY75eBLr+2Dnl3GYKTWRyfQKYRRR2XZxXmNvu9yh9GHAmUO/sxyMRkGNly4c714RZ7zaWtLHsX+N9NjvVrWxm99jmyvEhpOUhujmIYFI5zkCOYzYIj11a7QH7Tyz+nE8bw', NULL, 1, 1, NOW());
COMMIT;

-- ----------------------------
-- Table structure for t_flink_tutorial
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_tutorial`;
CREATE TABLE `t_flink_tutorial` (
`id` int NOT NULL AUTO_INCREMENT,
`type` tinyint DEFAULT NULL,
`name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`content` text COLLATE utf8mb4_general_ci,
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_tutorial
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_tutorial` VALUES (100000, 1, 'repl', '### Introduction\n\n[Apache Flink](https://flink.apache.org/) is a framework and distributed processing engine for stateful computations over unbounded and bounded data streams. This is Flink tutorial for running classical wordcount in both batch and streaming mode.\n\nThere\'re 3 things you need to do before using flink in StreamX Notebook.\n\n* Download [Flink 1.11](https://flink.apache.org/downloads.html) for scala 2.11 (Only scala-2.11 is supported, scala-2.12 is not supported yet in StreamX Notebook), unpack it and set `FLINK_HOME` in flink interpreter setting to this location.\n* Copy flink-python_2.11–1.11.1.jar from flink opt folder to flink lib folder (it is used by pyflink which is supported)\n* If you want to run yarn mode, you need to set `HADOOP_CONF_DIR` in flink interpreter setting. And make sure `hadoop` is in your `PATH`, because internally flink will call command `hadoop classpath` and put all the hadoop related jars in the classpath of flink interpreter process.\n\nThere\'re 6 sub interpreters in flink interpreter, each is used for different purpose. However they are in the the JVM and share the same ExecutionEnviroment/StremaExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment.\n\n* `flink`	- Creates ExecutionEnvironment/StreamExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment and provides a Scala environment\n* `pyflink`	- Provides a python environment\n* `ipyflink`	- Provides an ipython environment\n* `ssql`	 - Provides a stream sql environment\n* `bsql`	- Provides a batch sql environment\n', NOW());
COMMIT;

-- ----------------------------
-- Table structure for t_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_menu`;
CREATE TABLE `t_menu` (
`menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单/按钮ID',
`parent_id` bigint NOT NULL COMMENT '上级菜单ID',
`menu_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单/按钮名称',
`path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对应路由path',
`component` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对应路由组件component',
`perms` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限标识',
`icon` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
`type` char(2) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型 0菜单 1按钮',
`display` char(2) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '菜单是否显示',
`order_num` double(20,0) DEFAULT NULL COMMENT '排序',
`create_time` datetime NOT NULL COMMENT '创建时间',
`modify_time` datetime DEFAULT NULL COMMENT '修改时间',
PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_menu
-- ----------------------------
BEGIN;
INSERT INTO `t_menu` VALUES (100000, 0, 'System', '/system', 'PageView', NULL, 'desktop', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100001, 100000, 'User Management', '/system/user', 'system/user/User', 'user:view', 'user', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100002, 100000, 'Role Management', '/system/role', 'system/role/Role', 'role:view', 'smile', '0', '1', 2, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100003, 100000, 'Router Management', '/system/menu', 'system/menu/Menu', 'menu:view', 'bars', '0', '1', 3, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100004, 100001, 'add', NULL, NULL, 'user:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100005, 100001, 'update', NULL, NULL, 'user:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100006, 100001, 'delete', NULL, NULL, 'user:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100007, 100002, 'add', NULL, NULL, 'role:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100008, 100002, 'update', NULL, NULL, 'role:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100009, 100002, 'delete', NULL, NULL, 'role:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100010, 100003, 'add', NULL, NULL, 'menu:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100011, 100003, 'update', NULL, NULL, 'menu:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100012, 100001, 'reset', NULL, NULL, 'user:reset', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100013, 0, 'StreamX', '/flink', 'PageView', NULL, 'build', '0', '1', 2, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100014, 100013, 'Project', '/flink/project', 'flink/project/View', 'project:view', 'github', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100015, 100013, 'Application', '/flink/app', 'flink/app/View', 'app:view', 'mobile', '0', '1', 2, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100016, 100013, 'Add Application', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100017, 100013, 'Add Project', '/flink/project/add', 'flink/project/Add', 'project:create', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100018, 100013, 'App Detail', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100019, 100013, 'Notebook', '/flink/notebook/view', 'flink/notebook/Submit', 'notebook:submit', 'read', '0', '1', 3, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100020, 100013, 'Edit Flink App', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100021, 100013, 'Edit StreamX App', '/flink/app/edit_streamx', 'flink/app/EditStreamX', 'app:update', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100022, 100014, 'build', NULL, NULL, 'project:build', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100023, 100014, 'delete', NULL, NULL, 'project:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100024, 100015, 'mapping', NULL, NULL, 'app:mapping', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100025, 100015, 'launch', NULL, NULL, 'app:launch', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100026, 100015, 'start', NULL, NULL, 'app:start', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100027, 100015, 'clean', NULL, NULL, 'app:clean', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100028, 100015, 'cancel', NULL, NULL, 'app:cancel', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100029, 100015, 'savepoint delete', NULL, NULL, 'savepoint:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100030, 100015, 'backup rollback', NULL, NULL, 'backup:rollback', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100031, 100015, 'backup delete', NULL, NULL, 'backup:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100032, 100015, 'conf delete', NULL, NULL, 'conf:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100033, 100015, 'flame Graph', NULL, NULL, 'app:flameGraph', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100034, 100013, 'Setting', '/flink/setting', 'flink/setting/View', 'setting:view', 'setting', '0', '1', 4, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100035, 100034, 'Setting Update', NULL, NULL, 'setting:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100036, 100013, 'Edit Project', '/flink/project/edit', 'flink/project/Edit', 'project:update', NULL, '0', '0', NULL, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100037, 100015, 'delete', NULL, NULL, 'app:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100038, 100000, 'Token Management', '/system/token', 'system/token/Token', 'token:view', 'lock', '0', '1', 1.0, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100039, 100038, 'add', NULL, NULL, 'token:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100040, 100038, 'delete', NULL, NULL, 'token:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100041, 100013, 'Add Cluster', '/flink/setting/add_cluster', 'flink/setting/AddCluster', 'cluster:create', '', '0', '0', null, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100042, 100013, 'Edit Cluster', '/flink/setting/edit_cluster', 'flink/setting/EditCluster', 'cluster:update', '', '0', '0', null, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100043, 100000, 'Team Management', '/system/team', 'system/team/Team', 'team:view', 'team', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100044, 100043, 'add', NULL, NULL, 'team:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100045, 100043, 'update', NULL, NULL, 'team:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100046, 100043, 'delete', NULL, NULL, 'team:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100047, 100015, 'copy', null, null, 'app:copy', null, '1', '1', null, NOW(), null);



COMMIT;

-- ----------------------------
-- Table structure for t_message
-- ----------------------------
DROP TABLE IF EXISTS `t_message`;
CREATE TABLE `t_message` (
`id` bigint NOT NULL AUTO_INCREMENT,
`app_id` bigint DEFAULT NULL,
`user_id` bigint DEFAULT NULL,
`type` tinyint DEFAULT NULL,
`title` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`context` text COLLATE utf8mb4_general_ci,
`readed` tinyint DEFAULT '0',
`create_time` datetime DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
KEY `INX_MES_USER` (`USER_ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_message
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role` (
`role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
`role_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
`remark` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色描述',
`create_time` datetime NOT NULL COMMENT '创建时间',
`modify_time` datetime DEFAULT NULL COMMENT '修改时间',
`role_code` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色标识',
PRIMARY KEY (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_role
-- ----------------------------
BEGIN;
INSERT INTO `t_role` VALUES (100000, 'admin', 'admin', NOW(), NULL, NULL);
INSERT INTO `t_role` VALUES (100001, 'developer', 'developer', NOW(), NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_role_menu`;
CREATE TABLE `t_role_menu` (
`id` bigint NOT NULL AUTO_INCREMENT,
`role_id` bigint NOT NULL,
`menu_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE KEY `UN_ROLE_MENU_INX` (`role_id`,`menu_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_role_menu
-- ----------------------------
BEGIN;
INSERT INTO `t_role_menu` VALUES (100000, 100000, 100000);
INSERT INTO `t_role_menu` VALUES (100001, 100000, 100001);
INSERT INTO `t_role_menu` VALUES (100002, 100000, 100002);
INSERT INTO `t_role_menu` VALUES (100003, 100000, 100003);
INSERT INTO `t_role_menu` VALUES (100004, 100000, 100004);
INSERT INTO `t_role_menu` VALUES (100005, 100000, 100005);
INSERT INTO `t_role_menu` VALUES (100006, 100000, 100006);
INSERT INTO `t_role_menu` VALUES (100007, 100000, 100007);
INSERT INTO `t_role_menu` VALUES (100008, 100000, 100008);
INSERT INTO `t_role_menu` VALUES (100009, 100000, 100009);
INSERT INTO `t_role_menu` VALUES (100010, 100000, 100010);
INSERT INTO `t_role_menu` VALUES (100011, 100000, 100011);
INSERT INTO `t_role_menu` VALUES (100012, 100000, 100012);
INSERT INTO `t_role_menu` VALUES (100013, 100000, 100013);
INSERT INTO `t_role_menu` VALUES (100014, 100000, 100014);
INSERT INTO `t_role_menu` VALUES (100015, 100000, 100015);
INSERT INTO `t_role_menu` VALUES (100016, 100000, 100016);
INSERT INTO `t_role_menu` VALUES (100017, 100000, 100017);
INSERT INTO `t_role_menu` VALUES (100018, 100000, 100018);
INSERT INTO `t_role_menu` VALUES (100019, 100000, 100019);
INSERT INTO `t_role_menu` VALUES (100020, 100000, 100020);
INSERT INTO `t_role_menu` VALUES (100021, 100000, 100021);
INSERT INTO `t_role_menu` VALUES (100022, 100000, 100022);
INSERT INTO `t_role_menu` VALUES (100023, 100000, 100023);
INSERT INTO `t_role_menu` VALUES (100024, 100000, 100024);
INSERT INTO `t_role_menu` VALUES (100025, 100000, 100025);
INSERT INTO `t_role_menu` VALUES (100026, 100000, 100026);
INSERT INTO `t_role_menu` VALUES (100027, 100000, 100027);
INSERT INTO `t_role_menu` VALUES (100028, 100000, 100028);
INSERT INTO `t_role_menu` VALUES (100029, 100000, 100029);
INSERT INTO `t_role_menu` VALUES (100030, 100000, 100030);
INSERT INTO `t_role_menu` VALUES (100031, 100000, 100031);
INSERT INTO `t_role_menu` VALUES (100032, 100000, 100032);
INSERT INTO `t_role_menu` VALUES (100033, 100000, 100033);
INSERT INTO `t_role_menu` VALUES (100034, 100000, 100034);
INSERT INTO `t_role_menu` VALUES (100035, 100000, 100035);
INSERT INTO `t_role_menu` VALUES (100036, 100000, 100036);
INSERT INTO `t_role_menu` VALUES (100037, 100000, 100037);
INSERT INTO `t_role_menu` VALUES (100038, 100000, 100038);
INSERT INTO `t_role_menu` VALUES (100039, 100000, 100039);
INSERT INTO `t_role_menu` VALUES (100040, 100000, 100040);
INSERT INTO `t_role_menu` VALUES (100041, 100001, 100014);
INSERT INTO `t_role_menu` VALUES (100042, 100001, 100016);
INSERT INTO `t_role_menu` VALUES (100043, 100001, 100017);
INSERT INTO `t_role_menu` VALUES (100044, 100001, 100018);
INSERT INTO `t_role_menu` VALUES (100045, 100001, 100019);
INSERT INTO `t_role_menu` VALUES (100046, 100001, 100020);
INSERT INTO `t_role_menu` VALUES (100047, 100001, 100021);
INSERT INTO `t_role_menu` VALUES (100048, 100001, 100022);
INSERT INTO `t_role_menu` VALUES (100049, 100001, 100025);
INSERT INTO `t_role_menu` VALUES (100050, 100001, 100026);
INSERT INTO `t_role_menu` VALUES (100051, 100001, 100027);
INSERT INTO `t_role_menu` VALUES (100052, 100001, 100028);
INSERT INTO `t_role_menu` VALUES (100053, 100001, 100029);
INSERT INTO `t_role_menu` VALUES (100054, 100001, 100030);
INSERT INTO `t_role_menu` VALUES (100055, 100001, 100031);
INSERT INTO `t_role_menu` VALUES (100056, 100001, 100032);
INSERT INTO `t_role_menu` VALUES (100057, 100001, 100033);
INSERT INTO `t_role_menu` VALUES (100058, 100001, 100013);
INSERT INTO `t_role_menu` VALUES (100059, 100001, 100015);
INSERT INTO `t_role_menu` VALUES (100060, 100000, 100041);
INSERT INTO `t_role_menu` VALUES (100061, 100000, 100042);
INSERT INTO `t_role_menu` VALUES (100062, 100000, 100043);
INSERT INTO `t_role_menu` VALUES (100063, 100000, 100044);
INSERT INTO `t_role_menu` VALUES (100064, 100000, 100045);
INSERT INTO `t_role_menu` VALUES (100065, 100000, 100046);
INSERT INTO `t_role_menu` VALUES (100066, 100000, 100047);



COMMIT;

-- ----------------------------
-- Table structure for t_setting
-- ----------------------------
DROP TABLE IF EXISTS `t_setting`;
CREATE TABLE `t_setting` (
`order_num` int DEFAULT NULL,
`setting_key` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
`setting_value` text COLLATE utf8mb4_general_ci DEFAULT NULL,
`setting_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`type` tinyint NOT NULL COMMENT '1: input 2: boolean 3: number',
PRIMARY KEY (`setting_key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_setting
-- ----------------------------
BEGIN;
INSERT INTO `t_setting` VALUES (1, 'streamx.maven.central.repository', NULL, 'Maven Central Repository', 'Maven 私服地址', 1);
INSERT INTO `t_setting` VALUES (2, 'streamx.maven.auth.user', NULL, 'Maven Central Repository Auth User', 'Maven 私服认证用户名', 1);
INSERT INTO `t_setting` VALUES (3, 'streamx.maven.auth.password', NULL, 'Maven Central Repository Auth Password', 'Maven 私服认证密码', 1);
INSERT INTO `t_setting` VALUES (4, 'streamx.console.webapp.address', NULL, 'StreamX Webapp address', 'StreamX Console Web 应用程序HTTP URL', 1);
INSERT INTO `t_setting` VALUES (5, 'alert.email.host', NULL, 'Alert Email Smtp Host', '告警邮箱Smtp Host', 1);
INSERT INTO `t_setting` VALUES (6, 'alert.email.port', NULL, 'Alert Email Smtp Port', '告警邮箱的Smtp Port', 1);
INSERT INTO `t_setting` VALUES (7, 'alert.email.from', NULL, 'Alert  Email From', '发送告警的邮箱', 1);
INSERT INTO `t_setting` VALUES (8, 'alert.email.userName', NULL, 'Alert  Email User', '用来发送告警邮箱的认证用户名', 1);
INSERT INTO `t_setting` VALUES (9, 'alert.email.password', NULL, 'Alert Email Password', '用来发送告警邮箱的认证密码', 1);
INSERT INTO `t_setting` VALUES (10, 'alert.email.ssl', 'false', 'Alert Email Is SSL', '发送告警的邮箱是否开启SSL', 2);
INSERT INTO `t_setting` VALUES (11, 'docker.register.address', NULL, 'Docker Register Address', 'Docker容器服务地址', 1);
INSERT INTO `t_setting` VALUES (12, 'docker.register.user', NULL, 'Docker Register User', 'Docker容器服务认证用户名', 1);
INSERT INTO `t_setting` VALUES (13, 'docker.register.password', NULL, 'Docker Register Password', 'Docker容器服务认证密码', 1);
INSERT INTO `t_setting` VALUES (14, 'docker.register.namespace', NULL, 'Namespace for docker image used in docker building env and target image register', 'Docker命名空间', 1);
COMMIT;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
`user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
`username` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录用户名',
`nick_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '昵称',
`salt` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码加盐',
`password` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
`email` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
`status` char(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态 0锁定 1有效',
`create_time` datetime NOT NULL COMMENT '创建时间',
`modify_time` datetime DEFAULT NULL COMMENT '修改时间',
`last_login_time` datetime DEFAULT NULL COMMENT '最近访问时间',
`sex` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '性别 0男 1女 2保密',
`avatar` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户头像',
`description` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
PRIMARY KEY (`user_id`) USING BTREE,
UNIQUE KEY `UN_USERNAME` (`nick_name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
-- ----------------------------
-- Records of t_user
-- ----------------------------
BEGIN;
INSERT INTO `t_user` VALUES (100000, 'admin', '', 'ats6sdxdqf8vsqjtz0utj461wr', '829b009a6b9cc8ea486a4abbc38e56529f3c6f4c9c6fcd3604b41b1d6eca1a57', NULL, '1', NOW(), NULL,NULL,0,NULL,NULL );
COMMIT;

-- ----------------------------
-- Table structure for t_user_role
-- ----------------------------
DROP TABLE IF EXISTS `t_user_role`;
CREATE TABLE `t_user_role` (
`id` bigint NOT NULL AUTO_INCREMENT,
`user_id` bigint DEFAULT NULL COMMENT '用户ID',
`role_id` bigint DEFAULT NULL COMMENT '角色ID',
PRIMARY KEY (`id`) USING BTREE,
UNIQUE KEY `UN_USER_ROLE_INX` (`user_id`,`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_user_role
-- ----------------------------
BEGIN;
INSERT INTO `t_user_role` VALUES (100000, 100000, 100000);
COMMIT;

-- ----------------------------
-- Table of t_app_build_pipe
-- ----------------------------
DROP TABLE IF EXISTS `t_app_build_pipe`;
CREATE TABLE `t_app_build_pipe`(
`app_id`          BIGINT AUTO_INCREMENT,
`pipe_type`       TINYINT,
`pipe_status`     TINYINT,
`cur_step`        SMALLINT,
`total_step`      SMALLINT,
`steps_status`    TEXT,
`steps_status_ts` TEXT,
`error`           TEXT,
`build_result`    TEXT,
`update_time`     DATETIME,
PRIMARY KEY (`app_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- Table of t_flink_cluster
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_cluster`;
CREATE TABLE `t_flink_cluster` (
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
   `resolve_order` int DEFAULT NULL,
   `exception` text COMMENT '异常信息',
   `cluster_state` tinyint DEFAULT '0' COMMENT '集群状态(0:创建未启动,1:已启动,2:停止)',
   `create_time` datetime DEFAULT NULL,
   PRIMARY KEY (`id`,`cluster_name`),
   UNIQUE KEY `ID` (`cluster_id`,`address`,`execution_mode`)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- ----------------------------
-- Table of t_access_token definition
-- ----------------------------
DROP TABLE IF EXISTS `t_access_token`;
CREATE TABLE `t_access_token` (
`id`            int NOT NULL AUTO_INCREMENT COMMENT 'key',
`user_id`       bigint,
`token`         varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TOKEN',
`expire_time`   datetime DEFAULT NULL COMMENT '过期时间',
`description`   varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '使用场景描述',
`status`        tinyint DEFAULT NULL COMMENT '1:enable,0:disable',
`create_time`   datetime DEFAULT NULL COMMENT 'create time',
`modify_time`   datetime DEFAULT NULL COMMENT 'modify time',
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;


-- ----------------------------
-- Table of t_alert_config
-- ----------------------------
DROP TABLE IF EXISTS `t_alert_config`;
CREATE TABLE `t_alert_config` (
  `id`                   bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id`              bigint DEFAULT NULL,
  `alert_name`           varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '报警组名称',
  `alert_type`           int DEFAULT 0 COMMENT '报警类型',
  `email_params`         varchar(255) COLLATE utf8mb4_general_ci COMMENT '邮件报警配置信息',
  `sms_params`           text COLLATE utf8mb4_general_ci COMMENT '短信报警配置信息',
  `ding_talk_params`     text COLLATE utf8mb4_general_ci COMMENT '钉钉报警配置信息',
  `we_com_params`        varchar(255) COLLATE utf8mb4_general_ci COMMENT '企微报警配置信息',
  `http_callback_params` text COLLATE utf8mb4_general_ci COMMENT '报警http回调配置信息',
  `lark_params`          text COLLATE utf8mb4_general_ci COMMENT '飞书报警配置信息',
  `create_time`          datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modify_time`          datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  INDEX `INX_ALERT_USER` (`user_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;


-- ----------------------------
-- Table of t_team
-- ----------------------------
DROP TABLE IF EXISTS `t_team`;
CREATE TABLE `t_team`
(
    `team_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `team_code`   varchar(255) NOT NULL COMMENT '团队标识 后续可以用于队列 资源隔离相关',
    `team_name`   varchar(255) NOT NULL COMMENT '团队名',
    `create_time` datetime     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`team_id`) USING BTREE,
    UNIQUE KEY `TEAM_CODE` (team_code) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- Records of t_team
-- ----------------------------
BEGIN;
insert into t_team values (1,'bigdata','BIGDATA','2022-02-21 18:00:00');
COMMIT;

-- ----------------------------
-- Table of t_team_user
-- ----------------------------
DROP TABLE IF EXISTS `t_team_user`;
CREATE TABLE `t_team_user`
(
    `team_id`    bigint   NOT NULL COMMENT 'teamId',
    `user_id`     bigint   NOT NULL COMMENT 'userId',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    UNIQUE KEY `GROUP_USER` (`team_id`,`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


