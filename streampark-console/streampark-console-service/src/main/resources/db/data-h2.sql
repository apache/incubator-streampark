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

-- ----------------------------
-- Records of t_team
-- ----------------------------
insert into `t_team` values (100000, 'default', 'The default team', now(), now());
insert into `t_team` values (100001, 'test', 'The test team', now(), now());


-- ----------------------------
-- Records of t_flink_app
-- ----------------------------
insert into `t_flink_app` values (100000, 100000, 2, 4, null, null, 'Flink SQL Demo', null, null, null, null, null, null , null, 100000, null, 1, null, null, null, null, null, null, null, '0', 0, null, null, null, null, null, null, 'Flink SQL Demo', 0, null, null, null, null, null, null, null, 0, 0, now(), now(), null, 1, 1, null, null, null, null, null, null, 0, null, null, null, 'streampark,test');

-- ----------------------------
-- Records of t_flink_effective
-- ----------------------------
insert into `t_flink_effective` values (100000, 100000, 2, 100000, now());

-- ----------------------------
-- Records of t_flink_project
-- ----------------------------
insert into `t_flink_project` values (100000, 100000, 'streampark-quickstart', '1', 'https://github.com/apache/incubator-streampark-quickstart', 'dev', null, null, null, null, null, 1, 1, null, 'streampark-quickstart', -1, now(), now());

-- ----------------------------
-- Records of t_flink_sql
-- ----------------------------
insert into `t_flink_sql` values (100000, 100000, 'eNqlUUtPhDAQvu+vmFs1AYIHT5s94AaVqGxSSPZIKgxrY2mxrdGfb4GS3c0+LnJo6Mz36syapkmZQpk8vKbQMMt2KOFmAe5rK4Nf3yhrhCwvA1/TTDaqO61UxmooSprlT1PDGkgKEKpmwvIOjWVdP3W2zpG+JfQFHjfU46xxrVvYZuWztye1khJrqzSBFRCfjUwSYQiqt1xJJvyPcbWJp9WPCXvUoUEn0ZAVufcs0nIUjYn2L4s++YiY75eBLr+2Dnl3GYKTWRyfQKYRRR2XZxXmNvu9yh9GHAmUO/sxyMRkGNly4c714RZ7zaWtLHsX+N9NjvVrWxm99jmyvEhpOUhujmIYFI5zkCOYzYIj11a7QH7Tyz+nE8bw', null, null, 1, 1, now());

-- ----------------------------
-- Records of t_menu
-- ----------------------------
insert into `t_menu` values (110000, 0, 'Apache Flink', '/flink', 'PageView', null, null, '0', 1, 1, now(), now());
insert into `t_menu` values (120000, 0, 'Apache Spark', '/spark', 'PageView', null, null, '0', 1, 2, now(), now());
insert into `t_menu` values (130000, 0, 'menu.resource', '/resource', 'PageView', null, 'github', '0', 1, 3, now(), now());
insert into `t_menu` values (140000, 0, 'menu.setting', '/setting', 'PageView', null, 'setting', '0', 1, 4, now(), now());
insert into `t_menu` values (150000, 0, 'menu.system', '/system', 'PageView', null, 'desktop', '0', 1, 5, now(), now());

insert into `t_menu` values (110100, 110000, 'menu.application', '/flink/app', 'flink/app/View', null, null, '0', 1, 2, now(), now());
insert into `t_menu` values (110200, 110000, 'flink.flinkHome', '/flink/home', 'flink/home/View', null, null, '0', 1, 3, now(), now());
insert into `t_menu` values (110300, 110000, 'flink.flinkCluster', '/flink/cluster', 'flink/cluster/View', 'menu:view', null, '0', 1, 4, now(), now());

