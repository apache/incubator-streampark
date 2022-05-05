
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_app_backup
-- ----------------------------
DROP TABLE IF EXISTS `t_app_backup`;
CREATE TABLE `t_app_backup` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint DEFAULT NULL,
`SQL_ID` bigint DEFAULT NULL,
`CONFIG_ID` bigint DEFAULT NULL,
`VERSION` int DEFAULT NULL,
`PATH` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`DESCRIPTION` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
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
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint DEFAULT NULL,
`PROFILER` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`TIMELINE` datetime DEFAULT NULL,
`CONTENT` text COLLATE utf8mb4_general_ci,
PRIMARY KEY (`ID`) USING BTREE,
KEY `INX_TIME` (`TIMELINE`),
KEY `INX_APPID` (`APP_ID`)
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
`ID` bigint NOT NULL AUTO_INCREMENT,
`JOB_TYPE` tinyint DEFAULT NULL,
`EXECUTION_MODE` tinyint DEFAULT NULL,
`RESOURCE_FROM` tinyint(1) DEFAULT NULL,
`PROJECT_ID` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
`JOB_NAME` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`MODULE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`JAR` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`JAR_CHECK_SUM` bigint DEFAULT NULL,
`MAIN_CLASS` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`ARGS` text COLLATE utf8mb4_general_ci,
`OPTIONS` text COLLATE utf8mb4_general_ci,
`HOT_PARAMS` text COLLATE utf8mb4_general_ci,
`USER_ID` bigint DEFAULT NULL,
`APP_ID` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`APP_TYPE` tinyint DEFAULT NULL,
`DURATION` bigint DEFAULT NULL,
`JOB_ID` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
`VERSION_ID` bigint DEFAULT NULL,
`CLUSTER_ID` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`K8S_NAMESPACE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`FLINK_IMAGE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`STATE` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
`RESTART_SIZE` int DEFAULT NULL,
`RESTART_COUNT` int DEFAULT NULL,
`CP_THRESHOLD` int DEFAULT NULL,
`CP_MAX_FAILURE_INTERVAL` int DEFAULT NULL,
`CP_FAILURE_RATE_INTERVAL` int DEFAULT NULL,
`CP_FAILURE_ACTION` tinyint DEFAULT NULL,
`DYNAMIC_OPTIONS` text COLLATE utf8mb4_general_ci,
`DESCRIPTION` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`RESOLVE_ORDER` tinyint DEFAULT NULL,
`K8S_REST_EXPOSED_TYPE` tinyint DEFAULT NULL,
`FLAME_GRAPH` tinyint DEFAULT '0',
`JM_MEMORY` int DEFAULT NULL,
`TM_MEMORY` int DEFAULT NULL,
`TOTAL_TASK` int DEFAULT NULL,
`TOTAL_TM` int DEFAULT NULL,
`TOTAL_SLOT` int DEFAULT NULL,
`AVAILABLE_SLOT` int DEFAULT NULL,
`OPTION_STATE` tinyint DEFAULT NULL,
`TRACKING` tinyint DEFAULT NULL,
`CREATE_TIME` datetime DEFAULT NULL,
`LAUNCH` tinyint DEFAULT '1',
`BUILD` tinyint DEFAULT '1',
`START_TIME` datetime DEFAULT NULL,
`END_TIME` datetime DEFAULT NULL,
`ALERT_EMAIL` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`K8S_POD_TEMPLATE` text COLLATE utf8mb4_general_ci,
`K8S_JM_POD_TEMPLATE` text COLLATE utf8mb4_general_ci,
`K8S_TM_POD_TEMPLATE` text COLLATE utf8mb4_general_ci,
`K8S_HADOOP_INTEGRATION` tinyint(1) DEFAULT '0',
`FLINK_CLUSTER_ID` bigint DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE,
KEY `INX_STATE` (`STATE`) USING BTREE,
KEY `INX_JOB_TYPE` (`JOB_TYPE`) USING BTREE,
KEY `INX_TRACK` (`TRACKING`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_app
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_app` VALUES (100000, 2, 4, NULL, NULL, 'Flink SQL Demo', NULL, NULL, NULL, NULL, NULL, NULL , NULL, 100000, NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, 'Flink SQL Demo', 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, NOW(), 1, 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_flink_config
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_config`;
CREATE TABLE `t_flink_config` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint NOT NULL,
`FORMAT` tinyint NOT NULL DEFAULT '0',
`VERSION` int NOT NULL,
`LATEST` tinyint NOT NULL DEFAULT '0',
`CONTENT` text COLLATE utf8mb4_general_ci NOT NULL,
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
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
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint NOT NULL,
`TARGET_TYPE` tinyint NOT NULL COMMENT '1) config 2) flink sql',
`TARGET_ID` bigint NOT NULL COMMENT 'configId or sqlId',
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE,
UNIQUE KEY `UN_INX` (`APP_ID`,`TARGET_TYPE`) USING BTREE
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
`ID` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
`FLINK_NAME` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Flink实例名称',
`FLINK_HOME` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Flink Home路径',
`VERSION` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Flink对应的版本号',
`SCALA_VERSION` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Flink对应的scala版本号',
`FLINK_CONF` text COLLATE utf8mb4_general_ci NOT NULL COMMENT 'flink-conf配置内容',
`IS_DEFAULT` tinyint NOT NULL DEFAULT '0' COMMENT '是否为默认版本',
`DESCRIPTION` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述信息',
`CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
PRIMARY KEY (`ID`) USING BTREE,
UNIQUE KEY `UN_NAME` (`FLINK_NAME`) USING BTREE
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
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint DEFAULT NULL,
`YARN_APP_ID` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
`SUCCESS` tinyint DEFAULT NULL,
`EXCEPTION` text COLLATE utf8mb4_general_ci,
`OPTION_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
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
`ID` bigint NOT NULL AUTO_INCREMENT,
`NAME` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`URL` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
`BRANCHES` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
`USER_NAME` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`PASSWORD` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`POM` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`BUILD_ARGS` varchar(255) DEFAULT NULL,
`TYPE` tinyint DEFAULT NULL,
`REPOSITORY` tinyint DEFAULT NULL,
`DATE` datetime DEFAULT NULL,
`LAST_BUILD` datetime DEFAULT NULL,
`DESCRIPTION` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`BUILD_STATE` tinyint DEFAULT '-1',
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_project
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_project` VALUES (100000, 'streamx-quickstart', 'https://github.com/streamxhub/streamx-quickstart.git', 'main', NULL, NULL, NULL, NULL, 1, 1, NOW(), NULL, 'streamx-quickstart', 1);
COMMIT;

-- ----------------------------
-- Table structure for t_flink_savepoint
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_savepoint`;
CREATE TABLE `t_flink_savepoint` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint NOT NULL,
`TYPE` tinyint DEFAULT NULL,
`PATH` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`LATEST` tinyint NOT NULL,
`TRIGGER_TIME` datetime DEFAULT NULL,
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
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
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint DEFAULT NULL,
`SQL` text COLLATE utf8mb4_general_ci,
`DEPENDENCY` text COLLATE utf8mb4_general_ci,
`VERSION` int DEFAULT NULL,
`CANDIDATE` tinyint NOT NULL DEFAULT '1',
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
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
`ID` int NOT NULL AUTO_INCREMENT,
`TYPE` tinyint DEFAULT NULL,
`NAME` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`CONTENT` text COLLATE utf8mb4_general_ci,
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
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
`MENU_ID` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单/按钮ID',
`PARENT_ID` bigint NOT NULL COMMENT '上级菜单ID',
`MENU_NAME` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单/按钮名称',
`PATH` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对应路由path',
`COMPONENT` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '对应路由组件component',
`PERMS` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限标识',
`ICON` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
`TYPE` char(2) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型 0菜单 1按钮',
`DISPLAY` char(2) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '菜单是否显示',
`ORDER_NUM` double(20,0) DEFAULT NULL COMMENT '排序',
`CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
`MODIFY_TIME` datetime DEFAULT NULL COMMENT '修改时间',
PRIMARY KEY (`MENU_ID`) USING BTREE
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
COMMIT;

-- ----------------------------
-- Table structure for t_message
-- ----------------------------
DROP TABLE IF EXISTS `t_message`;
CREATE TABLE `t_message` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint DEFAULT NULL,
`USER_ID` bigint DEFAULT NULL,
`TYPE` tinyint DEFAULT NULL,
`TITLE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`CONTEXT` text COLLATE utf8mb4_general_ci,
`READED` tinyint DEFAULT '0',
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE,
KEY `INX_USER_ID` (`USER_ID`) USING BTREE
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
`ROLE_ID` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
`ROLE_NAME` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
`REMARK` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色描述',
`CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
`MODIFY_TIME` datetime DEFAULT NULL COMMENT '修改时间',
`ROLE_CODE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色标识',
PRIMARY KEY (`ROLE_ID`) USING BTREE
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
`ID` bigint NOT NULL AUTO_INCREMENT,
`ROLE_ID` bigint NOT NULL,
`MENU_ID` bigint NOT NULL,
PRIMARY KEY (`ID`) USING BTREE,
UNIQUE KEY `UN_INX` (`ROLE_ID`,`MENU_ID`) USING BTREE
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
COMMIT;

-- ----------------------------
-- Table structure for t_setting
-- ----------------------------
DROP TABLE IF EXISTS `t_setting`;
CREATE TABLE `t_setting` (
`NUM` int DEFAULT NULL,
`KEY` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
`VALUE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`TITLE` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`DESCRIPTION` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`TYPE` tinyint NOT NULL COMMENT '1: input 2: boolean 3: number',
PRIMARY KEY (`KEY`) USING BTREE
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
COMMIT;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
`USER_ID` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
`USERNAME` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录用户名',
`NICK_NAME` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '昵称',
`SALT` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码加盐',
`PASSWORD` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
`EMAIL` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
`MOBILE` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
`STATUS` char(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态 0锁定 1有效',
`CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
`MODIFY_TIME` datetime DEFAULT NULL COMMENT '修改时间',
`LAST_LOGIN_TIME` datetime DEFAULT NULL COMMENT '最近访问时间',
`SEX` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '性别 0男 1女 2保密',
`AVATAR` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户头像',
`DESCRIPTION` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
PRIMARY KEY (`USER_ID`) USING BTREE,
UNIQUE KEY `UN_USERNAME` (`NICK_NAME`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
-- ----------------------------
-- Records of t_user
-- ----------------------------
BEGIN;
INSERT INTO `t_user` VALUES (100000, 'admin', '', 'ats6sdxdqf8vsqjtz0utj461wr', '829b009a6b9cc8ea486a4abbc38e56529f3c6f4c9c6fcd3604b41b1d6eca1a57', 'benjobs@qq.com', '13800000000', '1', NOW(), NULL,NULL,NULL,NULL,NULL );
COMMIT;

-- ----------------------------
-- Table structure for t_user_role
-- ----------------------------
DROP TABLE IF EXISTS `t_user_role`;
CREATE TABLE `t_user_role` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`USER_ID` bigint DEFAULT NULL COMMENT '用户ID',
`ROLE_ID` bigint DEFAULT NULL COMMENT '角色ID',
PRIMARY KEY (`ID`) USING BTREE,
UNIQUE KEY `UN_INX` (`USER_ID`,`ROLE_ID`) USING BTREE
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
`APP_ID`          BIGINT AUTO_INCREMENT,
`PIPE_TYPE`       TINYINT,
`PIPE_STATUS`     TINYINT,
`CUR_STEP`        SMALLINT,
`TOTAL_STEP`      SMALLINT,
`STEPS_STATUS`    TEXT,
`STEPS_STATUS_TS` TEXT,
`ERROR`           TEXT,
`BUILD_RESULT`    TEXT,
`UPDATE_TIME`     DATETIME,
PRIMARY KEY (`APP_ID`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- Table of t_flink_cluster
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_cluster`;
CREATE TABLE `t_flink_cluster` (
   `ID` bigint(20) NOT NULL AUTO_INCREMENT,
   `ADDRESS` varchar(255) DEFAULT NULL COMMENT 'jobmanager的url地址',
   `CLUSTER_ID` varchar(255) DEFAULT NULL COMMENT 'session模式的clusterId(yarn-session:application-id,k8s-session:cluster-id)',
   `CLUSTER_NAME` varchar(255) NOT NULL COMMENT '集群名称',
   `OPTIONS` text COMMENT '参数集合json形式',
   `YARN_QUEUE` varchar(100) DEFAULT NULL COMMENT '任务所在yarn队列',
   `EXECUTION_MODE` tinyint(4) NOT NULL DEFAULT '1' COMMENT 'session类型(1:remote,3:yarn-session,5:kubernetes-session)',
   `VERSION_ID` bigint(20) NOT NULL COMMENT 'flink对应id',
   `K8S_NAMESPACE` varchar(255) DEFAULT 'default' COMMENT 'k8s namespace',
   `SERVICE_ACCOUNT` varchar(50) DEFAULT NULL COMMENT 'k8s service account',
   `DESCRIPTION` varchar(255) DEFAULT NULL,
   `USER_ID` bigint(20) DEFAULT NULL,
   `FLINK_IMAGE` varchar(255) DEFAULT NULL COMMENT 'flink使用镜像',
   `DYNAMIC_OPTIONS` text COMMENT '动态参数',
   `K8S_REST_EXPOSED_TYPE` tinyint(4) DEFAULT '2' COMMENT 'k8s 暴露类型(0:LoadBalancer,1:ClusterIP,2:NodePort)',
   `K8S_HADOOP_INTEGRATION` tinyint(4) DEFAULT '0',
   `FLAME_GRAPH` tinyint(4) DEFAULT '0' COMMENT '是否开启火焰图，默认不开启',
   `K8S_CONF` varchar(255) DEFAULT NULL COMMENT 'k8s配置文件所在路径',
   `RESOLVE_ORDER` int(11) DEFAULT NULL,
   `EXCEPTION` text COMMENT '异常信息',
   `CLUSTER_STATE` tinyint(4) DEFAULT '0' COMMENT '集群状态(0:创建未启动,1:已启动,2:停止)',
   `CREATE_TIME` datetime DEFAULT NULL,
   PRIMARY KEY (`ID`,`CLUSTER_NAME`),
   UNIQUE KEY `ID` (`CLUSTER_ID`,`ADDRESS`,`EXECUTION_MODE`)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- ----------------------------
-- Table of t_access_token definition
-- ----------------------------
DROP TABLE IF EXISTS `t_access_token`;
CREATE TABLE `t_access_token` (
`ID`            int NOT NULL AUTO_INCREMENT COMMENT 'key',
`USER_ID`       bigint,
`TOKEN`         varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TOKEN',
`EXPIRE_TIME`   datetime DEFAULT NULL COMMENT '过期时间',
`DESCRIPTION`   varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '使用场景描述',
`STATUS`        tinyint DEFAULT NULL COMMENT '1:enable,0:disable',
`CREATE_TIME`   datetime DEFAULT NULL COMMENT 'create time',
`MODIFY_TIME`   datetime DEFAULT NULL COMMENT 'modify time',
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;
