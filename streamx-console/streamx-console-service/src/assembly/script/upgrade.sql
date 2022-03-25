-- ------------------------------------- version: 1.2.1 START ---------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
COMMIT;

BEGIN;
-- ALTER TABLE `t_flink_app` ADD COLUMN `K8S_HADOOP_INTEGRATION` tinyint(1) default 0 AFTER `K8S_TM_POD_TEMPLATE`;

-- ALTER TABLE `t_flink_app` ADD COLUMN `RESOURCE_FROM` tinyint(1) NULL AFTER `EXECUTION_MODE`;

-- ALTER TABLE `t_flink_app` ADD COLUMN `JAR_CHECK_SUM` bigint NULL AFTER `JAR`;

-- ALTER TABLE `t_flink_app` ADD COLUMN `HOT_PARAMS` text NULL AFTER `OPTIONS`;

update `t_flink_app` set `RESOURCE_FROM` = 1 where `JOB_TYPE` = 1;

-- ALTER TABLE `t_user_role` ADD COLUMN `ID` bigint NOT NULL primary key AUTO_INCREMENT FIRST;
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
-- Table structure for t_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_role_menu`;
CREATE TABLE `t_role_menu` (
`ID` bigint NOT NULL AUTO_INCREMENT,
`ROLE_ID` bigint NOT NULL,
`MENU_ID` bigint NOT NULL,
PRIMARY KEY (`ID`) USING BTREE,
UNIQUE KEY `UN_INX` (`ROLE_ID`,`MENU_ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
INSERT INTO `t_role_menu` VALUES (37, 1, 37);
INSERT INTO `t_role_menu` VALUES (38, 2, 14);
INSERT INTO `t_role_menu` VALUES (39, 2, 16);
INSERT INTO `t_role_menu` VALUES (40, 2, 17);
INSERT INTO `t_role_menu` VALUES (41, 2, 18);
INSERT INTO `t_role_menu` VALUES (42, 2, 19);
INSERT INTO `t_role_menu` VALUES (43, 2, 20);
INSERT INTO `t_role_menu` VALUES (44, 2, 21);
INSERT INTO `t_role_menu` VALUES (45, 2, 22);
INSERT INTO `t_role_menu` VALUES (46, 2, 25);
INSERT INTO `t_role_menu` VALUES (47, 2, 26);
INSERT INTO `t_role_menu` VALUES (48, 2, 27);
INSERT INTO `t_role_menu` VALUES (49, 2, 28);
INSERT INTO `t_role_menu` VALUES (50, 2, 29);
INSERT INTO `t_role_menu` VALUES (51, 2, 30);
INSERT INTO `t_role_menu` VALUES (52, 2, 31);
INSERT INTO `t_role_menu` VALUES (53, 2, 32);
INSERT INTO `t_role_menu` VALUES (54, 2, 33);
INSERT INTO `t_role_menu` VALUES (55, 2, 34);
COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
-- ------------------------------------- version: 1.2.1 END ---------------------------------------


-- ------------------------------------- version: 1.2.2 START ---------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

-- menu
update `t_menu` set MENU_NAME='launch',PERMS='app:launch' where MENU_NAME='deploy';

-- change default value
UPDATE `t_setting` SET `KEY`='streamx.maven.central.repository' WHERE `KEY` = 'maven.central.repository';

-- rename column
ALTER TABLE `t_flink_project`
    CHANGE COLUMN `USERNAME` `USER_NAME` varchar(255) CHARACTER SET utf8mb4 DEFAULT NULL AFTER `BRANCHES`,
    CHANGE COLUMN `LASTBUILD` `LAST_BUILD` datetime(0) NULL DEFAULT NULL AFTER `DATE`,
    CHANGE COLUMN `BUILDSTATE` `BUILD_STATE` tinyint(4) NULL DEFAULT -1 AFTER `DESCRIPTION`,
    ADD COLUMN `BUILD_ARGS` varchar(255) NULL AFTER `POM`;

-- rename column name DEPLOY to LAUNCH
ALTER TABLE `t_flink_app`
    CHANGE COLUMN `DEPLOY` `LAUNCH` tinyint NULL DEFAULT 2 AFTER `CREATE_TIME`,
    ADD COLUMN `BUILD` tinyint DEFAULT '1' AFTER `LAUNCH`,
    ADD COLUMN `FLINK_CLUSTER_ID` bigint DEFAULT NULL AFTER `K8S_HADOOP_INTEGRATION`;

