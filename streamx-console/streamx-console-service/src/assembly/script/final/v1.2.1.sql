
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
`DEPLOY` tinyint DEFAULT '0',
`START_TIME` datetime DEFAULT NULL,
`END_TIME` datetime DEFAULT NULL,
`ALERT_EMAIL` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`K8S_POD_TEMPLATE` text COLLATE utf8mb4_general_ci,
`K8S_JM_POD_TEMPLATE` text COLLATE utf8mb4_general_ci,
`K8S_TM_POD_TEMPLATE` text COLLATE utf8mb4_general_ci,
`K8S_HADOOP_INTEGRATION` tinyint(1) DEFAULT '0',
PRIMARY KEY (`ID`) USING BTREE,
KEY `INX_STATE` (`STATE`) USING BTREE,
KEY `INX_JOB_TYPE` (`JOB_TYPE`) USING BTREE,
KEY `INX_TRACK` (`TRACKING`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1401710007170375682 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_app
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_app` VALUES (1401710007170375681, 2, 4, NULL, NULL, 'Flink SQL Demo', NULL, NULL, NULL, NULL, NULL, '{\"jobmanager.memory.process.size\":\"1024mb\",\"taskmanager.memory.process.size\":\"1024mb\",\"parallelism.default\":1,\"taskmanager.numberOfTaskSlots\":1}', 1, NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, '2', 0, NULL, NULL, NULL, NULL, NULL, NULL, 'Flink SQL Demo', 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, NOW(), 0, NULL, NULL, NULL, NULL, NULL, NULL, 0);
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
) ENGINE=InnoDB AUTO_INCREMENT=1401710007468171266 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_effective
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_effective` VALUES (1401710007468171265, 1401710007170375681, 2, 1401710007208124417, NOW());
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
`START_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
`USERNAME` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`PASSWORD` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`POM` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`TYPE` tinyint DEFAULT NULL,
`REPOSITORY` tinyint DEFAULT NULL,
`DATE` datetime DEFAULT NULL,
`LASTBUILD` datetime DEFAULT NULL,
`DESCRIPTION` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`BUILDSTATE` tinyint DEFAULT '-1',
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_project
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_project` VALUES (1, 'streamx-quickstart', 'https://gitee.com/streamxhub/streamx-quickstart.git', 'main', NULL, NULL, NULL, 1, 1, NOW(), NULL, 'streamx-quickstart', 1);
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
`ID` bigint NOT NULL,
`APP_ID` bigint DEFAULT NULL,
`SQL` text COLLATE utf8mb4_general_ci,
`DEPENDENCY` text COLLATE utf8mb4_general_ci,
`VERSION` int DEFAULT NULL,
`CANDIDATE` tinyint NOT NULL DEFAULT '0',
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_sql
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_sql` VALUES (1401710007208124417, 1401710007170375681, 'eNqlUUtPhDAQvu+vmFs1AYIHT5s94AaVqGxSSPZIKgxrY2mxrdGfb4GS3c0+LnJo6Mz36syapkmZQpk8vKbQMMt2KOFmAe5rK4Nf3yhrhCwvA1/TTDaqO61UxmooSprlT1PDGkgKEKpmwvIOjWVdP3W2zpG+JfQFHjfU46xxrVvYZuWztye1khJrqzSBFRCfjUwSYQiqt1xJJvyPcbWJp9WPCXvUoUEn0ZAVufcs0nIUjYn2L4s++YiY75eBLr+2Dnl3GYKTWRyfQKYRRR2XZxXmNvu9yh9GHAmUO/sxyMRkGNly4c714RZ7zaWtLHsX+N9NjvVrWxm99jmyvEhpOUhujmIYFI5zkCOYzYIj11a7QH7Tyz+nE8bw', NULL, 1, 0, NOW());
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_tutorial
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_tutorial` VALUES (1, 1, 'repl', '### Introduction\n\n[Apache Flink](https://flink.apache.org/) is a framework and distributed processing engine for stateful computations over unbounded and bounded data streams. This is Flink tutorial for running classical wordcount in both batch and streaming mode.\n\nThere\'re 3 things you need to do before using flink in StreamX Notebook.\n\n* Download [Flink 1.11](https://flink.apache.org/downloads.html) for scala 2.11 (Only scala-2.11 is supported, scala-2.12 is not supported yet in StreamX Notebook), unpack it and set `FLINK_HOME` in flink interpreter setting to this location.\n* Copy flink-python_2.11–1.11.1.jar from flink opt folder to flink lib folder (it is used by pyflink which is supported)\n* If you want to run yarn mode, you need to set `HADOOP_CONF_DIR` in flink interpreter setting. And make sure `hadoop` is in your `PATH`, because internally flink will call command `hadoop classpath` and put all the hadoop related jars in the classpath of flink interpreter process.\n\nThere\'re 6 sub interpreters in flink interpreter, each is used for different purpose. However they are in the the JVM and share the same ExecutionEnviroment/StremaExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment.\n\n* `flink`	- Creates ExecutionEnvironment/StreamExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment and provides a Scala environment\n* `pyflink`	- Provides a python environment\n* `ipyflink`	- Provides an ipython environment\n* `ssql`	 - Provides a stream sql environment\n* `bsql`	- Provides a batch sql environment\n', NOW());
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
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_menu
-- ----------------------------
BEGIN;
INSERT INTO `t_menu` VALUES (1, 0, 'System', '/system', 'PageView', NULL, 'desktop', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (2, 1, 'User Management', '/system/user', 'system/user/User', 'user:view', 'user', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (3, 1, 'Role Management', '/system/role', 'system/role/Role', 'role:view', 'smile', '0', '1', 2, NOW(), NULL);
INSERT INTO `t_menu` VALUES (4, 1, 'Router Management', '/system/menu', 'system/menu/Menu', 'menu:view', 'bars', '0', '1', 3, NOW(), NULL);
INSERT INTO `t_menu` VALUES (5, 2, 'add', NULL, NULL, 'user:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (6, 2, 'update', NULL, NULL, 'user:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (7, 2, 'delete', NULL, NULL, 'user:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (8, 3, 'add', NULL, NULL, 'role:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (9, 3, 'update', NULL, NULL, 'role:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (10, 3, 'delete', NULL, NULL, 'role:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (11, 4, 'add', NULL, NULL, 'menu:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (12, 4, 'update', NULL, NULL, 'menu:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (13, 2, 'reset', NULL, NULL, 'user:reset', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (14, 0, 'StreamX', '/flink', 'PageView', NULL, 'build', '0', '1', 2, NOW(), NULL);
INSERT INTO `t_menu` VALUES (15, 14, 'Project', '/flink/project', 'flink/project/View', 'project:view', 'github', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (16, 14, 'Application', '/flink/app', 'flink/app/View', 'app:view', 'mobile', '0', '1', 2, NOW(), NULL);
INSERT INTO `t_menu` VALUES (17, 14, 'Add Application', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (18, 14, 'Add Project', '/flink/project/add', 'flink/project/Add', 'project:create', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (19, 14, 'App Detail', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (20, 14, 'Notebook', '/flink/notebook/view', 'flink/notebook/Submit', 'notebook:submit', 'read', '0', '1', 3, NOW(), NULL);
INSERT INTO `t_menu` VALUES (21, 14, 'Edit Flink App', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (22, 14, 'Edit StreamX App', '/flink/app/edit_streamx', 'flink/app/EditStreamX', 'app:update', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (23, 15, 'build', NULL, NULL, 'project:build', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (24, 15, 'delete', NULL, NULL, 'project:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (25, 16, 'mapping', NULL, NULL, 'app:mapping', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (26, 16, 'deploy', NULL, NULL, 'app:deploy', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (27, 16, 'start', NULL, NULL, 'app:start', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (28, 16, 'clean', NULL, NULL, 'app:clean', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (29, 16, 'cancel', NULL, NULL, 'app:cancel', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (30, 16, 'savepoint delete', NULL, NULL, 'savepoint:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (31, 16, 'backup rollback', NULL, NULL, 'backup:rollback', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (32, 16, 'backup delete', NULL, NULL, 'backup:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (33, 16, 'conf delete', NULL, NULL, 'conf:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (34, 16, 'flame Graph', NULL, NULL, 'app:flameGraph', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (35, 14, 'Setting', '/flink/setting', 'flink/setting/View', 'setting:view', 'setting', '0', '1', 4, NOW(), NULL);
INSERT INTO `t_menu` VALUES (36, 35, 'Setting Update', NULL, NULL, 'setting:update', NULL, '1', '1', NULL, NOW(), NULL);
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_role
-- ----------------------------
BEGIN;
INSERT INTO `t_role` VALUES (1, 'admin', 'admin', NOW(), NULL, NULL);
INSERT INTO `t_role` VALUES (2, 'developer', 'developer', NOW(), NULL, NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_role_menu
-- ----------------------------
BEGIN;
INSERT INTO `t_role_menu` VALUES (1, 1, 1);
INSERT INTO `t_role_menu` VALUES (2, 1, 2);
INSERT INTO `t_role_menu` VALUES (3, 1, 3);
INSERT INTO `t_role_menu` VALUES (4, 1, 4);
INSERT INTO `t_role_menu` VALUES (5, 1, 5);
INSERT INTO `t_role_menu` VALUES (6, 1, 6);
INSERT INTO `t_role_menu` VALUES (7, 1, 7);
INSERT INTO `t_role_menu` VALUES (8, 1, 8);
INSERT INTO `t_role_menu` VALUES (9, 1, 9);
INSERT INTO `t_role_menu` VALUES (10, 1, 10);
INSERT INTO `t_role_menu` VALUES (11, 1, 11);
INSERT INTO `t_role_menu` VALUES (12, 1, 12);
INSERT INTO `t_role_menu` VALUES (13, 1, 13);
INSERT INTO `t_role_menu` VALUES (14, 1, 14);
INSERT INTO `t_role_menu` VALUES (15, 1, 15);
INSERT INTO `t_role_menu` VALUES (16, 1, 16);
INSERT INTO `t_role_menu` VALUES (17, 1, 17);
INSERT INTO `t_role_menu` VALUES (18, 1, 18);
INSERT INTO `t_role_menu` VALUES (19, 1, 19);
INSERT INTO `t_role_menu` VALUES (20, 1, 20);
INSERT INTO `t_role_menu` VALUES (21, 1, 21);
INSERT INTO `t_role_menu` VALUES (22, 1, 22);
INSERT INTO `t_role_menu` VALUES (23, 1, 23);
INSERT INTO `t_role_menu` VALUES (24, 1, 24);
INSERT INTO `t_role_menu` VALUES (25, 1, 25);
INSERT INTO `t_role_menu` VALUES (26, 1, 26);
INSERT INTO `t_role_menu` VALUES (27, 1, 27);
INSERT INTO `t_role_menu` VALUES (28, 1, 28);
INSERT INTO `t_role_menu` VALUES (29, 1, 29);
INSERT INTO `t_role_menu` VALUES (30, 1, 30);
INSERT INTO `t_role_menu` VALUES (31, 1, 31);
INSERT INTO `t_role_menu` VALUES (32, 1, 32);
INSERT INTO `t_role_menu` VALUES (33, 1, 33);
INSERT INTO `t_role_menu` VALUES (34, 1, 34);
INSERT INTO `t_role_menu` VALUES (35, 1, 35);
INSERT INTO `t_role_menu` VALUES (36, 1, 36);
INSERT INTO `t_role_menu` VALUES (37, 2, 16);
INSERT INTO `t_role_menu` VALUES (38, 2, 17);
INSERT INTO `t_role_menu` VALUES (39, 2, 18);
INSERT INTO `t_role_menu` VALUES (40, 2, 19);
INSERT INTO `t_role_menu` VALUES (41, 2, 20);
INSERT INTO `t_role_menu` VALUES (42, 2, 21);
INSERT INTO `t_role_menu` VALUES (43, 2, 22);
INSERT INTO `t_role_menu` VALUES (44, 2, 25);
INSERT INTO `t_role_menu` VALUES (45, 2, 26);
INSERT INTO `t_role_menu` VALUES (46, 2, 27);
INSERT INTO `t_role_menu` VALUES (47, 2, 28);
INSERT INTO `t_role_menu` VALUES (48, 2, 29);
INSERT INTO `t_role_menu` VALUES (49, 2, 30);
INSERT INTO `t_role_menu` VALUES (50, 2, 31);
INSERT INTO `t_role_menu` VALUES (51, 2, 32);
INSERT INTO `t_role_menu` VALUES (52, 2, 33);
INSERT INTO `t_role_menu` VALUES (53, 2, 34);
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
INSERT INTO `t_setting` VALUES (5, 'alert.email.from', NULL, 'Alert  Email From', '发送告警的邮箱', 1);
INSERT INTO `t_setting` VALUES (3, 'alert.email.host', NULL, 'Alert Email Smtp Host', '告警邮箱Smtp Host', 1);
INSERT INTO `t_setting` VALUES (7, 'alert.email.password', NULL, 'Alert Email Password', '用来发送告警邮箱的认证密码', 1);
INSERT INTO `t_setting` VALUES (4, 'alert.email.port', NULL, 'Alert Email Smtp Port', '告警邮箱的Smtp Port', 1);
INSERT INTO `t_setting` VALUES (8, 'alert.email.ssl', 'false', 'Alert Email Is SSL', '发送告警的邮箱是否开启SSL', 2);
INSERT INTO `t_setting` VALUES (6, 'alert.email.userName', NULL, 'Alert  Email User', '用来发送告警邮箱的认证用户名', 1);
INSERT INTO `t_setting` VALUES (9, 'docker.register.address', NULL, 'Docker Register Address', 'Docker容器服务地址', 1);
INSERT INTO `t_setting` VALUES (11, 'docker.register.password', NULL, 'Docker Register Password', 'Docker容器服务认证密码', 1);
INSERT INTO `t_setting` VALUES (10, 'docker.register.user', NULL, 'Docker Register User', 'Docker容器服务认证用户名', 1);
INSERT INTO `t_setting` VALUES (1, 'maven.central.repository', NULL, 'Maven Central Repository', 'Maven 私服地址', 1);
INSERT INTO `t_setting` VALUES (2, 'streamx.console.webapp.address', NULL, 'StreamX Webapp address', 'StreamX Console Web 应用程序HTTP URL', 1);
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
-- ----------------------------
-- Records of t_user
-- ----------------------------
BEGIN;
INSERT INTO `t_user` VALUES (1, 'admin', '', 'ats6sdxdqf8vsqjtz0utj461wr', '829b009a6b9cc8ea486a4abbc38e56529f3c6f4c9c6fcd3604b41b1d6eca1a57', 'benjobs@qq.com', '13800000000', '1', NOW(), NULL,NULL,NULL,NULL,NULL );
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_user_role
-- ----------------------------
BEGIN;
INSERT INTO `t_user_role` VALUES (1, 1, 1);
COMMIT;

-- ----------------------------
-- Table of t_app_build_pipe
-- ----------------------------
DROP TABLE IF EXISTS `t_app_build_pipe`;
CREATE TABLE `t_app_build_pipe`
(
    `APP_ID`          BIGINT PRIMARY KEY,
    `PIPE_TYPE`       TINYINT,
    `PIPE_STATUS`     TINYINT,
    `CUR_STEP`        SMALLINT,
    `TOTAL_STEP`      SMALLINT,
    `STEPS_STATUS`    TEXT,
    `STEPS_STATUS_TS` TEXT,
    `ERROR`           TEXT,
    `BUILD_RESULT`    TEXT,
    `UPDATE_TIME`     DATETIME
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;


SET FOREIGN_KEY_CHECKS = 1;
