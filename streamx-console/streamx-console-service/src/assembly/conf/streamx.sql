SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_app_backup
-- ----------------------------
DROP TABLE IF EXISTS `t_app_backup`;
CREATE TABLE `t_app_backup` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `APP_ID` bigint(20) DEFAULT NULL,
  `SQL_ID` bigint(20) DEFAULT NULL,
  `CONFIG_ID` bigint(20) DEFAULT NULL,
  `VERSION` int(10) DEFAULT NULL,
  `PATH` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
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
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `APP_ID` bigint(20) DEFAULT NULL,
  `PROFILER` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TIMELINE` datetime DEFAULT NULL,
  `CONTENT` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `INX_TIME` (`TIMELINE`) USING BTREE,
  KEY `INX_APPID` (`APP_ID`) USING BTREE
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
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `JOB_TYPE` int(1) DEFAULT NULL,
  `EXECUTION_MODE` int(10) DEFAULT NULL,
  `PROJECT_ID` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `JOB_NAME` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `MODULE` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `JAR` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `MAIN_CLASS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ARGS` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `OPTIONS` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `USER_ID` int(25) DEFAULT NULL,
  `APP_ID` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `APP_TYPE` int(1) DEFAULT NULL,
  `DURATION` bigint(20) DEFAULT NULL,
  `JOB_ID` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `STATE` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DYNAMIC_OPTIONS` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `DESCRIPTION` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ALERT_EMAIL` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `RESOLVE_ORDER` smallint(1) DEFAULT NULL,
  `FLAME_GRAPH` tinyint(1) DEFAULT '0',
  `CP_THRESHOLD` int(10) DEFAULT NULL,
  `JM_MEMORY` int(10) DEFAULT NULL,
  `TM_MEMORY` int(255) DEFAULT NULL,
  `TOTAL_TASK` int(10) DEFAULT NULL,
  `TOTAL_TM` int(10) DEFAULT NULL,
  `TOTAL_SLOT` int(10) DEFAULT NULL,
  `AVAILABLE_SLOT` int(10) DEFAULT NULL,
  `OPTION_STATE` int(11) DEFAULT NULL,
  `TRACKING` int(1) DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  `DEPLOY` int(1) DEFAULT '0',
  `START_TIME` datetime DEFAULT NULL,
  `END_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `INX_STATE` (`STATE`) USING BTREE,
  KEY `INX_TRACK` (`TRACKING`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_app
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_flink_config
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_config`;
CREATE TABLE `t_flink_config` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `APP_ID` bigint(20) NOT NULL,
  `FORMAT` tinyint(1) NOT NULL DEFAULT '0',
  `VERSION` int(10) NOT NULL,
  `LATEST` tinyint(1) NOT NULL DEFAULT '0',
  `CONTENT` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
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
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `APP_ID` bigint(20) NOT NULL,
  `TARGET_TYPE` int(1) NOT NULL COMMENT '1) config 2) flink sql',
  `TARGET_ID` bigint(20) NOT NULL COMMENT 'configId or sqlId',
  `CREATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `UN_INX` (`APP_ID`,`TARGET_TYPE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_effective
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_flink_log
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_log`;
CREATE TABLE `t_flink_log` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `APP_ID` bigint(20) DEFAULT NULL,
  `YARN_APP_ID` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `SUCCESS` tinyint(1) DEFAULT NULL,
  `EXCEPTION` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
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
  `ID` bigint(64) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `URL` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `BRANCHES` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `USERNAME` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `PASSWORD` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `POM` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TYPE` int(1) DEFAULT NULL,
  `REPOSITORY` int(1) DEFAULT NULL,
  `DATE` datetime DEFAULT NULL,
  `LASTBUILD` datetime DEFAULT NULL,
  `DESCRIPTION` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `BUILDSTATE` int(1) DEFAULT '-1',
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_project
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_project` VALUES (1, 'streamx-quickstart', 'https://gitee.com/benjobs/streamx-quickstart.git', 'main', NULL, NULL, NULL, 1, 1, '2021-04-08 05:01:02', '2021-04-17 11:47:39', 'streamx-quickstart', 1);
COMMIT;

-- ----------------------------
-- Table structure for t_flink_savepoint
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_savepoint`;
CREATE TABLE `t_flink_savepoint` (
  `ID` bigint(50) NOT NULL AUTO_INCREMENT,
  `APP_ID` bigint(50) NOT NULL,
  `TYPE` int(1) DEFAULT NULL,
  `PATH` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `LATEST` tinyint(4) NOT NULL,
  `TRIGGER_TIME` datetime DEFAULT NULL,
  `CREATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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
  `ID` bigint(20) NOT NULL,
  `APP_ID` bigint(20) DEFAULT NULL,
  `SQL` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `DEPENDENCY` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `VERSION` int(20) DEFAULT NULL,
  `Candidate` int(1) NOT NULL DEFAULT '0',
  `CREATE_TIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_sql
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for t_flink_tutorial
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_tutorial`;
CREATE TABLE `t_flink_tutorial` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `TYPE` int(11) DEFAULT NULL,
  `NAME` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `CONTENT` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `CREATE_TIME` date DEFAULT NULL,
  PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_flink_tutorial
-- ----------------------------
BEGIN;
INSERT INTO `t_flink_tutorial` VALUES (1, 1, 'repl', '### Introduction\n\n[Apache Flink](https://flink.apache.org/) is a framework and distributed processing engine for stateful computations over unbounded and bounded data streams. This is Flink tutorial for running classical wordcount in both batch and streaming mode.\n\nThere\'re 3 things you need to do before using flink in StreamX Notebook.\n\n* Download [Flink 1.11](https://flink.apache.org/downloads.html) for scala 2.11 (Only scala-2.11 is supported, scala-2.12 is not supported yet in StreamX Notebook), unpack it and set `FLINK_HOME` in flink interpreter setting to this location.\n* Copy flink-python_2.11–1.11.1.jar from flink opt folder to flink lib folder (it is used by pyflink which is supported)\n* If you want to run yarn mode, you need to set `HADOOP_CONF_DIR` in flink interpreter setting. And make sure `hadoop` is in your `PATH`, because internally flink will call command `hadoop classpath` and put all the hadoop related jars in the classpath of flink interpreter process.\n\nThere\'re 6 sub interpreters in flink interpreter, each is used for different purpose. However they are in the the JVM and share the same ExecutionEnviroment/StremaExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment.\n\n* `flink`	- Creates ExecutionEnvironment/StreamExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment and provides a Scala environment\n* `pyflink`	- Provides a python environment\n* `ipyflink`	- Provides an ipython environment\n* `ssql`	 - Provides a stream sql environment\n* `bsql`	- Provides a batch sql environment\n', '2020-10-22');
COMMIT;

-- ----------------------------
-- Table structure for t_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_menu`;
CREATE TABLE `t_menu` (
  `MENU_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '菜单/按钮ID',
  `PARENT_ID` bigint(20) NOT NULL COMMENT '上级菜单ID',
  `MENU_NAME` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '菜单/按钮名称',
  `PATH` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '对应路由path',
  `COMPONENT` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '对应路由组件component',
  `PERMS` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '权限标识',
  `ICON` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '图标',
  `TYPE` char(2) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '类型 0菜单 1按钮',
  `DISPLAY` char(2) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '1' COMMENT '菜单是否显示',
  `ORDER_NUM` double(20,0) DEFAULT NULL COMMENT '排序',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `MODIFY_TIME` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`MENU_ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=211 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_menu
-- ----------------------------
BEGIN;
INSERT INTO `t_menu` VALUES (1, 0, 'System', '/system', 'PageView', NULL, 'appstore', '0', '1', 1, '2017-12-27 16:39:07', '2021-02-18 10:45:18');
INSERT INTO `t_menu` VALUES (3, 1, 'User Management', '/system/user', 'system/user/User', 'user:view', 'user', '0', '1', 1, '2017-12-27 16:47:13', '2021-02-09 19:01:36');
INSERT INTO `t_menu` VALUES (4, 1, 'Role Management', '/system/role', 'system/role/Role', 'role:view', 'smile', '0', '1', 2, '2017-12-27 16:48:09', '2021-02-09 19:01:41');
INSERT INTO `t_menu` VALUES (5, 1, 'Router Management', '/system/menu', 'system/menu/Menu', 'menu:view', 'profile', '0', '1', 3, '2017-12-27 16:48:57', '2021-02-09 19:01:47');
INSERT INTO `t_menu` VALUES (11, 3, 'add', '', '', 'user:add', NULL, '1', '1', NULL, '2017-12-27 17:02:58', '2020-10-11 10:38:35');
INSERT INTO `t_menu` VALUES (12, 3, 'update', '', '', 'user:update', NULL, '1', '1', NULL, '2017-12-27 17:04:07', '2020-10-11 10:38:29');
INSERT INTO `t_menu` VALUES (13, 3, 'delete', '', '', 'user:delete', NULL, '1', '1', NULL, '2017-12-27 17:04:58', '2020-10-11 10:38:22');
INSERT INTO `t_menu` VALUES (14, 4, 'add', '', '', 'role:add', NULL, '1', '1', NULL, '2017-12-27 17:06:38', '2020-10-11 10:37:23');
INSERT INTO `t_menu` VALUES (15, 4, 'update', '', '', 'role:update', NULL, '1', '1', NULL, '2017-12-27 17:06:38', '2020-10-11 10:37:32');
INSERT INTO `t_menu` VALUES (16, 4, 'delete', '', '', 'role:delete', NULL, '1', '1', NULL, '2017-12-27 17:06:38', '2020-10-11 10:37:42');
INSERT INTO `t_menu` VALUES (17, 5, 'add', '', '', 'menu:add', NULL, '1', '1', NULL, '2017-12-27 17:08:02', '2020-10-11 10:40:33');
INSERT INTO `t_menu` VALUES (18, 5, 'update', '', '', 'menu:update', NULL, '1', '1', NULL, '2017-12-27 17:08:02', '2020-10-11 10:40:28');
INSERT INTO `t_menu` VALUES (19, 5, 'delete', '', '', 'menu:delete', NULL, '1', '1', NULL, '2017-12-27 17:08:02', '2020-10-11 10:40:22');
INSERT INTO `t_menu` VALUES (130, 3, 'export', NULL, NULL, 'user:export', NULL, '1', '1', NULL, '2019-01-23 06:35:16', '2020-10-11 10:38:15');
INSERT INTO `t_menu` VALUES (131, 4, 'export', NULL, NULL, 'role:export', NULL, '1', '1', NULL, '2019-01-23 06:35:36', '2020-10-11 10:37:51');
INSERT INTO `t_menu` VALUES (132, 5, 'export', NULL, NULL, 'menu:export', NULL, '1', '1', NULL, '2019-01-23 06:36:05', '2020-10-11 10:40:16');
INSERT INTO `t_menu` VALUES (135, 3, 'reset', NULL, NULL, 'user:reset', NULL, '1', '1', NULL, '2019-01-23 06:37:00', '2020-10-11 10:38:07');
INSERT INTO `t_menu` VALUES (183, 0, 'StreamX', '/flink', 'PageView', NULL, 'dashboard', '0', '1', 2, '2019-12-10 10:06:54', '2021-02-18 10:45:26');
INSERT INTO `t_menu` VALUES (184, 183, 'Project', '/flink/project', 'flink/project/View', 'project:view', 'github', '0', '1', 1, '2019-12-10 10:08:30', '2020-09-27 18:33:41');
INSERT INTO `t_menu` VALUES (185, 183, 'Application', '/flink/app', 'flink/app/View', 'app:view', 'hdd', '0', '1', 2, '2019-12-10 10:10:26', '2020-09-27 18:33:47');
INSERT INTO `t_menu` VALUES (188, 183, 'Add Application', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', '0', NULL, '2019-12-11 11:54:24', '2020-10-12 08:05:02');
INSERT INTO `t_menu` VALUES (190, 183, 'Add Project', '/flink/project/add', 'flink/project/Add', 'project:create', '', '0', '0', NULL, '2020-08-04 07:36:00', '2020-10-12 08:04:19');
INSERT INTO `t_menu` VALUES (191, 183, 'App Detail', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', '0', NULL, '2020-08-20 09:56:47', '2020-10-12 08:04:26');
INSERT INTO `t_menu` VALUES (192, 183, 'Notebook', '/flink/notebook/view', 'flink/notebook/Submit', 'notebook:submit', 'read', '0', '1', 3, '2020-09-07 17:10:57', '2020-11-09 15:19:57');
INSERT INTO `t_menu` VALUES (193, 183, 'Edit Flink App', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', '0', NULL, '2020-09-10 16:03:49', '2020-10-12 08:04:51');
INSERT INTO `t_menu` VALUES (194, 183, 'Edit Streamx App', '/flink/app/edit_streamx', 'flink/app/EditStreamX', 'app:update', '', '0', '0', NULL, '2020-09-22 21:11:51', '2020-10-12 08:04:43');
INSERT INTO `t_menu` VALUES (195, 184, 'build', NULL, NULL, 'project:build', NULL, '1', '1', NULL, '2020-09-30 14:18:38', '2020-10-11 10:38:48');
INSERT INTO `t_menu` VALUES (196, 184, 'delete', NULL, NULL, 'project:delete', NULL, '1', '1', NULL, '2020-09-30 14:21:10', '2020-10-11 10:38:57');
INSERT INTO `t_menu` VALUES (197, 185, 'mapping', NULL, NULL, 'app:mapping', NULL, '1', '1', NULL, '2020-09-30 14:33:32', '2020-10-11 10:39:05');
INSERT INTO `t_menu` VALUES (198, 185, 'deploy', NULL, NULL, 'app:deploy', NULL, '1', '1', NULL, '2020-09-30 14:33:49', '2020-10-11 10:39:14');
INSERT INTO `t_menu` VALUES (199, 185, 'start', NULL, NULL, 'app:start', NULL, '1', '1', NULL, '2020-09-30 14:34:05', '2020-10-11 10:39:22');
INSERT INTO `t_menu` VALUES (200, 185, 'clean', NULL, NULL, 'app:clean', NULL, '1', '1', NULL, '2020-09-30 14:34:25', '2020-10-11 10:39:57');
INSERT INTO `t_menu` VALUES (201, 185, 'cancel', NULL, NULL, 'app:cancel', NULL, '1', '1', NULL, '2020-09-30 14:34:40', '2020-11-16 10:32:21');
INSERT INTO `t_menu` VALUES (202, 185, 'savepoint delete', NULL, NULL, 'savepoint:delete', NULL, '1', '1', NULL, '2020-10-17 14:49:29', NULL);
INSERT INTO `t_menu` VALUES (203, 185, 'backup rollback', NULL, NULL, 'backup:rollback', NULL, '1', '1', NULL, '2020-10-17 14:50:05', '2021-02-20 15:55:56');
INSERT INTO `t_menu` VALUES (204, 185, 'backup delete', NULL, NULL, 'backup:delete', NULL, '1', '1', NULL, '2020-10-20 13:49:58', NULL);
INSERT INTO `t_menu` VALUES (205, 185, 'conf delete', NULL, NULL, 'conf:delete', NULL, '1', '1', NULL, '2020-10-20 14:06:39', '2020-10-20 14:15:21');
INSERT INTO `t_menu` VALUES (206, 185, 'flame Graph', NULL, NULL, 'app:flameGraph', NULL, '1', '1', NULL, '2020-12-23 21:59:00', '2020-12-23 22:00:10');
INSERT INTO `t_menu` VALUES (207, 183, 'Setting', '/flink/setting', 'flink/setting/View', 'setting:view', 'setting', '0', '1', 4, '2021-02-08 22:54:41', '2021-02-09 09:32:09');
INSERT INTO `t_menu` VALUES (208, 207, 'Setting Update', NULL, NULL, 'setting:update', NULL, '1', '1', NULL, '2021-02-08 23:17:29', NULL);
INSERT INTO `t_menu` VALUES (209, 1, 'User Profile', '/user/profile', 'system/user/Profile', '', '', '0', '0', NULL, '2021-04-11 23:59:10', '2021-04-12 00:05:30');
COMMIT;

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role` (
  `ROLE_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `ROLE_NAME` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '角色名称',
  `REMARK` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '角色描述',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `MODIFY_TIME` datetime DEFAULT NULL COMMENT '修改时间',
  `ROLE_CODE` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '角色标识',
  PRIMARY KEY (`ROLE_ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=143 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_role
-- ----------------------------
BEGIN;
INSERT INTO `t_role` VALUES (1, 'admin', 'admin', '2017-12-27 16:23:11', '2021-04-22 09:35:44', 'admin');
INSERT INTO `t_role` VALUES (142, 'developer', '普通开发者', '2021-04-11 19:02:56', '2021-04-12 00:00:14', NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_role_menu`;
CREATE TABLE `t_role_menu` (
  `ROLE_ID` bigint(20) NOT NULL,
  `MENU_ID` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_role_menu
-- ----------------------------
BEGIN;
INSERT INTO `t_role_menu` VALUES (142, 197);
INSERT INTO `t_role_menu` VALUES (142, 198);
INSERT INTO `t_role_menu` VALUES (142, 199);
INSERT INTO `t_role_menu` VALUES (142, 200);
INSERT INTO `t_role_menu` VALUES (142, 185);
INSERT INTO `t_role_menu` VALUES (142, 201);
INSERT INTO `t_role_menu` VALUES (142, 202);
INSERT INTO `t_role_menu` VALUES (142, 203);
INSERT INTO `t_role_menu` VALUES (142, 205);
INSERT INTO `t_role_menu` VALUES (142, 206);
INSERT INTO `t_role_menu` VALUES (142, 204);
INSERT INTO `t_role_menu` VALUES (142, 188);
INSERT INTO `t_role_menu` VALUES (142, 191);
INSERT INTO `t_role_menu` VALUES (142, 192);
INSERT INTO `t_role_menu` VALUES (142, 193);
INSERT INTO `t_role_menu` VALUES (142, 194);
INSERT INTO `t_role_menu` VALUES (142, 183);
INSERT INTO `t_role_menu` VALUES (142, 209);
INSERT INTO `t_role_menu` VALUES (1, 1);
INSERT INTO `t_role_menu` VALUES (1, 3);
INSERT INTO `t_role_menu` VALUES (1, 11);
INSERT INTO `t_role_menu` VALUES (1, 12);
INSERT INTO `t_role_menu` VALUES (1, 17);
INSERT INTO `t_role_menu` VALUES (1, 18);
INSERT INTO `t_role_menu` VALUES (1, 19);
INSERT INTO `t_role_menu` VALUES (1, 132);
INSERT INTO `t_role_menu` VALUES (1, 135);
INSERT INTO `t_role_menu` VALUES (1, 5);
INSERT INTO `t_role_menu` VALUES (1, 4);
INSERT INTO `t_role_menu` VALUES (1, 14);
INSERT INTO `t_role_menu` VALUES (1, 15);
INSERT INTO `t_role_menu` VALUES (1, 16);
INSERT INTO `t_role_menu` VALUES (1, 131);
INSERT INTO `t_role_menu` VALUES (1, 13);
INSERT INTO `t_role_menu` VALUES (1, 130);
INSERT INTO `t_role_menu` VALUES (1, 183);
INSERT INTO `t_role_menu` VALUES (1, 184);
INSERT INTO `t_role_menu` VALUES (1, 191);
INSERT INTO `t_role_menu` VALUES (1, 192);
INSERT INTO `t_role_menu` VALUES (1, 193);
INSERT INTO `t_role_menu` VALUES (1, 194);
INSERT INTO `t_role_menu` VALUES (1, 195);
INSERT INTO `t_role_menu` VALUES (1, 196);
INSERT INTO `t_role_menu` VALUES (1, 197);
INSERT INTO `t_role_menu` VALUES (1, 200);
INSERT INTO `t_role_menu` VALUES (1, 201);
INSERT INTO `t_role_menu` VALUES (1, 185);
INSERT INTO `t_role_menu` VALUES (1, 198);
INSERT INTO `t_role_menu` VALUES (1, 199);
INSERT INTO `t_role_menu` VALUES (1, 188);
INSERT INTO `t_role_menu` VALUES (1, 190);
INSERT INTO `t_role_menu` VALUES (1, 202);
INSERT INTO `t_role_menu` VALUES (1, 203);
INSERT INTO `t_role_menu` VALUES (1, 204);
INSERT INTO `t_role_menu` VALUES (1, 205);
INSERT INTO `t_role_menu` VALUES (1, 206);
INSERT INTO `t_role_menu` VALUES (1, 207);
INSERT INTO `t_role_menu` VALUES (1, 208);
INSERT INTO `t_role_menu` VALUES (1, 209);
COMMIT;

-- ----------------------------
-- Table structure for t_setting
-- ----------------------------
DROP TABLE IF EXISTS `t_setting`;
CREATE TABLE `t_setting` (
  `NUM` int(10) DEFAULT NULL,
  `KEY` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `VALUE` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `TITLE` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `DESCRIPTION` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`KEY`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_setting
-- ----------------------------
BEGIN;
INSERT INTO `t_setting` VALUES (7, 'alert.email.address', NULL, 'Alert  Email Sender', '用来发送告警邮箱的mail');
INSERT INTO `t_setting` VALUES (5, 'alert.email.host', NULL, 'Alert Email Smtp Host', '告警邮箱Smtp Host');
INSERT INTO `t_setting` VALUES (8, 'alert.email.password', NULL, 'Alert Email Password', '发送告警的邮箱的密码');
INSERT INTO `t_setting` VALUES (6, 'alert.email.port', NULL, 'Alert Email Smtp Port', '告警邮箱的Smtp Port');
INSERT INTO `t_setting` VALUES (9, 'alert.email.ssl', NULL, 'Alert Email Is SSL', '发送告警的邮箱是否开启SSL');
INSERT INTO `t_setting` VALUES (1, 'env.flink.home', '/usr/local/flink-1.12.0', 'Flink Home', 'Flink Home');
INSERT INTO `t_setting` VALUES (2, 'maven.central.repository', '', 'Maven Central Repository', 'Maven 私服地址');
INSERT INTO `t_setting` VALUES (4, 'streamx.console.webapp.address', 'http://test-hadoop-2:10000', 'StreamX Webapp address', 'StreamX Console Web 应用程序 HTTP 端口');
INSERT INTO `t_setting` VALUES (3, 'streamx.console.workspace', '/streamx/workspace', 'StreamX Console Workspace', 'StreamX Console 的工作空间,用于存放项目源码,编译后的项目等');
COMMIT;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `USER_ID` bigint(10) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `USERNAME` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '登录用户名',
  `NICK_NAME` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '昵称',
  `SALT` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '密码加盐',
  `PASSWORD` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '密码',
  `DEPT_ID` bigint(20) DEFAULT NULL COMMENT '部门ID',
  `EMAIL` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '邮箱',
  `MOBILE` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '联系电话',
  `STATUS` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '状态 0锁定 1有效',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `MODIFY_TIME` datetime DEFAULT NULL COMMENT '修改时间',
  `LAST_LOGIN_TIME` datetime DEFAULT NULL COMMENT '最近访问时间',
  `SEX` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '性别 0男 1女 2保密',
  `DESCRIPTION` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '描述',
  `AVATAR` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '用户头像',
  `USER_TYPE` char(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '2' COMMENT '用户类型 1内部用户 2外部用户',
  PRIMARY KEY (`USER_ID`) USING BTREE,
  UNIQUE KEY `UN_USERNAME` (`NICK_NAME`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_user
-- ----------------------------
BEGIN;
INSERT INTO `t_user` VALUES (1, 'admin', '', 'ats6sdxdqf8vsqjtz0utj461wr', '829b009a6b9cc8ea486a4abbc38e56529f3c6f4c9c6fcd3604b41b1d6eca1a57', 1, 'benjobs@qq.com', '18500193260', '1', '2017-12-27 15:47:19', '2019-08-09 15:42:57', '2021-04-22 09:37:13', '0', 'author。', 'ubnKSIfAJTxIgXOKlciN.png', '1');
COMMIT;

-- ----------------------------
-- Table structure for t_user_role
-- ----------------------------
DROP TABLE IF EXISTS `t_user_role`;
CREATE TABLE `t_user_role` (
  `USER_ID` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `ROLE_ID` bigint(20) DEFAULT NULL COMMENT '角色ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_user_role
-- ----------------------------
BEGIN;
INSERT INTO `t_user_role` VALUES (1, 1);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
