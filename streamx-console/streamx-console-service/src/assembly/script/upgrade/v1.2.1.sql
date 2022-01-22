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

INSERT INTO `t_menu` VALUES (37, 14, 'Edit Project', '/flink/project/edit', 'flink/project/Edit', 'project:update', NULL, '0', '0', NULL, NOW(), NOW());

delete from `t_role_menu` where id > 36 AND id < 56;
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