insert into `t_menu` values (110101, 110100, 'add', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', 0, null, now(), now());
insert into `t_menu` values (110102, 110100, 'detail app', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', 0, null, now(), now());
insert into `t_menu` values (110103, 110100, 'edit flink', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', 0, null, now(), now());
insert into `t_menu` values (110104, 110100, 'edit streampark', '/flink/app/edit_streampark', 'flink/app/EditStreamPark', 'app:update', '', '0', 0, null, now(), now());
insert into `t_menu` values (110105, 110100, 'mapping', null, null, 'app:mapping', null, '1', 1, null, now(), now());
insert into `t_menu` values (110106, 110100, 'release', null, null, 'app:release', null, '1', 1, null, now(), now());
insert into `t_menu` values (110107, 110100, 'start', null, null, 'app:start', null, '1', 1, null, now(), now());
insert into `t_menu` values (110108, 110100, 'clean', null, null, 'app:clean', null, '1', 1, null, now(), now());
insert into `t_menu` values (110109, 110100, 'cancel', null, null, 'app:cancel', null, '1', 1, null, now(), now());
insert into `t_menu` values (110110, 110100, 'savepoint delete', null, null, 'savepoint:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110111, 110100, 'backup rollback', null, null, 'backup:rollback', null, '1', 1, null, now(), now());
insert into `t_menu` values (110112, 110100, 'backup delete', null, null, 'backup:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110113, 110100, 'conf delete', null, null, 'conf:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110114, 110100, 'delete', null, null, 'app:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (110115, 110100, 'copy', null, null, 'app:copy', null, '1', 1, null, now(), now());
insert into `t_menu` values (110116, 110100, 'view', null, null, 'app:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (110117, 110100, 'savepoint trigger', null, null, 'savepoint:trigger', null, '1', 1, null, now(), now());
insert into `t_menu` values (110118, 110100, 'sql delete', null, null, 'sql:delete', null, '1', 1, null, now(), now());

insert into `t_menu` values (110301, 110300, 'add cluster', '/flink/add_cluster', 'flink/cluster/AddCluster', 'cluster:create', '', '0', 0, null, now(), now());
insert into `t_menu` values (110302, 110300, 'edit cluster', '/flink/edit_cluster', 'flink/cluster/EditCluster', 'cluster:update', '', '0', 0, null, now(), now());

insert into `t_menu` values (130100, 130000, 'resource.project', '/resource/project', 'resource/project/View', null, 'github', '0', 1, 3, now(), now());
insert into `t_menu` values (130200, 130000, 'resource.variable', '/resource/variable', 'resource/variable/View', null, null, '0', 1, 4, now(), now());
insert into `t_menu` values (130300, 130000, 'resource.material', '/resource/resource', 'resource/material/View', null, null, '0', 1, 3, now(), now());

insert into `t_menu` values (130101, 130100, 'add', '/project/add', 'project/Add', 'project:create', '', '0', 0, null, now(), now());
insert into `t_menu` values (130102, 130100, 'build', null, null, 'project:build', null, '1', 1, null, now(), now());
insert into `t_menu` values (130103, 130100, 'delete', null, null, 'project:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (130104, 130100, 'edit', '/project/edit', 'project/Edit', 'project:update', null, '0', 0, null, now(), now());
insert into `t_menu` values (130105, 130100, 'view', null, null, 'project:view', null, '1', 1, null, now(), now());

insert into `t_menu` values (130201, 130200, 'add', NULL, NULL, 'variable:add', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130202, 130200, 'update', NULL, NULL, 'variable:update', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130203, 130200, 'delete', NULL, NULL, 'variable:delete', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130204, 130200, 'depend apps', 'variable/depend_apps', 'variable/DependApps', 'variable:depend_apps', '', '0', 0, NULL, now(), now());
insert into `t_menu` values (130205, 130200, 'show original', NULL, NULL, 'variable:show_original', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130206, 130200, 'view', NULL, NULL, 'variable:view', NULL, '1', 1, null, now(), now());
insert into `t_menu` values (130207, 130200, 'depend view', null, null, 'variable:depend_apps', null, '1', 1, NULL, now(), now());

insert into `t_menu` values (130301, 130300, 'add', NULL, NULL, 'resource:add', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130302, 130300, 'update', NULL, NULL, 'resource:update', NULL, '1', 1, NULL, now(), now());
insert into `t_menu` values (130303, 130300, 'delete', NULL, NULL, 'resource:delete', NULL, '1', 1, NULL, now(), now());

insert into `t_menu` values (140100, 140000, 'setting.system', '/setting/system', 'setting/System/index', null, null, '0', 1, 1, now(), now());
insert into `t_menu` values (140200, 140000, 'setting.alarm', '/setting/alarm', 'setting/Alarm/index', null, null, '0', 1, 2, now(), now());
insert into `t_menu` values (140300, 140000, 'setting.externalLink', '/setting/externalLink', 'setting/ExternalLink/index', 'menu:view', null, '0', 1, 5, now(), now());
insert into `t_menu` values (140400, 140000, 'setting.yarnQueue', '/setting/yarnQueue', 'setting/YarnQueue/index', 'menu:view', null, '0', 1, 6, now(), now());
insert into `t_menu` values (140101, 140100, 'view', null, null, 'setting:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (140102, 140100, 'setting update', null, null, 'setting:update', null, '1', 1, null, now(), now());

insert into `t_menu` values (140301, 140300, 'link view', null, null, 'externalLink:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (140302, 140300, 'link create', null, null, 'externalLink:create', null, '1', 1, null, now(), now());
insert into `t_menu` values (140303, 140300, 'link update', null, null, 'externalLink:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (140304, 140300, 'link delete', null, null, 'externalLink:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (140401, 140400, 'add yarn queue', null, null, 'yarnQueue:create', '', '1', 0, null, now(), now());
insert into `t_menu` values (140402, 140400, 'edit yarn queue', null, null, 'yarnQueue:update', '', '1', 0, null, now(), now());
insert into `t_menu` values (140403, 140400, 'delete yarn queue', null, null, 'yarnQueue:delete', '', '1', 0, null, now(), now());

insert into `t_menu` values (150100, 150000, 'menu.userManagement', '/system/user', 'system/user/User', null, null, '0', 1, 1, now(), now());
insert into `t_menu` values (150200, 150000, 'menu.roleManagement', '/system/role', 'system/role/Role', null, null, '0', 1, 2, now(), now());
insert into `t_menu` values (150300, 150000, 'menu.tokenManagement', '/system/token', 'system/token/Token', null, null, '0', 1, 1, now(), now());
insert into `t_menu` values (150400, 150000, 'menu.teamManagement', '/system/team', 'system/team/Team', null, null, '0', 1, 2, now(), now());
insert into `t_menu` values (150500, 150000, 'menu.memberManagement', '/system/member', 'system/member/Member', null, null, '0', 1, 2, now(), now());

insert into `t_menu` values (150101, 150100, 'add', null, null, 'user:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (150102, 150100, 'update', null, null, 'user:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (150103, 150100, 'delete', null, null, 'user:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (150104, 150100, 'reset', null, null, 'user:reset', null, '1', 1, null, now(), now());
insert into `t_menu` values (150105, 150100, 'types', null, null, 'user:types', null, '1', 1, null, now(), now());
insert into `t_menu` values (150106, 150100, 'view', null, null, 'user:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (150201, 150200, 'add', null, null, 'role:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (150202, 150200, 'update', null, null, 'role:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (150203, 150200, 'delete', null, null, 'role:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (150204, 150200, 'view', null, null, 'role:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (150301, 150300, 'add', null, null, 'token:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (150302, 150300, 'delete', null, null, 'token:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (150303, 150300, 'view', null, null, 'token:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (150401, 150400, 'add', null, null, 'team:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (150402, 150400, 'update', null, null, 'team:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (150403, 150400, 'delete', null, null, 'team:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (150404, 150400, 'view', null, null, 'team:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (150501, 150500, 'add', null, null, 'member:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (150502, 150500, 'update', null, null, 'member:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (150503, 150500, 'delete', null, null, 'member:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (150504, 150500, 'role view', null, null, 'role:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (150505, 150500, 'view', null, null, 'member:view', null, '1', 1, null, now(), now());

-- ----------------------------
-- Records of t_role
-- ----------------------------
insert into `t_role` values (100001, 'developer', now(), now(), 'developer');
insert into `t_role` values (100002, 'team admin', now(), now(), 'Team Admin has all permissions inside the team.');

-- ----------------------------
-- Records of t_role_menu
-- ----------------------------
insert into `t_role_menu` (role_id, menu_id) values (100002, 150000);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110600);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110601);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110602);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110603);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110604);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110605);
insert into `t_role_menu` (role_id, menu_id) values (100002, 110000);
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
insert into `t_role_menu` (role_id, menu_id) values (100002, 130200);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120301);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120302);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120303);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120304);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120305);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120306);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120307);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130300);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120401);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120402);
insert into `t_role_menu` (role_id, menu_id) values (100002, 120403);
insert into `t_role_menu` (role_id, menu_id) values (100002, 140000);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130100);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130101);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130200);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130300);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130400);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130401);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130402);
insert into `t_role_menu` (role_id, menu_id) values (100002, 140300);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130501);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130502);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130503);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130504);
insert into `t_role_menu` (role_id, menu_id) values (100002, 140400);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130601);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130602);
insert into `t_role_menu` (role_id, menu_id) values (100002, 130603);

