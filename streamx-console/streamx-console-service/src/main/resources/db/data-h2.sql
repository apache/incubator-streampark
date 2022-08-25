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

-- ----------------------------
-- Records of t_flink_app
-- ----------------------------
INSERT INTO `t_flink_app` VALUES (100000, 2, 4, NULL, NULL, 'Flink SQL Demo', NULL, NULL, NULL, NULL, NULL, NULL , NULL, 100000, NULL, 1, NULL, NULL, NULL, NULL, NULL, NULL, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, 'Flink SQL Demo', 0, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, NOW(), NOW(), NULL, 1, 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL,1);

-- ----------------------------
-- Records of t_flink_effective
-- ----------------------------
INSERT INTO `t_flink_effective` VALUES (100000, 100000, 2, 100000, NOW());

-- ----------------------------
-- Records of t_flink_project
-- ----------------------------
INSERT INTO `t_flink_project` VALUES (100000, 'streamx-quickstart', 'https://github.com/streamxhub/streamx-quickstart.git', 'main', NULL, NULL, NULL, NULL, 1, 1, NOW(), NULL, 'streamx-quickstart', 1,1);

-- ----------------------------
-- Records of t_flink_sql
-- ----------------------------
INSERT INTO `t_flink_sql` VALUES (100000, 100000, 'eNqlUUtPhDAQvu+vmFs1AYIHT5s94AaVqGxSSPZIKgxrY2mxrdGfb4GS3c0+LnJo6Mz36syapkmZQpk8vKbQMMt2KOFmAe5rK4Nf3yhrhCwvA1/TTDaqO61UxmooSprlT1PDGkgKEKpmwvIOjWVdP3W2zpG+JfQFHjfU46xxrVvYZuWztye1khJrqzSBFRCfjUwSYQiqt1xJJvyPcbWJp9WPCXvUoUEn0ZAVufcs0nIUjYn2L4s++YiY75eBLr+2Dnl3GYKTWRyfQKYRRR2XZxXmNvu9yh9GHAmUO/sxyMRkGNly4c714RZ7zaWtLHsX+N9NjvVrWxm99jmyvEhpOUhujmIYFI5zkCOYzYIj11a7QH7Tyz+nE8bw', NULL, 1, 1, NOW());

-- ----------------------------
-- Records of t_menu
-- ----------------------------
INSERT INTO `t_menu` VALUES (100000, 0, 'System', '/system', 'PageView', NULL, 'desktop', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100001, 100000, 'User Management', '/system/user', 'system/user/User', 'user:view', 'user', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100002, 100000, 'Role Management', '/system/role', 'system/role/Role', 'role:view', 'smile', '0', '1', 2, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100003, 100000, 'Router Management', '/system/menu', 'system/menu/Menu', 'menu:view', 'bars', '0', '1', 3, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100004, 100001, 'add', NULL, NULL, 'user:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100005, 100001, 'update', NULL, NULL, 'user:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100006, 100001, 'delete', NULL, NULL, 'user:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100007, 100002, 'add', NULL, NULL, 'role:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100008, 100002, 'update', NULL, NULL, 'role:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100009, 100002, 'delete', NULL, NULL, 'role:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100010, 100003, 'add', NULL, NULL, 'menu:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100011, 100003, 'update', NULL, NULL, 'menu:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100012, 100001, 'reset', NULL, NULL, 'user:reset', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100013, 0, 'StreamX', '/flink', 'PageView', NULL, 'build', '0', '1', 2, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100014, 100013, 'Project', '/flink/project', 'flink/project/View', 'project:view', 'github', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100015, 100013, 'Application', '/flink/app', 'flink/app/View', 'app:view', 'mobile', '0', '1', 2, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100016, 100013, 'Add Application', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100017, 100013, 'Add Project', '/flink/project/add', 'flink/project/Add', 'project:create', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100018, 100013, 'App Detail', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100019, 100013, 'Notebook', '/flink/notebook/view', 'flink/notebook/Submit', 'notebook:submit', 'read', '0', '1', 3, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100020, 100013, 'Edit Flink App', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100021, 100013, 'Edit StreamX App', '/flink/app/edit_streamx', 'flink/app/EditStreamX', 'app:update', '', '0', '0', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100022, 100014, 'build', NULL, NULL, 'project:build', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100023, 100014, 'delete', NULL, NULL, 'project:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100024, 100015, 'mapping', NULL, NULL, 'app:mapping', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100025, 100015, 'launch', NULL, NULL, 'app:launch', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100026, 100015, 'start', NULL, NULL, 'app:start', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100027, 100015, 'clean', NULL, NULL, 'app:clean', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100028, 100015, 'cancel', NULL, NULL, 'app:cancel', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100029, 100015, 'savepoint delete', NULL, NULL, 'savepoint:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100030, 100015, 'backup rollback', NULL, NULL, 'backup:rollback', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100031, 100015, 'backup delete', NULL, NULL, 'backup:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100032, 100015, 'conf delete', NULL, NULL, 'conf:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100033, 100015, 'flame Graph', NULL, NULL, 'app:flameGraph', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100034, 100013, 'Setting', '/flink/setting', 'flink/setting/View', 'setting:view', 'setting', '0', '1', 4, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100035, 100034, 'Setting Update', NULL, NULL, 'setting:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100036, 100013, 'Edit Project', '/flink/project/edit', 'flink/project/Edit', 'project:update', NULL, '0', '0', NULL, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100037, 100015, 'delete', NULL, NULL, 'app:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100038, 100000, 'Token Management', '/system/token', 'system/token/Token', 'token:view', 'lock', '0', '1', 1.0, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100039, 100038, 'add', NULL, NULL, 'token:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100040, 100038, 'delete', NULL, NULL, 'token:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100041, 100013, 'Add Cluster', '/flink/setting/add_cluster', 'flink/setting/AddCluster', 'cluster:create', '', '0', '0', null, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100042, 100013, 'Edit Cluster', '/flink/setting/edit_cluster', 'flink/setting/EditCluster', 'cluster:update', '', '0', '0', null, NOW(), NOW());
INSERT INTO `t_menu` VALUES (100043, 100000, 'Team Management', '/system/team', 'system/team/Team', 'team:view', 'team', '0', '1', 1, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100044, 100043, 'add', NULL, NULL, 'team:add', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100045, 100043, 'update', NULL, NULL, 'team:update', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100046, 100043, 'delete', NULL, NULL, 'team:delete', NULL, '1', '1', NULL, NOW(), NULL);
INSERT INTO `t_menu` VALUES (100047, 100015, 'copy', null, null, 'app:copy', null, '1', '1', null, NOW(), null);


