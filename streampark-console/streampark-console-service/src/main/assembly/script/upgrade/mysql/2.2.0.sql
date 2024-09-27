/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use streampark;

set names utf8mb4;
set foreign_key_checks = 0;


-- ----------------------------
-- Table structure for t_app
-- ----------------------------
create table if not exists `t_app` (
`id` bigint not null,
`job_type` tinyint default null,
`create_time` datetime default null comment 'create time',
`modify_time` datetime default null comment 'modify time',
primary key(`id`)
);


alter table `t_flink_app`
    modify column `id` not null;
add column `k8s_name` varchar(63) collate utf8mb4_general_ci default null,
    -- modify_time change with duration #3188
    modify column `modify_time` datetime not null default current_timestamp comment 'modify time';

alter table t_app_backup rename to t_flink_app_backup;

alter table `t_flink_log`
    add column `user_id` bigint default null comment 'operator user id';

alter table `t_flink_project`
    add column `salt` varchar(26) collate utf8mb4_general_ci default null comment 'password salt',
    modify column `password` varchar(512) collate utf8mb4_general_ci default null comment 'password';

alter table `t_flink_sql`
    add column `team_resource` varchar(64) default null;

alter table `t_flink_cluster`
    add column `job_manager_url` varchar(150) default null comment 'url address of jobmanager' after `address`,
    add column `start_time` datetime default null comment 'start time',
    add column `end_time` datetime default null comment 'end time',
    add column `alert_id` bigint default null comment 'alert id';

-- ----------------------------
-- Table of t_resource
-- ----------------------------
drop table if exists `t_resource`;
create table `t_resource` (
`id` bigint not null auto_increment,
`resource_name` varchar(128) collate utf8mb4_general_ci not null comment 'The name of the resource file',
`resource_type` int  not null comment '0:app 1:common 2:connector 3:format 4:udf',
`resource` text collate utf8mb4_general_ci comment 'resource content, including jars and poms',
`engine_type` int  not null comment 'compute engine type, 0:apache flink 1:apache spark',
`main_class` varchar(255) collate utf8mb4_general_ci default null,
`description` text collate utf8mb4_general_ci default null comment 'More detailed description of resource',
`creator_id` bigint collate utf8mb4_general_ci not null comment 'user id of creator',
`team_id` bigint collate utf8mb4_general_ci not null comment 'team id',
`create_time` datetime default null comment 'create time',
`modify_time` datetime default null comment 'modify time',
primary key (`id`) using btree,
unique key `un_team_vcode_inx` (`team_id`,`resource_name`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;

-- menu level 2
insert into `t_menu` values (120400, 120000, 'menu.resource', '/flink/resource', 'flink/resource/View', null, 'apartment', '0', 1, 3, now(), now());
-- menu level 3
insert into `t_menu` values (120401, 120400, 'add', NULL, NULL, 'resource:add', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (120402, 120400, 'update', NULL, NULL, 'resource:update', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (120403, 120400, 'delete', NULL, NULL, 'resource:delete', NULL, '1', 1, NULL, now(), now());

-- role menu script
insert into `t_role_menu` (role_id, menu_id) values (100001, 120400);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120401);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120402);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120403);

insert into `t_role_menu` (role_id, menu_id) values (100002, 120400);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120401);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120402);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120403);

-- add sso as login type
alter table `t_user` modify column `password` varchar(64) collate utf8mb4_general_ci default null comment 'password';
alter table `t_user` modify column `login_type` tinyint default 0 comment 'login type 0:password 1:ldap 2:sso';

-- ----------------------------
-- Table structure for jdbc registry
-- ----------------------------
DROP TABLE IF EXISTS `t_jdbc_registry_data`;
CREATE TABLE `t_jdbc_registry_data` (
    `id`               bigint(11)   NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `data_key`         varchar(256) NOT NULL COMMENT 'key, like zookeeper node path',
    `data_value`       text         NOT NULL COMMENT 'data, like zookeeper node value',
    `data_type`        varchar(64)  NOT NULL COMMENT 'EPHEMERAL, PERSISTENT',
    `client_id`        bigint(11)   NOT NULL COMMENT 'client id',
    `create_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `last_update_time` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'last update time',
    PRIMARY KEY (`id`),
    unique Key `uk_t_jdbc_registry_dataKey` (`data_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


DROP TABLE IF EXISTS `t_jdbc_registry_lock`;
CREATE TABLE `t_jdbc_registry_lock` (
    `id`          bigint(11)   NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `lock_key`    varchar(256) NOT NULL COMMENT 'lock path',
    `lock_owner`  varchar(256) NOT NULL COMMENT 'the lock owner, ip_processId',
    `client_id`   bigint(11)   NOT NULL COMMENT 'client id',
    `create_time` timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    PRIMARY KEY (`id`),
    unique Key `uk_t_jdbc_registry_lockKey` (`lock_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `t_jdbc_registry_client_heartbeat`;
CREATE TABLE `t_jdbc_registry_client_heartbeat` (
    `id`                  bigint(11)   NOT NULL COMMENT 'primary key',
    `client_name`         varchar(256) NOT NULL COMMENT 'client name, ip_processId',
    `last_heartbeat_time` bigint(11)   NOT NULL COMMENT 'last heartbeat timestamp',
    `connection_config`   text         NOT NULL COMMENT 'connection config',
    `create_time`         timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `t_jdbc_registry_data_change_event`;
CREATE TABLE `t_jdbc_registry_data_change_event` (
    `id`                 bigint(11)  NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `event_type`         varchar(64) NOT NULL COMMENT 'ADD, UPDATE, DELETE',
    `jdbc_registry_data` text        NOT NULL COMMENT 'jdbc registry data',
    `create_time`        timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

set foreign_key_checks = 1;