-- ----------------------------
-- Records of t_setting
-- ----------------------------
insert into `t_setting` values (1, 'streampark.maven.settings', null, 'Maven Settings File Path', 'Maven Settings.xml full path', 1);
insert into `t_setting` values (2, 'streampark.maven.central.repository', null, 'Maven Central Repository', 'Maven private server address', 1);
insert into `t_setting` values (3, 'streampark.maven.auth.user', null, 'Maven Central Repository Auth User', 'Maven private server authentication username', 1);
insert into `t_setting` values (4, 'streampark.maven.auth.password', null, 'Maven Central Repository Auth Password', 'Maven private server authentication password', 1);
insert into `t_setting` values (5, 'alert.email.host', null, 'Alert Email Smtp Host', 'Alert Mailbox Smtp Host', 1);
insert into `t_setting` values (6, 'alert.email.port', null, 'Alert Email Smtp Port', 'Smtp Port of the alarm mailbox', 1);
insert into `t_setting` values (7, 'alert.email.from', null, 'Alert  Email From', 'Email to send alerts', 1);
insert into `t_setting` values (8, 'alert.email.userName', null, 'Alert  Email User', 'Authentication username used to send alert emails', 1);
insert into `t_setting` values (9, 'alert.email.password', null, 'Alert Email Password', 'Authentication password used to send alarm email', 1);
insert into `t_setting` values (10, 'alert.email.ssl', 'false', 'Alert Email Is SSL', 'Whether to enable SSL in the mailbox that sends the alert', 2);
insert into `t_setting` values (11, 'docker.register.address', null, 'Docker Register Address', 'Docker container service address', 1);
insert into `t_setting` values (12, 'docker.register.user', null, 'Docker Register User', 'Docker container service authentication username', 1);
insert into `t_setting` values (13, 'docker.register.password', null, 'Docker Register Password', 'Docker container service authentication password', 1);
insert into `t_setting` values (14, 'docker.register.namespace', null, 'Namespace for docker image used in docker building env and target image register', 'Docker namespace', 1);
insert into `t_setting` values (15, 'ingress.mode.default', null, 'Automatically generate an nginx-based ingress by passing in a domain name', 'Ingress domain address', 1);

