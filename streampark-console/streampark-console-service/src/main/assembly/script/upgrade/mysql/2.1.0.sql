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

-- ISSUE-2192 DDL & DML Start

alter table `t_flink_savepoint` modify column `path`  varchar(1024) collate utf8mb4_general_ci default null;

insert into `t_menu` values (100070, 100015, 'savepoint trigger', null, null, 'savepoint:trigger', null, '1', 1, null, now(), now());

-- ISSUE-2192 DDL & DML End


-- ISSUE-2366 DDL & DML Start
alter table `t_flink_app` change column `launch` `release` tinyint default 1;
update `t_menu` set `menu_name`='release',`perms` = 'app:release' where `menu_id` = 100025;
-- ISSUE-2366 DDL & DML End
