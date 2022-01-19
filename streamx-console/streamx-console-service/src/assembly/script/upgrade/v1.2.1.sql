SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `t_flink_app` ADD COLUMN `K8S_HADOOP_INTEGRATION` tinyint(1) default 0 AFTER `K8S_TM_POD_TEMPLATE`;

ALTER TABLE `t_flink_app` ADD COLUMN `RESOURCE_FROM` tinyint(1) NULL AFTER `EXECUTION_MODE`;

ALTER TABLE `t_flink_app` ADD COLUMN `JAR_CHECK_SUM` bigint NULL AFTER `JAR`;

update `t_flink_app` set `RESOURCE_FROM` = 1 where `JOB_TYPE` = 1;

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

BEGIN;
delete from `t_role_menu` where id > 36 AND id < 55;

INSERT INTO `t_role_menu` VALUES (37, 2, 14);
INSERT INTO `t_role_menu` VALUES (38, 2, 16);
INSERT INTO `t_role_menu` VALUES (39, 2, 17);
INSERT INTO `t_role_menu` VALUES (40, 2, 18);
INSERT INTO `t_role_menu` VALUES (41, 2, 19);
INSERT INTO `t_role_menu` VALUES (42, 2, 20);
INSERT INTO `t_role_menu` VALUES (43, 2, 21);
INSERT INTO `t_role_menu` VALUES (44, 2, 22);
INSERT INTO `t_role_menu` VALUES (45, 2, 25);
INSERT INTO `t_role_menu` VALUES (46, 2, 26);
INSERT INTO `t_role_menu` VALUES (47, 2, 27);
INSERT INTO `t_role_menu` VALUES (48, 2, 28);
INSERT INTO `t_role_menu` VALUES (49, 2, 29);
INSERT INTO `t_role_menu` VALUES (50, 2, 30);
INSERT INTO `t_role_menu` VALUES (51, 2, 31);
INSERT INTO `t_role_menu` VALUES (52, 2, 32);
INSERT INTO `t_role_menu` VALUES (53, 2, 33);
INSERT INTO `t_role_menu` VALUES (54, 2, 34);
COMMIT;


drop table if exists t_alert_config;
create table `t_alert_config`
(
    `ID`                   bigint   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `USER_ID`              bigint                                  DEFAULT NULL,
    `ALERT_NAME`           varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '报警组名称',
    `ALERT_TYPE`           int                                     DEFAULT 0 COMMENT '报警类型',
    `EMAIL_PARAMS`         varchar(255) COLLATE utf8mb4_general_ci COMMENT '邮件报警配置信息',
    `DING_TALK_PARAMS`     text COLLATE utf8mb4_general_ci COMMENT '钉钉报警配置信息',
    `WE_COM_PARAMS`        varchar(255) COLLATE utf8mb4_general_ci COMMENT '企微报警配置信息',
    `HTTP_CALLBACK_PARAMS` text COLLATE utf8mb4_general_ci COMMENT '报警http回调配置信息',
    `CREATE_TIME`          datetime NOT NULL                       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `MODIFY_TIME`          datetime NOT NULL                       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    INDEX `INX_USER_ID` (`USER_ID`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

alter table t_flink_app add column ALERT_ID bigint after END_TIME;
alter table t_flink_app drop column ALERT_EMAIL;

SET FOREIGN_KEY_CHECKS = 1;