-- ----------------------------
-- Records of t_user
-- ----------------------------
insert into `t_user` values (100000, 'admin', '', 'rh8b1ojwog777yrg0daesf04gk', '2513f3748847298ea324dffbf67fe68681dd92315bda830065facd8efe08f54f', null, 1, 0, null, '1', now(), now(),null,'0',null,null);
insert into `t_user` values (100001, 'test1', '', 'rh8b1ojwog777yrg0daesf04gk', '2513f3748847298ea324dffbf67fe68681dd92315bda830065facd8efe08f54f', null, 2, 0, null, '1', now(), now(),null,'0',null,null);
insert into `t_user` values (100002, 'test2', '', 'rh8b1ojwog777yrg0daesf04gk', '2513f3748847298ea324dffbf67fe68681dd92315bda830065facd8efe08f54f', null, 2, 0, null, '1', now(), now(),null,'0',null,null);
insert into `t_user` values (100003, 'test3', '', 'rh8b1ojwog777yrg0daesf04gk', '2513f3748847298ea324dffbf67fe68681dd92315bda830065facd8efe08f54f', null, 2, 0, null, '1', now(), now(),null,'0',null,null);

-- ----------------------------
-- Records of t_member
-- ----------------------------
insert into `t_member` values (100000, 100000, 100001, 100001, now(), now()); -- test_user1 is the developer of the default team
insert into `t_member` values (100001, 100001, 100001, 100002, now(), now()); -- test_user1 is the team admin of the test team
insert into `t_member` values (100002, 100000, 100002, 100001, now(), now()); -- test_user2 is the developer of the test team
insert into `t_member` values (100003, 100001, 100003, 100001, now(), now()); -- test_user3 is the developer of the test team
insert into `t_member` values (100004, 100000, 100000, 100001, now(), now()); -- admin is the developer of the default team
