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

-- reset t_menu
truncate table `t_menu`;
insert into `t_menu` values (110000, 0, 'Apache Flink', '/flink', 'PageView', null, null, '0', 1, 1, now(), now());
insert into `t_menu` values (120000, 0, 'Apache Spark', '/spark', 'PageView', null, null, '0', 1, 2, now(), now());
insert into `t_menu` values (130000, 0, 'resource.menu', '/resource', 'PageView', null, 0, '0', 1, 3, now(), now());
insert into `t_menu` values (140000, 0, 'setting.menu', '/setting', 'PageView', null, 0, '0', 1, 4, now(), now());
insert into `t_menu` values (150000, 0, 'system.menu', '/system', 'PageView', null, 0, '0', 1, 5, now(), now());

insert into `t_menu` values (110100, 110000, 'flink.application', '/flink/app', 'flink/app/View', null, null, '0', 1, 2, now(), now());
insert into `t_menu` values (110200, 110000, 'flink.flinkHome', '/flink/home', 'flink/home/View', null, null, '0', 1, 3, now(), now());
insert into `t_menu` values (110300, 110000, 'flink.flinkCluster', '/flink/cluster', 'flink/cluster/View', 'menu:view', null, '0', 1, 4, now(), now());

insert into `t_menu` values (110101, 110100, 'app view', null, null, 'app:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (110102, 110100, 'app add', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', 0, null, now(), now());
insert into `t_menu` values (110103, 110100, 'app detail', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', 0, null, now(), now());
insert into `t_menu` values (110104, 110100, 'app edit flink', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', 0, null, now(), now());
insert into `t_menu` values (110105, 110100, 'app edit streampark', '/flink/app/edit_streampark', 'flink/app/EditStreamPark', 'app:update', '', '0', 0, null, now(), now());
insert into `t_menu` values (110106, 110100, 'app mapping', null, null, 'app:mapping', null, '1', 1, null, now(), now());
insert into `t_menu` values (110107, 110100, 'app release', null, null, 'app:release', null, '1', 1, null, now(), now());
insert into `t_menu` values (110108, 110100, 'app start', null, null, 'app:start', null, '1', 1, null, now(), now());
insert into `t_menu` values (110109, 110100, 'app clean', null, null, 'app:clean', null, '1', 1, null, now(), now());
insert into `t_menu` values (110110, 110100, 'app cancel', null, null, 'app:cancel', null, '1', 1, null, now(), now());
insert into `t_menu` values (110111, 110100, 'app savepoint delete', null, null, 'savepoint:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110112, 110100, 'app backup rollback', null, null, 'backup:rollback', null, '1', 1, null, now(), now());
insert into `t_menu` values (110113, 110100, 'app backup delete', null, null, 'backup:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110114, 110100, 'app conf delete', null, null, 'conf:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110115, 110100, 'app delete', null, null, 'app:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110116, 110100, 'app copy', null, null, 'app:copy', null, '1', 1, null, now(), now());
insert into `t_menu` values (110117, 110100, 'app savepoint trigger', null, null, 'savepoint:trigger', null, '1', 1, null, now(), now());
insert into `t_menu` values (110118, 110100, 'app sql delete', null, null, 'sql:delete', null, '1', 1, null, now(), now());

insert into `t_menu` values (110301, 110300, 'cluster add', '/flink/add_cluster', 'flink/cluster/Add', 'cluster:create', '', '0', 0, null, now(), now());
insert into `t_menu` values (110302, 110300, 'cluster edit', '/flink/edit_cluster', 'flink/cluster/Edit', 'cluster:update', '', '0', 0, null, now(), now());

insert into `t_menu` values (130100, 130000, 'resource.project', '/resource/project', 'resource/project/View', null, 'github', '0', 1, 3, now(), now());
insert into `t_menu` values (130200, 130000, 'resource.variable', '/resource/variable', 'resource/variable/View', null, null, '0', 1, 4, now(), now());
insert into `t_menu` values (130300, 130000, 'resource.upload', '/resource/upload', 'resource/upload/View', null, null, '0', 1, 3, now(), now());

insert into `t_menu` values (130101, 130100, 'project view', null, null, 'project:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (130102, 130100, 'project add', '/project/add', 'project/Add', 'project:create', '', '0', 0, null, now(), now());
insert into `t_menu` values (130103, 130100, 'project build', null, null, 'project:build', null, '1', 1, null, now(), now());
insert into `t_menu` values (130104, 130100, 'project delete', null, null, 'project:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (130105, 130100, 'project edit', '/project/edit', 'project/Edit', 'project:update', null, '0', 0, null, now(), now());

insert into `t_menu` values (130201, 130200, 'variable view', NULL, NULL, 'variable:view', NULL, '1', 1, null, now(), now());
insert into `t_menu` values (130202, 130200, 'variable depend view', null, null, 'variable:depend_apps', null, '1', 1, NULL, now(), now());
insert into `t_menu` values (130203, 130200, 'variable add', NULL, NULL, 'variable:add', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130204, 130200, 'variable update', NULL, NULL, 'variable:update', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130205, 130200, 'variable delete', NULL, NULL, 'variable:delete', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130206, 130200, 'variable depend apps', '/resource/variable/depend_apps', 'resource/variable/DependApps', 'variable:depend_apps', '', '0', 0, NULL, now(), now());
insert into `t_menu` values (130207, 130200, 'variable show original', NULL, NULL, 'variable:show_original', NULL, '1', 1, NULL, now(), now());

insert into `t_menu` values (130301, 130300, 'resource add', NULL, NULL, 'resource:add', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130302, 130300, 'resource update', NULL, NULL, 'resource:update', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130303, 130300, 'resource delete', NULL, NULL, 'resource:delete', NULL, '1', 1, NULL, now(), now());

insert into `t_menu` values (140100, 140000, 'setting.system', '/setting/system', 'setting/system/View', null, null, '0', 1, 1, now(), now());
insert into `t_menu` values (140200, 140000, 'setting.alarm', '/setting/alarm', 'setting/alarm/View', null, null, '0', 1, 2, now(), now());
insert into `t_menu` values (140300, 140000, 'setting.externalLink', '/setting/externalLink', 'setting/externalLink/View', 'menu:view', null, '0', 1, 5, now(), now());
insert into `t_menu` values (140400, 140000, 'setting.yarnQueue', '/setting/yarn-queue', 'setting/yarn-queue/View', 'menu:view', null, '0', 1, 6, now(), now());
insert into `t_menu` values (140101, 140100, 'setting view', null, null, 'setting:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (140102, 140100, 'setting update', null, null, 'setting:update', null, '1', 1, null, now(), now());

insert into `t_menu` values (140301, 140300, 'link view', null, null, 'externalLink:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (140302, 140300, 'link create', null, null, 'externalLink:create', null, '1', 1, null, now(), now());
insert into `t_menu` values (140303, 140300, 'link update', null, null, 'externalLink:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (140304, 140300, 'link delete', null, null, 'externalLink:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (140401, 140400, 'yarn queue add', null, null, 'yarnQueue:create', '', '1', 0, null, now(), now());
insert into `t_menu` values (140402, 140400, 'yarn queue edit', null, null, 'yarnQueue:update', '', '1', 0, null, now(), now());
insert into `t_menu` values (140403, 140400, 'yarn queue delete', null, null, 'yarnQueue:delete', '', '1', 0, null, now(), now());

insert into `t_menu` values (150100, 150000, 'system.user', '/system/user', 'system/user/View', null, null, '0', 1, 1, now(), now());
insert into `t_menu` values (150200, 150000, 'system.role', '/system/role', 'system/role/View', null, null, '0', 1, 2, now(), now());
insert into `t_menu` values (150300, 150000, 'system.token', '/system/token', 'system/token/View', null, null, '0', 1, 1, now(), now());
insert into `t_menu` values (150400, 150000, 'system.team', '/system/team', 'system/team/View', null, null, '0', 1, 2, now(), now());
insert into `t_menu` values (150500, 150000, 'system.member', '/system/member', 'system/member/View', null, null, '0', 1, 2, now(), now());

insert into `t_menu` values (150101, 150100, 'user view', null, null, 'user:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (150102, 150100, 'user add', null, null, 'user:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (150103, 150100, 'user update', null, null, 'user:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (150104, 150100, 'user delete', null, null, 'user:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (150105, 150100, 'user reset', null, null, 'user:reset', null, '1', 1, null, now(), now());
insert into `t_menu` values (150106, 150100, 'user types', null, null, 'user:types', null, '1', 1, null, now(), now());

insert into `t_menu` values (150201, 150200, 'role view', null, null, 'role:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (150202, 150200, 'role add', null, null, 'role:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (150203, 150200, 'role update', null, null, 'role:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (150204, 150200, 'role delete', null, null, 'role:delete', null, '1', 1, null, now(), now());

insert into `t_menu` values (150301, 150300, 'token view', null, null, 'token:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (150302, 150300, 'token add', null, null, 'token:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (150303, 150300, 'token delete', null, null, 'token:delete', null, '1', 1, null, now(), now());

insert into `t_menu` values (150401, 150400, 'team view', null, null, 'team:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (150402, 150400, 'team add', null, null, 'team:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (150403, 150400, 'team update', null, null, 'team:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (150404, 150400, 'team delete', null, null, 'team:delete', null, '1', 1, null, now(), now());

insert into `t_menu` values (150501, 150500, 'member view', null, null, 'member:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (150502, 150500, 'member add', null, null, 'member:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (150503, 150500, 'member update', null, null, 'member:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (150504, 150500, 'member delete', null, null, 'member:delete', null, '1', 1, null, now(), now());

-- reset t_role_menu
truncate table `t_role_menu`;
insert into `t_role_menu` values (100000, 100001, 110000);
insert into `t_role_menu` values (100001, 100001, 110100);
insert into `t_role_menu` values (100002, 100001, 110101);
insert into `t_role_menu` values (100003, 100001, 110102);
insert into `t_role_menu` values (100004, 100001, 110103);
insert into `t_role_menu` values (100005, 100001, 110104);
insert into `t_role_menu` values (100006, 100001, 110105);
insert into `t_role_menu` values (100007, 100001, 110106);
insert into `t_role_menu` values (100008, 100001, 110107);
insert into `t_role_menu` values (100009, 100001, 110108);
insert into `t_role_menu` values (100010, 100001, 110109);
insert into `t_role_menu` values (100011, 100001, 110110);
insert into `t_role_menu` values (100012, 100001, 110111);
insert into `t_role_menu` values (100013, 100001, 110112);
insert into `t_role_menu` values (100014, 100001, 110113);
insert into `t_role_menu` values (100015, 100001, 110114);
insert into `t_role_menu` values (100016, 100001, 110115);
insert into `t_role_menu` values (100017, 100001, 110116);
insert into `t_role_menu` values (100018, 100001, 110117);
insert into `t_role_menu` values (100019, 100001, 110118);
insert into `t_role_menu` values (100020, 100001, 110200);
insert into `t_role_menu` values (100021, 100001, 110300);
insert into `t_role_menu` values (100022, 100001, 110301);
insert into `t_role_menu` values (100023, 100001, 110302);
insert into `t_role_menu` values (100024, 100001, 120000);
insert into `t_role_menu` values (100025, 100001, 130000);
insert into `t_role_menu` values (100026, 100001, 130100);
insert into `t_role_menu` values (100027, 100001, 130101);
insert into `t_role_menu` values (100028, 100001, 130102);
insert into `t_role_menu` values (100029, 100001, 130103);
insert into `t_role_menu` values (100030, 100001, 130104);
insert into `t_role_menu` values (100031, 100001, 130105);
insert into `t_role_menu` values (100032, 100001, 130200);
insert into `t_role_menu` values (100033, 100001, 130201);
insert into `t_role_menu` values (100034, 100001, 130202);
insert into `t_role_menu` values (100035, 100001, 130203);
insert into `t_role_menu` values (100036, 100001, 130204);
insert into `t_role_menu` values (100037, 100001, 130205);
insert into `t_role_menu` values (100038, 100001, 130206);
insert into `t_role_menu` values (100039, 100001, 130207);
insert into `t_role_menu` values (100040, 100001, 130300);
insert into `t_role_menu` values (100041, 100001, 130301);
insert into `t_role_menu` values (100042, 100001, 130302);
insert into `t_role_menu` values (100043, 100001, 130303);
insert into `t_role_menu` values (100044, 100001, 140000);
insert into `t_role_menu` values (100045, 100001, 140100);
insert into `t_role_menu` values (100046, 100001, 140101);
insert into `t_role_menu` values (100047, 100002, 110000);
insert into `t_role_menu` values (100048, 100002, 110100);
insert into `t_role_menu` values (100049, 100002, 110101);
insert into `t_role_menu` values (100050, 100002, 110102);
insert into `t_role_menu` values (100051, 100002, 110103);
insert into `t_role_menu` values (100052, 100002, 110104);
insert into `t_role_menu` values (100053, 100002, 110105);
insert into `t_role_menu` values (100054, 100002, 110106);
insert into `t_role_menu` values (100055, 100002, 110107);
insert into `t_role_menu` values (100056, 100002, 110108);
insert into `t_role_menu` values (100057, 100002, 110109);
insert into `t_role_menu` values (100058, 100002, 110110);
insert into `t_role_menu` values (100059, 100002, 110111);
insert into `t_role_menu` values (100060, 100002, 110112);
insert into `t_role_menu` values (100061, 100002, 110113);
insert into `t_role_menu` values (100062, 100002, 110114);
insert into `t_role_menu` values (100063, 100002, 110115);
insert into `t_role_menu` values (100064, 100002, 110116);
insert into `t_role_menu` values (100065, 100002, 110117);
insert into `t_role_menu` values (100066, 100002, 110118);
insert into `t_role_menu` values (100067, 100002, 110200);
insert into `t_role_menu` values (100068, 100002, 110300);
insert into `t_role_menu` values (100069, 100002, 110301);
insert into `t_role_menu` values (100070, 100002, 110302);
insert into `t_role_menu` values (100071, 100002, 120000);
insert into `t_role_menu` values (100072, 100002, 130000);
insert into `t_role_menu` values (100073, 100002, 130100);
insert into `t_role_menu` values (100074, 100002, 130101);
insert into `t_role_menu` values (100075, 100002, 130102);
insert into `t_role_menu` values (100076, 100002, 130103);
insert into `t_role_menu` values (100077, 100002, 130104);
insert into `t_role_menu` values (100078, 100002, 130105);
insert into `t_role_menu` values (100079, 100002, 130200);
insert into `t_role_menu` values (100080, 100002, 130201);
insert into `t_role_menu` values (100081, 100002, 130202);
insert into `t_role_menu` values (100082, 100002, 130203);
insert into `t_role_menu` values (100083, 100002, 130204);
insert into `t_role_menu` values (100084, 100002, 130205);
insert into `t_role_menu` values (100085, 100002, 130206);
insert into `t_role_menu` values (100086, 100002, 130207);
insert into `t_role_menu` values (100087, 100002, 130300);
insert into `t_role_menu` values (100088, 100002, 130301);
insert into `t_role_menu` values (100089, 100002, 130302);
insert into `t_role_menu` values (100090, 100002, 130303);
insert into `t_role_menu` values (100091, 100002, 140000);
insert into `t_role_menu` values (100092, 100002, 140100);
insert into `t_role_menu` values (100093, 100002, 140101);
insert into `t_role_menu` values (100094, 100002, 140102);
insert into `t_role_menu` values (100095, 100002, 140200);
insert into `t_role_menu` values (100096, 100002, 140300);
insert into `t_role_menu` values (100097, 100002, 140301);
insert into `t_role_menu` values (100098, 100002, 140302);
insert into `t_role_menu` values (100099, 100002, 140303);
insert into `t_role_menu` values (100100, 100002, 140304);
insert into `t_role_menu` values (100101, 100002, 140400);
insert into `t_role_menu` values (100102, 100002, 140401);
insert into `t_role_menu` values (100103, 100002, 140402);
insert into `t_role_menu` values (100104, 100002, 140403);
insert into `t_role_menu` values (100105, 100002, 150000);
insert into `t_role_menu` values (100106, 100002, 150500);
insert into `t_role_menu` values (100107, 100002, 150501);
insert into `t_role_menu` values (100108, 100002, 150502);
insert into `t_role_menu` values (100109, 100002, 150503);
insert into `t_role_menu` values (100110, 100002, 150504);

alter table `t_flink_app`
    add column `k8s_name` varchar(63) collate utf8mb4_general_ci default null,
    -- modify_time change with duration #3188
    modify column `modify_time` datetime not null default current_timestamp comment 'modify time';

alter table `t_flink_log`
    add column `user_id` bigint default null comment 'operator user id';

alter table `t_flink_project`
    add column `salt` varchar(26) collate utf8mb4_general_ci default null comment 'password salt',
    modify column `password` varchar(512) collate utf8mb4_general_ci default null comment 'password';