-- ----------------------------
-- Records of t_role
-- ----------------------------
INSERT INTO `t_role` VALUES (100000, 'admin', 'admin', NOW(), NULL, NULL);
INSERT INTO `t_role` VALUES (100001, 'developer', 'developer', NOW(), NULL, NULL);


-- ----------------------------
-- Records of t_role_menu
-- ----------------------------
INSERT INTO `t_role_menu` VALUES (100000, 100000, 100000);
INSERT INTO `t_role_menu` VALUES (100001, 100000, 100001);
INSERT INTO `t_role_menu` VALUES (100002, 100000, 100002);
INSERT INTO `t_role_menu` VALUES (100003, 100000, 100003);
INSERT INTO `t_role_menu` VALUES (100004, 100000, 100004);
INSERT INTO `t_role_menu` VALUES (100005, 100000, 100005);
INSERT INTO `t_role_menu` VALUES (100006, 100000, 100006);
INSERT INTO `t_role_menu` VALUES (100007, 100000, 100007);
INSERT INTO `t_role_menu` VALUES (100008, 100000, 100008);
INSERT INTO `t_role_menu` VALUES (100009, 100000, 100009);
INSERT INTO `t_role_menu` VALUES (100010, 100000, 100010);
INSERT INTO `t_role_menu` VALUES (100011, 100000, 100011);
INSERT INTO `t_role_menu` VALUES (100012, 100000, 100012);
INSERT INTO `t_role_menu` VALUES (100013, 100000, 100013);
INSERT INTO `t_role_menu` VALUES (100014, 100000, 100014);
INSERT INTO `t_role_menu` VALUES (100015, 100000, 100015);
INSERT INTO `t_role_menu` VALUES (100016, 100000, 100016);
INSERT INTO `t_role_menu` VALUES (100017, 100000, 100017);
INSERT INTO `t_role_menu` VALUES (100018, 100000, 100018);
INSERT INTO `t_role_menu` VALUES (100019, 100000, 100019);
INSERT INTO `t_role_menu` VALUES (100020, 100000, 100020);
INSERT INTO `t_role_menu` VALUES (100021, 100000, 100021);
INSERT INTO `t_role_menu` VALUES (100022, 100000, 100022);
INSERT INTO `t_role_menu` VALUES (100023, 100000, 100023);
INSERT INTO `t_role_menu` VALUES (100024, 100000, 100024);
INSERT INTO `t_role_menu` VALUES (100025, 100000, 100025);
INSERT INTO `t_role_menu` VALUES (100026, 100000, 100026);
INSERT INTO `t_role_menu` VALUES (100027, 100000, 100027);
INSERT INTO `t_role_menu` VALUES (100028, 100000, 100028);
INSERT INTO `t_role_menu` VALUES (100029, 100000, 100029);
INSERT INTO `t_role_menu` VALUES (100030, 100000, 100030);
INSERT INTO `t_role_menu` VALUES (100031, 100000, 100031);
INSERT INTO `t_role_menu` VALUES (100032, 100000, 100032);
INSERT INTO `t_role_menu` VALUES (100033, 100000, 100033);
INSERT INTO `t_role_menu` VALUES (100034, 100000, 100034);
INSERT INTO `t_role_menu` VALUES (100035, 100000, 100035);
INSERT INTO `t_role_menu` VALUES (100036, 100000, 100036);
INSERT INTO `t_role_menu` VALUES (100037, 100000, 100037);
INSERT INTO `t_role_menu` VALUES (100038, 100000, 100038);
INSERT INTO `t_role_menu` VALUES (100039, 100000, 100039);
INSERT INTO `t_role_menu` VALUES (100040, 100000, 100040);
INSERT INTO `t_role_menu` VALUES (100041, 100001, 100014);
INSERT INTO `t_role_menu` VALUES (100042, 100001, 100016);
INSERT INTO `t_role_menu` VALUES (100043, 100001, 100017);
INSERT INTO `t_role_menu` VALUES (100044, 100001, 100018);
INSERT INTO `t_role_menu` VALUES (100045, 100001, 100019);
INSERT INTO `t_role_menu` VALUES (100046, 100001, 100020);
INSERT INTO `t_role_menu` VALUES (100047, 100001, 100021);
INSERT INTO `t_role_menu` VALUES (100048, 100001, 100022);
INSERT INTO `t_role_menu` VALUES (100049, 100001, 100025);
INSERT INTO `t_role_menu` VALUES (100050, 100001, 100026);
INSERT INTO `t_role_menu` VALUES (100051, 100001, 100027);
INSERT INTO `t_role_menu` VALUES (100052, 100001, 100028);
INSERT INTO `t_role_menu` VALUES (100053, 100001, 100029);
INSERT INTO `t_role_menu` VALUES (100054, 100001, 100030);
INSERT INTO `t_role_menu` VALUES (100055, 100001, 100031);
INSERT INTO `t_role_menu` VALUES (100056, 100001, 100032);
INSERT INTO `t_role_menu` VALUES (100057, 100001, 100033);
INSERT INTO `t_role_menu` VALUES (100058, 100001, 100013);
INSERT INTO `t_role_menu` VALUES (100059, 100001, 100015);
INSERT INTO `t_role_menu` VALUES (100060, 100000, 100041);
INSERT INTO `t_role_menu` VALUES (100061, 100000, 100042);
INSERT INTO `t_role_menu` VALUES (100062, 100000, 100043);
INSERT INTO `t_role_menu` VALUES (100063, 100000, 100044);
INSERT INTO `t_role_menu` VALUES (100064, 100000, 100045);
INSERT INTO `t_role_menu` VALUES (100065, 100000, 100046);
INSERT INTO `t_role_menu` VALUES (100066, 100000, 100047);


