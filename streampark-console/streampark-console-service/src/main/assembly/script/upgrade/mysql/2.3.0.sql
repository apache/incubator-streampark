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