-- change column id to AUTO_INCREMENT
ALTER TABLE `t_flink_sql`
    CHANGE COLUMN `id` `id` bigint NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN `CANDIDATE` tinyint(4) NOT NULL DEFAULT 1;

ALTER TABLE `t_flink_log`
    CHANGE COLUMN `START_TIME` `OPTION_TIME` datetime(0) NULL DEFAULT NULL AFTER `EXCEPTION`;

-- change launch value
BEGIN;
update `t_flink_app` set launch = 0;
COMMIT;

-- change state value
BEGIN;
update `t_flink_app` set STATE = 0 where STATE in (1,2);
COMMIT;

BEGIN;
update `t_flink_app` set STATE = STATE - 2 where STATE > 1;
COMMIT;

-- t_setting
BEGIN;
update `t_setting` set `NUM` = `NUM` + 2 where `NUM` > 1;
COMMIT;

BEGIN;
INSERT INTO `t_setting` VALUES (2, 'streamx.maven.auth.user', NULL, 'Maven Central Repository Auth User', 'Maven 私服认证用户名', 1);
INSERT INTO `t_setting` VALUES (3, 'streamx.maven.auth.password', NULL, 'Maven Central Repository Auth Password', 'Maven 私服认证密码', 1);
COMMIT;

-- change table AUTO_INCREMENT to 100000
BEGIN;
ALTER TABLE t_app_backup AUTO_INCREMENT = 100000 ;
ALTER TABLE t_flame_graph AUTO_INCREMENT = 100000 ;
ALTER TABLE t_flink_app AUTO_INCREMENT = 100000 ;
ALTER TABLE t_flink_config AUTO_INCREMENT = 100000 ;
ALTER TABLE t_flink_effective AUTO_INCREMENT = 100000 ;
ALTER TABLE t_flink_env AUTO_INCREMENT = 100000 ;
ALTER TABLE t_flink_log AUTO_INCREMENT = 100000 ;
ALTER TABLE t_flink_project AUTO_INCREMENT = 100000 ;
ALTER TABLE t_flink_savepoint AUTO_INCREMENT = 100000 ;
ALTER TABLE t_flink_sql AUTO_INCREMENT = 100000 ;
ALTER TABLE t_flink_tutorial AUTO_INCREMENT = 100000 ;
ALTER TABLE t_menu AUTO_INCREMENT = 100037 ;
ALTER TABLE t_message AUTO_INCREMENT = 100000 ;
ALTER TABLE t_role AUTO_INCREMENT = 100003 ;
ALTER TABLE t_role_menu AUTO_INCREMENT = 100055 ;
ALTER TABLE t_user AUTO_INCREMENT = 100001 ;
ALTER TABLE t_user_role AUTO_INCREMENT = 100001 ;
ALTER TABLE t_app_build_pipe AUTO_INCREMENT = 100000 ;
COMMIT;
-- update table id
BEGIN;
UPDATE t_menu set PARENT_ID=PARENT_ID+99999 where PARENT_ID != '0';
UPDATE t_menu set MENU_ID=MENU_ID+99999;
UPDATE t_role set ROLE_ID=ROLE_ID+99999;
UPDATE t_role_menu set ID=ID+99999,ROLE_ID=ROLE_ID+99999,MENU_ID=MENU_ID+99999;
UPDATE t_user set USER_ID=USER_ID+99999;
UPDATE t_user_role set ID=ID+99999,ROLE_ID=ROLE_ID+99999,USER_ID=USER_ID+99999;
UPDATE t_flink_app set USER_ID = USER_ID+99999;
COMMIT;
-- ----------------------------
-- Table of t_flink_cluster
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_cluster`;
CREATE TABLE `t_flink_cluster`(
`ID`              bigint NOT NULL AUTO_INCREMENT,
`CLUSTER_NAME`    varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群名称',
`ADDRESS`         text COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群地址,http://$host:$port多个地址用,分割',
`DESCRIPTION`     varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`CREATE_TIME`     datetime DEFAULT NULL,
PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;

BEGIN;
-- INSERT INTO `t_role_menu` VALUES (100055, 100001, 100013);
-- INSERT INTO `t_role_menu` VALUES (100056, 100001, 100015);
COMMIT;
-- ------------------------------------- version: 1.2.2 END ---------------------------------------