-- ----------------------------
-- Records of t_setting
-- ----------------------------
INSERT INTO `t_setting` VALUES (1, 'streamx.maven.central.repository', NULL, 'Maven Central Repository', 'Maven 私服地址', 1);
INSERT INTO `t_setting` VALUES (2, 'streamx.maven.auth.user', NULL, 'Maven Central Repository Auth User', 'Maven 私服认证用户名', 1);
INSERT INTO `t_setting` VALUES (3, 'streamx.maven.auth.password', NULL, 'Maven Central Repository Auth Password', 'Maven 私服认证密码', 1);
INSERT INTO `t_setting` VALUES (4, 'streamx.console.webapp.address', NULL, 'StreamX Webapp address', 'StreamX Console Web 应用程序HTTP URL', 1);
INSERT INTO `t_setting` VALUES (5, 'alert.email.host', NULL, 'Alert Email Smtp Host', '告警邮箱Smtp Host', 1);
INSERT INTO `t_setting` VALUES (6, 'alert.email.port', NULL, 'Alert Email Smtp Port', '告警邮箱的Smtp Port', 1);
INSERT INTO `t_setting` VALUES (7, 'alert.email.from', NULL, 'Alert  Email From', '发送告警的邮箱', 1);
INSERT INTO `t_setting` VALUES (8, 'alert.email.userName', NULL, 'Alert  Email User', '用来发送告警邮箱的认证用户名', 1);
INSERT INTO `t_setting` VALUES (9, 'alert.email.password', NULL, 'Alert Email Password', '用来发送告警邮箱的认证密码', 1);
INSERT INTO `t_setting` VALUES (10, 'alert.email.ssl', 'false', 'Alert Email Is SSL', '发送告警的邮箱是否开启SSL', 2);
INSERT INTO `t_setting` VALUES (11, 'docker.register.address', NULL, 'Docker Register Address', 'Docker容器服务地址', 1);
INSERT INTO `t_setting` VALUES (12, 'docker.register.user', NULL, 'Docker Register User', 'Docker容器服务认证用户名', 1);
INSERT INTO `t_setting` VALUES (13, 'docker.register.password', NULL, 'Docker Register Password', 'Docker容器服务认证密码', 1);
INSERT INTO `t_setting` VALUES (14, 'docker.register.namespace', NULL, 'Namespace for docker image used in docker building env and target image register', 'Docker命名空间', 1);

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES (100000, 'admin', '', 'ats6sdxdqf8vsqjtz0utj461wr', '829b009a6b9cc8ea486a4abbc38e56529f3c6f4c9c6fcd3604b41b1d6eca1a57', NULL, '1', NOW(), NULL,NULL,0,NULL,NULL );

-- ----------------------------
-- Records of t_user_role
-- ----------------------------
INSERT INTO `t_user_role` VALUES (100000, 100000, 100000);

-- Records of t_team
-- ----------------------------
insert into `t_team` values (1,'bigdata','BIGDATA','2022-02-21 18:00:00');
