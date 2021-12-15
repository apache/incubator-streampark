SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `t_flink_app` ADD COLUMN `K8S_HADOOP_INTEGRATION` tinyint(1) default 0 AFTER `K8S_TM_POD_TEMPLATE`;

-- ----------------------------
-- Table structure for t_app_build_pipe
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

