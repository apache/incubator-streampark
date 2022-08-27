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

-- ------------------------------------- version: 1.2.1 START ---------------------------------------
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

BEGIN;
-- ALTER TABLE `t_flink_app` ADD COLUMN `K8S_HADOOP_INTEGRATION` tinyint(1) default 0 AFTER `K8S_TM_POD_TEMPLATE`;

-- ALTER TABLE `t_flink_app` ADD COLUMN `RESOURCE_FROM` tinyint(1) NULL AFTER `EXECUTION_MODE`;

-- ALTER TABLE `t_flink_app` ADD COLUMN `JAR_CHECK_SUM` bigint NULL AFTER `JAR`;

-- ALTER TABLE `t_flink_app` ADD COLUMN `HOT_PARAMS` text NULL AFTER `OPTIONS`;

update `t_flink_app` set `resource_from` = 1 where `job_type` = 1;

-- ALTER TABLE `t_user_role` ADD COLUMN `ID` bigint NOT NULL primary key AUTO_INCREMENT FIRST;
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
-- Table structure for t_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_role_menu`;
CREATE TABLE `t_role_menu` (
`id` bigint NOT NULL AUTO_INCREMENT,
`role_id` bigint NOT NULL,
`menu_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE KEY `UN_INX` (`role_id`,`menu_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_role_menu
-- ----------------------------
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
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

BEGIN;
-- menu
update `t_menu` set menu_name='launch',perms='app:launch' where menu_name='deploy';
-- change default value
UPDATE `t_setting` SET `key`='streamx.maven.central.repository' WHERE `key` = 'maven.central.repository';
COMMIT;

-- rename column
ALTER TABLE `t_flink_project`
    CHANGE COLUMN `username` `user_name` varchar(255) CHARACTER SET utf8mb4 DEFAULT NULL AFTER `branches`,
    CHANGE COLUMN `lastbuild` `last_build` datetime NULL DEFAULT NULL AFTER `date`,
    CHANGE COLUMN `buildstate` `build_state` tinyint NULL DEFAULT -1 AFTER `description`,
    ADD COLUMN `build_args` varchar(255) NULL AFTER `POM`;

-- rename column name DEPLOY to LAUNCH
ALTER TABLE `t_flink_app`
    CHANGE COLUMN `deploy` `launch` tinyint NULL DEFAULT 2 AFTER `create_time`,
    ADD COLUMN `build` tinyint DEFAULT '1' AFTER `launch`,
    ADD COLUMN `flink_cluster_id` bigint DEFAULT NULL AFTER `k8s_hadoop_integration`;

-- change column id to AUTO_INCREMENT
ALTER TABLE `t_flink_sql`
    CHANGE COLUMN `id` `id` bigint NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN `candidate` tinyint NOT NULL DEFAULT 1;

ALTER TABLE `t_flink_log`
    CHANGE COLUMN `start_time` `option_time` datetime NULL DEFAULT NULL AFTER `exception`;

-- change launch value
BEGIN;
update `t_flink_app` set launch = 0;
COMMIT;

-- change state value
BEGIN;
update `t_flink_app` set state = 0 where state in (1,2);
COMMIT;

BEGIN;
update `t_flink_app` set state = state - 2 where state > 1;
COMMIT;

-- t_setting
BEGIN;
update `t_setting` set `num` = `num` + 2 where `num` > 1;
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
UPDATE t_menu set parent_id=parent_id+99999 where parent_id != '0';
UPDATE t_menu set menu_id=menu_id+99999;
UPDATE t_role set role_id=role_id+99999;
UPDATE t_role_menu set id=id+99999,role_id=role_id+99999,menu_id=menu_id+99999;
UPDATE t_user set user_id=user_id+99999;
UPDATE t_user_role set id=id+99999,role_id=role_id+99999,user_id=user_id+99999;
UPDATE t_flink_app set user_id = user_id+99999;
COMMIT;

-- ----------------------------
-- Table of t_flink_cluster
-- ----------------------------
DROP TABLE IF EXISTS `t_flink_cluster`;
CREATE TABLE `t_flink_cluster`(
`id`              bigint NOT NULL AUTO_INCREMENT,
`cluster_name`    varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群名称',
`address`         text COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '集群地址,http://$host:$port多个地址用,分割',
`description`     varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
`create_time`     datetime DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


BEGIN;
INSERT INTO `t_menu` VALUES (100037, 100013, 'Add Cluster', '/flink/setting/add_cluster', 'flink/setting/AddCluster', 'cluster:create', '', '0', '0', null, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100038, 100013, 'Edit Cluster', '/flink/setting/edit_cluster', 'flink/setting/EditCluster', 'cluster:update', '', '0', '0', null, NOW(), NOW());
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
-- ------------------------------------- version: 1.2.2 END ---------------------------------------

-- ------------------------------------- version: 1.2.3 START ---------------------------------------
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

BEGIN;

-- ----------------------------
-- Table of t_access_token definition
-- ----------------------------
DROP TABLE IF EXISTS `t_access_token`;
CREATE TABLE `t_access_token` (
`id` int NOT NULL AUTO_INCREMENT COMMENT 'key',
`user_id`     bigint,
`token` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TOKEN',
`expire_time` datetime DEFAULT NULL COMMENT '过期时间',
`description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '使用场景描述',
`status` tinyint DEFAULT NULL COMMENT '1:enable,0:disable',
`create_time` datetime DEFAULT NULL COMMENT 'create time',
`modify_time` datetime DEFAULT NULL COMMENT 'modify time',
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table of t_flink_cluster
-- ----------------------------
ALTER TABLE `t_flink_cluster`
CHANGE COLUMN `address` `ADDRESS` varchar(255) CHARACTER SET utf8mb4 NOT NULL,
ADD COLUMN  `cluster_id` varchar(255) DEFAULT NULL COMMENT 'session模式的clusterId(yarn-session:application-id,k8s-session:cluster-id)',
ADD COLUMN  `options` text COMMENT '参数集合json形式',
ADD COLUMN  `yarn_queue` varchar(100) DEFAULT NULL COMMENT '任务所在yarn队列',
ADD COLUMN  `execution_mode` tinyint NOT NULL DEFAULT '1' COMMENT 'session类型(1:remote,3:yarn-session,5:kubernetes-session)',
ADD COLUMN  `version_id` bigint NOT NULL COMMENT 'flink对应id',
ADD COLUMN  `k8s_namespace` varchar(255) DEFAULT 'default' COMMENT 'k8s namespace',
ADD COLUMN  `service_account` varchar(50) DEFAULT NULL COMMENT 'k8s service account',
ADD COLUMN  `user_id` bigint DEFAULT NULL,
ADD COLUMN  `flink_image` varchar(255) DEFAULT NULL COMMENT 'flink使用镜像',
ADD COLUMN  `dynamic_options` text COMMENT '动态参数',
ADD COLUMN  `k8s_rest_exposed_type` tinyint DEFAULT '2' COMMENT 'k8s 暴露类型(0:LoadBalancer,1:ClusterIP,2:NodePort)',
ADD COLUMN  `k8s_hadoop_integration` tinyint DEFAULT '0',
ADD COLUMN  `flame_graph` tinyint DEFAULT '0' COMMENT '是否开启火焰图，默认不开启',
ADD COLUMN  `k8s_conf` varchar(255) DEFAULT NULL COMMENT 'k8s配置文件所在路径',
ADD COLUMN  `resolve_order` int(11) DEFAULT NULL,
ADD COLUMN  `exception` text COMMENT '异常信息',
ADD COLUMN  `cluster_state` tinyint DEFAULT '0' COMMENT '集群状态(0:创建未启动,1:已启动,2:停止)',
ADD UNIQUE INDEX `INX_NAME`(`cluster_name`),
ADD UNIQUE INDEX `INX_CLUSTER`(`cluster_id`, `address`, `execution_mode`);

INSERT INTO `t_menu` VALUES (100038, 100000, 'Token Management', '/system/token', 'system/token/Token', 'token:view', 'lock', '0', '1', 1.0, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100039, 100038, 'add', NULL, NULL, 'token:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100040, 100038, 'delete', NULL, NULL, 'token:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100041, 100013, 'Add Cluster', '/flink/setting/add_cluster', 'flink/setting/AddCluster', 'cluster:create', '', '0', '0', null, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100042, 100013, 'Edit Cluster', '/flink/setting/edit_cluster', 'flink/setting/EditCluster', 'cluster:update', '', '0', '0', null, NOW(), NOW());

INSERT INTO `t_role_menu` VALUES (100057, 100000, 100038);
INSERT INTO `t_role_menu` VALUES (100058, 100000, 100039);
INSERT INTO `t_role_menu` VALUES (100059, 100000, 100040);
INSERT INTO `t_role_menu` VALUES (100060, 100000, 100041);
INSERT INTO `t_role_menu` VALUES (100061, 100000, 100042);

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------- version: 1.2.4 START ---------------------------------------
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `t_alert_config`;
CREATE TABLE `t_alert_config` (
  `id`                   bigint   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id`              bigint   DEFAULT NULL,
  `alert_name`           varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '报警组名称',
  `alert_type`           int DEFAULT 0 COMMENT '报警类型',
  `email_params`         varchar(255) COLLATE utf8mb4_general_ci COMMENT '邮件报警配置信息',
  `sms_params`           text COLLATE utf8mb4_general_ci COMMENT '短信报警配置信息',
  `ding_talk_params`     text COLLATE utf8mb4_general_ci COMMENT '钉钉报警配置信息',
  `we_com_params`        varchar(255) COLLATE utf8mb4_general_ci COMMENT '企微报警配置信息',
  `http_callback_params` text COLLATE utf8mb4_general_ci COMMENT '报警http回调配置信息',
  `lark_params`          text COLLATE utf8mb4_general_ci COMMENT '飞书报警配置信息',
  `create_time`          datetime NOT NULL DEFAULT current_timestamp COMMENT '创建时间',
  `modify_time`          datetime NOT NULL DEFAULT current_timestamp ON UPDATE current_timestamp COMMENT '修改时间',
  INDEX `INX_USER_ID` (`user_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

BEGIN;
-- 增加 ALERT_ID 字段
ALTER TABLE t_flink_app ADD COLUMN alert_id bigint AFTER end_time;

-- 转存历史邮件报警配置
INSERT INTO t_alert_config(user_id, alert_name, alert_type, email_params)
SELECT a.user_id, concat('emailAlertConf-', (@rownum := @rownum + 1)) AS alert_name, 1 as alert_type, a.alert_email
from (select user_id, alert_email from t_flink_app where alert_email is not null group by user_id, alert_email) a,
     (select @rownum := 0) t;

-- 更新原表邮件配置 id
UPDATE t_flink_app a INNER JOIN t_alert_config b ON a.alert_email = b.email_params
    SET a.ALERT_ID = b.ID
WHERE a.alert_email = b.email_params;

-- 调整报警配置表 params 内容
UPDATE t_alert_config
SET email_params     = concat('{"contacts":"', email_params, '"}'),
    ding_talk_params = '{}',
    we_com_params='{}',
    lark_params='{}'
WHERE alert_type = 1;
-- 删除原 ALERT_EMAIL 字段
ALTER TABLE t_flink_app DROP COLUMN ALERT_EMAIL;

ALTER TABLE `t_flink_app` ADD COLUMN `option_time` datetime DEFAULT NULL AFTER `CREATE_TIME`;
ALTER TABLE t_setting modify COLUMN `value` text ;
INSERT INTO `t_setting` VALUES (14, 'docker.register.namespace', NULL, 'Docker Register Image namespace', 'Docker命名空间', 1);
ALTER TABLE `t_flink_app` ADD COLUMN `ingress_template` text COLLATE utf8mb4_general_ci COMMENT 'ingress模版文件';
ALTER TABLE `t_flink_app` ADD COLUMN `default_mode_ingress` text COLLATE utf8mb4_general_ci COMMENT '配置ingress的域名';
ALTER TABLE `t_flink_app` ADD COLUMN `modify_time` datetime NOT NULL DEFAULT current_timestamp ON UPDATE CURRENT_TIMESTAMP AFTER create_time;


-- 项目组
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

insert into t_team values (1,'bigdata','BIGDATA','2022-02-21 18:00:00');

-- 组与user 的对应关系
DROP TABLE IF EXISTS `t_team_user`;
CREATE TABLE `t_team_user`
(
    `team_id`    bigint   NOT NULL COMMENT 'teamId',
    `user_id`     bigint   NOT NULL COMMENT 'userId',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    UNIQUE KEY `GROUP_USER` (`team_id`,`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- 给 app 和 project 加字段
ALTER TABLE `t_flink_app` ADD COLUMN `team_id` bigint not null default 1 comment '任务所属组';
ALTER TABLE `t_flink_project` ADD COLUMN `team_id` bigint not null default 1 comment '项目所属组';



-- 添加用户组管理的权限
INSERT INTO `t_menu` VALUES (100043, 100000, 'Team Management', '/system/team', 'system/team/Team', 'team:view', 'team', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100044, 100043, 'add', NULL, NULL, 'team:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100045, 100043, 'update', NULL, NULL, 'team:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100046, 100043, 'delete', NULL, NULL, 'team:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100047, 100015, 'copy', null, null, 'app:copy', null, '1', '1', null, NOW(), null);


-- 给Admin添加权限
INSERT INTO `t_role_menu` VALUES (100062, 100000, 100043);
INSERT INTO `t_role_menu` VALUES (100063, 100000, 100044);
INSERT INTO `t_role_menu` VALUES (100064, 100000, 100045);
INSERT INTO `t_role_menu` VALUES (100065, 100000, 100046);
INSERT INTO `t_role_menu` VALUES (100066, 100000, 100047);

-- 移除用户表联系电话字段
ALTER TABLE `t_user` DROP COLUMN `mobile`;


COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
---------------------------------------- version: 1.2.4 END ---------------------------------------
