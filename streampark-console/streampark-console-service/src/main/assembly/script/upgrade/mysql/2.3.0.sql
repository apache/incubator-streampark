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

