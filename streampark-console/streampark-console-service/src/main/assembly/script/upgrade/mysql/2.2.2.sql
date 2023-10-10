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

alter table `t_flink_app`
    add column `k8s_name` varchar(63) collate utf8mb4_general_ci default null,
    -- modify_time change with duration #3188
    modify column `modify_time` datetime not null default current_timestamp comment 'modify time';

alter table `t_flink_log`
    add column `user_id` bigint default null comment 'operator user id';
