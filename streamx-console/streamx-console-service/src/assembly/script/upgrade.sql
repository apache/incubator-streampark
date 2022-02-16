--------------------------------------- version: 1.2.1 START ---------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `t_flink_app` ADD COLUMN `K8S_HADOOP_INTEGRATION` tinyint(1) default 0 AFTER `K8S_TM_POD_TEMPLATE`;

ALTER TABLE `t_flink_app` ADD COLUMN `RESOURCE_FROM` tinyint(1) NULL AFTER `EXECUTION_MODE`;

ALTER TABLE `t_flink_app` ADD COLUMN `JAR_CHECK_SUM` bigint NULL AFTER `JAR`;

ALTER TABLE `t_flink_app` ADD COLUMN `HOT_PARAMS` text NULL AFTER `OPTIONS`;

update `t_flink_app` set `RESOURCE_FROM` = 1 where `JOB_TYPE` = 1;

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

SET FOREIGN_KEY_CHECKS = 1;
--------------------------------------- version: 1.2.1 END ---------------------------------------


--------------------------------------- version: 1.2.2 START ---------------------------------------
ALTER TABLE `t_flink_app` ADD COLUMN `FLINK_CLUSTER_ID` bigint DEFAULT NULL AFTER `K8S_HADOOP_INTEGRATION`;

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

--------------------------------------- version: 1.2.2 END ---------------------------------------
