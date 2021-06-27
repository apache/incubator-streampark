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
`PATH` varchar(255) DEFAULT NULL,
`DESCRIPTION` varchar(255) DEFAULT NULL,
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_flame_graph
-- ----------------------------
DROP TABLE IF EXISTS `t_flame_graph`;
CREATE TABLE `t_flame_graph` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint DEFAULT NULL,
`PROFILER` varchar(255) DEFAULT NULL,
`TIMELINE` datetime DEFAULT NULL,
`CONTENT` text DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE,
KEY `INX_TIME` (`TIMELINE`) USING HASH,
KEY `INX_APPID` (`APP_ID`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_flink_app
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_app`;

CREATE TABLE `t_flink_app` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`JOB_TYPE` tinyint DEFAULT NULL,
`EXECUTION_MODE` tinyint DEFAULT NULL,
`PROJECT_ID` varchar(64) DEFAULT NULL,
`JOB_NAME` varchar(255) DEFAULT NULL,
`MODULE` varchar(255) DEFAULT NULL,
`JAR` varchar(255) DEFAULT NULL,
`MAIN_CLASS` varchar(255) DEFAULT NULL,
`ARGS` text DEFAULT NULL,
`OPTIONS` text DEFAULT NULL,
`USER_ID` bigint DEFAULT NULL,
`APP_ID` varchar(255) DEFAULT NULL,
`APP_TYPE` tinyint DEFAULT NULL,
`DURATION` bigint DEFAULT NULL,
`JOB_ID` varchar(64) DEFAULT NULL,
`STATE` varchar(50) DEFAULT NULL,
`RESTART_SIZE` int DEFAULT NULL,
`RESTART_COUNT` int DEFAULT NULL,
`CP_THRESHOLD` int DEFAULT NULL,
`CP_MAX_FAILURE_INTERVAL` int NULL,
`CP_FAILURE_RATE_INTERVAL` int NULL,
`CP_FAILURE_ACTION` tinyint NULL,
`DYNAMIC_OPTIONS` text DEFAULT NULL,
`DESCRIPTION` varchar(255) DEFAULT NULL,
`RESOLVE_ORDER` tinyint DEFAULT NULL,
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
`ALERT_EMAIL` varchar(255) DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE,
KEY `INX_STATE` (`STATE`) USING BTREE,
KEY `INX_JOB_TYPE` (`JOB_TYPE`) USING BTREE,
KEY `INX_TRACK` (`TRACKING`) USING BTREE
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
`CONTENT` text NOT NULL,
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_flink_log
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_log`;
CREATE TABLE `t_flink_log` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint DEFAULT NULL,
`YARN_APP_ID` varchar(50) DEFAULT NULL,
`SUCCESS` tinyint DEFAULT NULL,
`EXCEPTION` text DEFAULT NULL,
`START_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_flink_project
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_project`;
CREATE TABLE `t_flink_project` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`NAME` varchar(255) DEFAULT NULL,
`URL` varchar(1000) DEFAULT NULL,
`BRANCHES` varchar(1000) DEFAULT NULL,
`USERNAME` varchar(255) DEFAULT NULL,
`PASSWORD` varchar(255) DEFAULT NULL,
`POM` varchar(255) DEFAULT NULL,
`TYPE` tinyint DEFAULT NULL,
`REPOSITORY` tinyint DEFAULT NULL,
`DATE` datetime DEFAULT NULL,
`LASTBUILD` datetime DEFAULT NULL,
`DESCRIPTION` varchar(255) DEFAULT NULL,
`BUILDSTATE` tinyint DEFAULT '-1',
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_flink_savepoint
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_savepoint`;
CREATE TABLE `t_flink_savepoint` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`APP_ID` bigint NOT NULL,
`TYPE` tinyint DEFAULT NULL,
`PATH` varchar(255) DEFAULT NULL,
`LATEST` tinyint NOT NULL,
`TRIGGER_TIME` datetime DEFAULT NULL,
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_flink_sql
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_sql`;
CREATE TABLE `t_flink_sql` (
`ID` bigint NOT NULL,
`APP_ID` bigint DEFAULT NULL,
`SQL` text DEFAULT NULL,
`DEPENDENCY` text DEFAULT NULL,
`VERSION` int DEFAULT NULL,
`CANDIDATE` tinyint NOT NULL DEFAULT '0',
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_flink_tutorial
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_tutorial`;
CREATE TABLE `t_flink_tutorial` (
`ID` int NOT NULL AUTO_INCREMENT,
`TYPE` tinyint DEFAULT NULL,
`NAME` varchar(255) DEFAULT NULL,
`CONTENT` text DEFAULT NULL,
`CREATE_TIME` datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_menu`;
CREATE TABLE `t_menu` (
`MENU_ID` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单/按钮ID',
`PARENT_ID` bigint NOT NULL COMMENT '上级菜单ID',
`MENU_NAME` varchar(50) NOT NULL COMMENT '菜单/按钮名称',
`PATH` varchar(255) DEFAULT NULL COMMENT '对应路由path',
`COMPONENT` varchar(255) DEFAULT NULL COMMENT '对应路由组件component',
`PERMS` varchar(50) DEFAULT NULL COMMENT '权限标识',
`ICON` varchar(50) DEFAULT NULL COMMENT '图标',
`TYPE` char(2) COMMENT '类型 0菜单 1按钮',
`DISPLAY` char(2) NOT NULL DEFAULT '1' COMMENT '菜单是否显示',
`ORDER_NUM` double(20,0) DEFAULT NULL COMMENT '排序',
`CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
`MODIFY_TIME` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`MENU_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role` (
`ROLE_ID` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
`ROLE_NAME` varchar(50) NOT NULL COMMENT '角色名称',
`REMARK` varchar(100) DEFAULT NULL COMMENT '角色描述',
`CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
`MODIFY_TIME` datetime DEFAULT NULL COMMENT '修改时间',
`ROLE_CODE` varchar(255) DEFAULT NULL COMMENT '角色标识',
PRIMARY KEY (`ROLE_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_role_menu`;
CREATE TABLE `t_role_menu` (
`ROLE_ID` bigint NOT NULL,
`MENU_ID` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_setting
-- ----------------------------
DROP TABLE IF EXISTS `t_setting`;
CREATE TABLE `t_setting` (
`NUM` int DEFAULT NULL,
`KEY` varchar(50) NOT NULL,
`VALUE` varchar(255) DEFAULT NULL,
`TITLE` varchar(255) DEFAULT NULL,
`DESCRIPTION` varchar(255) DEFAULT NULL,
`TYPE` tinyint NOT NULL COMMENT '1: input 2: boolean 3: number',
PRIMARY KEY (`KEY`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
`USER_ID` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
`USERNAME` varchar(255) DEFAULT NULL COMMENT '登录用户名',
`NICK_NAME` varchar(50) NOT NULL COMMENT '昵称',
`SALT` varchar(255) DEFAULT NULL COMMENT '密码加盐',
`PASSWORD` varchar(128) NOT NULL COMMENT '密码',
`DEPT_ID` bigint DEFAULT NULL COMMENT '部门ID',
`EMAIL` varchar(128) DEFAULT NULL COMMENT '邮箱',
`MOBILE` varchar(20) DEFAULT NULL COMMENT '联系电话',
`STATUS` char(1) NOT NULL COMMENT '状态 0锁定 1有效',
`CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
`MODIFY_TIME` datetime DEFAULT NULL COMMENT '修改时间',
`LAST_LOGIN_TIME` datetime DEFAULT NULL COMMENT '最近访问时间',
`SEX` char(1) DEFAULT NULL COMMENT '性别 0男 1女 2保密',
`DESCRIPTION` varchar(100) DEFAULT NULL COMMENT '描述',
`AVATAR` varchar(100) DEFAULT NULL COMMENT '用户头像',
`USER_TYPE` char(1) NOT NULL DEFAULT '2' COMMENT '用户类型 1内部用户 2外部用户',
PRIMARY KEY (`USER_ID`) USING BTREE,
UNIQUE KEY `UN_USERNAME` (`NICK_NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for t_user_role
-- ----------------------------
DROP TABLE IF EXISTS `t_user_role`;
CREATE TABLE `t_user_role` (
`USER_ID` bigint DEFAULT NULL COMMENT '用户ID',
`ROLE_ID` bigint DEFAULT NULL COMMENT '角色ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;

COMMIT
