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

drop table if exists `t_external_link`;
CREATE TABLE `t_external_link` (
  `id` bigint not null auto_increment primary key,
  `badge_label` varchar(100) collate utf8mb4_general_ci default null,
  `badge_name` varchar(100) collate utf8mb4_general_ci default null,
  `badge_color` varchar(100) collate utf8mb4_general_ci default null,
  `link_url` varchar(1000) collate utf8mb4_general_ci default null,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `modify_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'modify time'
) engine = innodb default charset=utf8mb4 collate=utf8mb4_general_ci;

drop table if exists `t_yarn_queue`;
create table `t_yarn_queue` (
  `id` bigint not null primary key auto_increment comment 'queue id',
  `team_id` bigint not null comment 'team id',
  `queue_label` varchar(255) collate utf8mb4_general_ci not null comment 'queue and label expression',
  `description` varchar(512) collate utf8mb4_general_ci default null comment 'description of the queue label',
  `create_time` datetime not null default current_timestamp comment 'create time',
  `modify_time` datetime not null default current_timestamp on update current_timestamp comment 'modify time',
  unique key `unq_team_id_queue_label` (`team_id`, `queue_label`) using btree
) engine = innodb default charset = utf8mb4 collate = utf8mb4_general_ci;

drop table if exists t_flink_tutorial;

alter table `t_user` add column `login_type` tinyint default 0 after `user_type`;
alter table `t_flink_app` change column `launch` `release` tinyint default 1;
alter table `t_flink_log` add column `option_name` tinyint default null;
alter table `t_flink_savepoint` modify column `path` varchar(1024) collate utf8mb4_general_ci default null;

-- menu script
delete from t_menu;
-- menu level 1
insert into `t_menu` values (110000, 0, 'menu.system', '/system', 'PageView', null, 'desktop', '0', 1, 1, now(), now());
insert into `t_menu` values (120000, 0, 'StreamPark', '/flink', 'PageView', null, 'build', '0', 1, 2, now(), now());
insert into `t_menu` values (130000, 0, 'menu.setting', '/setting', 'PageView', null, 'setting', '0', 1, 5, now(), now());
-- menu level 2
insert into `t_menu` values (110100, 110000, 'menu.userManagement', '/system/user', 'system/user/User', null, 'user', '0', 1, 1, now(), now());
insert into `t_menu` values (110200, 110000, 'menu.roleManagement', '/system/role', 'system/role/Role', null, 'smile', '0', 1, 2, now(), now());
insert into `t_menu` values (110300, 110000, 'menu.menuManagement', '/system/menu', 'system/menu/Menu', 'menu:view', 'bars', '0', 1, 3, now(), now());
insert into `t_menu` values (110400, 110000, 'menu.tokenManagement', '/system/token', 'system/token/Token', null, 'lock', '0', 1, 1, now(), now());
insert into `t_menu` values (110500, 110000, 'menu.teamManagement', '/system/team', 'system/team/Team', null, 'team', '0', 1, 2, now(), now());
insert into `t_menu` values (110600, 110000, 'menu.memberManagement', '/system/member', 'system/member/Member', null, 'usergroup-add', '0', 1, 2, now(), now());
insert into `t_menu` values (120100, 120000, 'menu.project', '/flink/project', 'flink/project/View', null, 'github', '0', 1, 1, now(), now());
insert into `t_menu` values (120200, 120000, 'menu.application', '/flink/app', 'flink/app/View', null, 'mobile', '0', 1, 2, now(), now());
insert into `t_menu` values (120300, 120000, 'menu.variable', '/flink/variable', 'flink/variable/View', null, 'code', '0', 1, 3, now(), now());
insert into `t_menu` values (130100, 130000, 'setting.system', '/setting/system', 'setting/System/index', null, 'database', '0', 1, 1, now(), now());
insert into `t_menu` values (130200, 130000, 'setting.alarm', '/setting/alarm', 'setting/Alarm/index', null, 'alert', '0', 1, 2, now(), now());
insert into `t_menu` values (130300, 130000, 'setting.flinkHome', '/setting/flinkHome', 'setting/FlinkHome/index', null, 'desktop', '0', 1, 3, now(), now());
insert into `t_menu` values (130400, 130000, 'setting.flinkCluster', '/setting/flinkCluster', 'setting/FlinkCluster/index', 'menu:view', 'cluster', '0', 1, 4, now(), now());
insert into `t_menu` values (130500, 130000, 'setting.externalLink', '/setting/externalLink', 'setting/ExternalLink/index', 'menu:view', 'link', '0', 1, 5, now(), now());
insert into `t_menu` values (130600, 130000, 'setting.yarnQueue', '/setting/yarnQueue', 'setting/YarnQueue/index', 'menu:view', 'bars', '0', 1, 6, now(), now());
-- menu level 3
insert into `t_menu` values (110101, 110100, 'add', null, null, 'user:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (110102, 110100, 'update', null, null, 'user:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (110103, 110100, 'delete', null, null, 'user:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110104, 110100, 'reset', null, null, 'user:reset', null, '1', 1, null, now(), now());
insert into `t_menu` values (110105, 110100, 'types', null, null, 'user:types', null, '1', 1, null, now(), now());
insert into `t_menu` values (110106, 110100, 'view', null, null, 'user:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (110201, 110200, 'add', null, null, 'role:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (110202, 110200, 'update', null, null, 'role:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (110203, 110200, 'delete', null, null, 'role:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110204, 110200, 'view', null, null, 'role:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (110401, 110400, 'add', null, null, 'token:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (110402, 110400, 'delete', null, null, 'token:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110403, 110400, 'view', null, null, 'token:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (110501, 110500, 'add', null, null, 'team:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (110502, 110500, 'update', null, null, 'team:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (110503, 110500, 'delete', null, null, 'team:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110504, 110500, 'view', null, null, 'team:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (110601, 110600, 'add', null, null, 'member:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (110602, 110600, 'update', null, null, 'member:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (110603, 110600, 'delete', null, null, 'member:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110604, 110600, 'role view', null, null, 'role:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (110605, 110600, 'view', null, null, 'member:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (120101, 120100, 'add', '/flink/project/add', 'flink/project/Add', 'project:create', '', '0', 0, null, now(), now());
insert into `t_menu` values (120102, 120100, 'build', null, null, 'project:build', null, '1', 1, null, now(), now());
insert into `t_menu` values (120103, 120100, 'delete', null, null, 'project:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (120104, 120100, 'edit', '/flink/project/edit', 'flink/project/Edit', 'project:update', null, '0', 0, null, now(), now());
insert into `t_menu` values (120105, 120100, 'view', null, null, 'project:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (120201, 120200, 'add', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', 0, null, now(), now());
insert into `t_menu` values (120202, 120200, 'detail app', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', 0, null, now(), now());
insert into `t_menu` values (120203, 120200, 'edit flink', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', 0, null, now(), now());
insert into `t_menu` values (120204, 120200, 'edit streampark', '/flink/app/edit_streampark', 'flink/app/EditStreamPark', 'app:update', '', '0', 0, null, now(), now());
insert into `t_menu` values (120205, 120200, 'mapping', null, null, 'app:mapping', null, '1', 1, null, now(), now());
insert into `t_menu` values (120206, 120200, 'release', null, null, 'app:release', null, '1', 1, null, now(), now());
insert into `t_menu` values (120207, 120200, 'start', null, null, 'app:start', null, '1', 1, null, now(), now());
insert into `t_menu` values (120208, 120200, 'clean', null, null, 'app:clean', null, '1', 1, null, now(), now());
insert into `t_menu` values (120209, 120200, 'cancel', null, null, 'app:cancel', null, '1', 1, null, now(), now());
insert into `t_menu` values (120210, 120200, 'savepoint delete', null, null, 'savepoint:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (120211, 120200, 'backup rollback', null, null, 'backup:rollback', null, '1', 1, null, now(), now());
insert into `t_menu` values (120212, 120200, 'backup delete', null, null, 'backup:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (120213, 120200, 'conf delete', null, null, 'conf:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (120214, 120200, 'delete', null, null, 'app:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (120215, 120200, 'copy', null, null, 'app:copy', null, '1', 1, null, now(), now());
insert into `t_menu` values (120216, 120200, 'view', null, null, 'app:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (120217, 120200, 'savepoint trigger', null, null, 'savepoint:trigger', null, '1', 1, null, now(), now());
insert into `t_menu` values (120218, 120200, 'sql delete', null, null, 'sql:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (120301, 120300, 'add', NULL, NULL, 'variable:add', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (120302, 120300, 'update', NULL, NULL, 'variable:update', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (120303, 120300, 'delete', NULL, NULL, 'variable:delete', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (120304, 120300, 'depend apps', '/flink/variable/depend_apps', 'flink/variable/DependApps', 'variable:depend_apps', '', '0', 0, NULL, now(), now());
insert into `t_menu` values (120305, 120300, 'show original', NULL, NULL, 'variable:show_original', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (120306, 120300, 'view', NULL, NULL, 'variable:view', NULL, '1', 1, null, now(), now());
insert into `t_menu` values (120307, 120300, 'depend view', null, null, 'variable:depend_apps', null, '1', 1, NULL, now(), now());
insert into `t_menu` values (130101, 130100, 'view', null, null, 'setting:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (130102, 130100, 'setting update', null, null, 'setting:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (130401, 130400, 'add cluster', '/setting/add_cluster', 'setting/FlinkCluster/AddCluster', 'cluster:create', '', '0', 0, null, now(), now());
insert into `t_menu` values (130402, 130400, 'edit cluster', '/setting/edit_cluster', 'setting/FlinkCluster/EditCluster', 'cluster:update', '', '0', 0, null, now(), now());
insert into `t_menu` values (130501, 130500, 'link view', null, null, 'externalLink:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (130502, 130500, 'link create', null, null, 'externalLink:create', null, '1', 1, null, now(), now());
insert into `t_menu` values (130503, 130500, 'link update', null, null, 'externalLink:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (130504, 130500, 'link delete', null, null, 'externalLink:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (130601, 130600, 'add yarn queue', null, null, 'yarnQueue:create', '', '1', 0, null, now(), now());
insert into `t_menu` values (130602, 130600, 'edit yarn queue', null, null, 'yarnQueue:update', '', '1', 0, null, now(), now());
insert into `t_menu` values (130603, 130600, 'delete yarn queue', null, null, 'yarnQueue:delete', '', '1', 0, null, now(), now());

-- role menu script
delete from t_role_menu;
insert into `t_role_menu` (role_id, menu_id) values (100001, 120000);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120100);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120101);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120102);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120104);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120105);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120200);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120201);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120202);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120203);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120204);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120206);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120207);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120208);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120209);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120210);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120211);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120212);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120213);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120215);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120216);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120217);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120300);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120304);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120306);
insert into `t_role_menu` (role_id, menu_id) values (100001, 120307);
insert into `t_role_menu` (role_id, menu_id) values (100001, 130101);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110000);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110600);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110601);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110602);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110603);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110604);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110605);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120000);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120100);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120101);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120102);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120103);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120104);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120105);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120200);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120201);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120202);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120203);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120204);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120205);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120206);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120207);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120208);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120209);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120210);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120211);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120212);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120213);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120214);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120215);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120216);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120217);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120218);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120300);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120301);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120302);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120303);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120304);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120305);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120306);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120307);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130000);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130100);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130101);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130200);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130300);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130400);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130500);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130501);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130502);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130503);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130504);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130600);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130601);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130602);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130603);

set foreign_key_checks = 1;
