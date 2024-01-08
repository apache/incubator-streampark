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
`create_time` datetime not null default current_timestamp comment 'create time',
`modify_time` datetime not null default current_timestamp on update current_timestamp comment 'modify time',
primary key (`id`) using btree,
unique key `un_team_vcode_inx` (`team_id`,`resource_name`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;

alter table `t_flink_sql`
    add column `team_resource` varchar(64) default null;

alter table `t_flink_app`
    add column `probing` tinyint default 0;

alter table `t_flink_app`
    add column `hadoop_user` varchar(64) default null;

alter table `t_flink_cluster`
    add column `job_manager_url` varchar(150) default null comment 'url address of jobmanager' after `address`,
    add column `start_time` datetime default null comment 'start time',
    add column `end_time` datetime default null comment 'end time',
    add column `alert_id` bigint default null comment 'alert id';

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
-- Table of t_flink_gateway
-- ----------------------------
drop table if exists `t_flink_gateway`;
create table `t_flink_gateway` (
`id` bigint not null auto_increment,
`gateway_name` varchar(128) collate utf8mb4_general_ci not null comment 'The name of the gateway',
`description` text collate utf8mb4_general_ci default null comment 'More detailed description of resource',
`gateway_type` int not null comment 'The type of the gateway',
`address` varchar(150) default null comment 'url address of gateway endpoint',
`create_time` datetime not null default current_timestamp comment 'create time',
`modify_time` datetime not null default current_timestamp on update current_timestamp comment 'modify time',
primary key (`id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;

-- menu level 2
insert into `t_menu` values (120500, 130000, 'setting.flinkGateway', '/setting/FlinkGateway', 'setting/FlinkGateway/index', null, 'apartment', '0', 1, 3, now(), now());
-- menu level 3
insert into `t_menu` values (120501, 120500, 'add', NULL, NULL, 'gateway:add', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (120502, 120500, 'update', NULL, NULL, 'gateway:update', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (120503, 120500, 'delete', NULL, NULL, 'gateway:delete', NULL, '1', 1, NULL, now(), now());

-- role menu script
insert into `t_role_menu` (role_id, menu_id) values (100001, 120500);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120501);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120502);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120503);

insert into `t_role_menu` (role_id, menu_id) values (100002, 120500);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120501);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120502);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120503);

set foreign_key_checks = 1;
