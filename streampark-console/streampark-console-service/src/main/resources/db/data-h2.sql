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
-- Records of t_flink_app
-- ----------------------------
insert into `t_flink_app` values (100000, 2, 4, null, null, 'Flink SQL Demo', null, null, null, null, null, null , null, 100000, null, 1, null, null, null, null, null, null, '0', 0, null, null, null, null, null, null, 'Flink SQL Demo', 0, null, 0, null, null, null, null, null, null, 0, 0, now(), now(), null, 1, 1, null, null, null, null, null, null, 0, null, null, null, 'streampark,test');

-- ----------------------------
-- Records of t_flink_effective
-- ----------------------------
insert into `t_flink_effective` values (100000, 100000, 2, 100000, now());

-- ----------------------------
-- Records of t_flink_project
-- ----------------------------
insert into `t_flink_project` values (100000, 'streampark-quickstart', 'https://github.com/streamxhub/streampark-quickstart.git', 'main', null, null, null, null, 1, 1, null, 'streampark-quickstart', 1, now(), null);

-- ----------------------------
-- Records of t_flink_sql
-- ----------------------------
insert into `t_flink_sql` values (100000, 100000, 'eNqlUUtPhDAQvu+vmFs1AYIHT5s94AaVqGxSSPZIKgxrY2mxrdGfb4GS3c0+LnJo6Mz36syapkmZQpk8vKbQMMt2KOFmAe5rK4Nf3yhrhCwvA1/TTDaqO61UxmooSprlT1PDGkgKEKpmwvIOjWVdP3W2zpG+JfQFHjfU46xxrVvYZuWztye1khJrqzSBFRCfjUwSYQiqt1xJJvyPcbWJp9WPCXvUoUEn0ZAVufcs0nIUjYn2L4s++YiY75eBLr+2Dnl3GYKTWRyfQKYRRR2XZxXmNvu9yh9GHAmUO/sxyMRkGNly4c714RZ7zaWtLHsX+N9NjvVrWxm99jmyvEhpOUhujmIYFI5zkCOYzYIj11a7QH7Tyz+nE8bw', null, 1, 1, now());

-- ----------------------------
-- Records of t_menu
-- ----------------------------
insert into `t_menu` values (100000, 0, 'System', '/system', 'PageView', null, 'desktop', '0', '1', 1, now(), null);
insert into `t_menu` values (100001, 100000, 'User Management', '/system/user', 'system/user/User', 'user:view', 'user', '0', '1', 1, now(), null);
insert into `t_menu` values (100002, 100000, 'Role Management', '/system/role', 'system/role/Role', 'role:view', 'smile', '0', '1', 2, now(), null);
insert into `t_menu` values (100003, 100000, 'Router Management', '/system/menu', 'system/menu/Menu', 'menu:view', 'bars', '0', '1', 3, now(), null);
insert into `t_menu` values (100004, 100001, 'add', null, null, 'user:add', null, '1', '1', null, now(), null);
insert into `t_menu` values (100005, 100001, 'update', null, null, 'user:update', null, '1', '1', null, now(), null);
insert into `t_menu` values (100006, 100001, 'delete', null, null, 'user:delete', null, '1', '1', null, now(), null);
insert into `t_menu` values (100007, 100002, 'add', null, null, 'role:add', null, '1', '1', null, now(), null);
insert into `t_menu` values (100008, 100002, 'update', null, null, 'role:update', null, '1', '1', null, now(), null);
insert into `t_menu` values (100009, 100002, 'delete', null, null, 'role:delete', null, '1', '1', null, now(), null);
insert into `t_menu` values (100010, 100003, 'add', null, null, 'menu:add', null, '1', '1', null, now(), null);
insert into `t_menu` values (100011, 100003, 'update', null, null, 'menu:update', null, '1', '1', null, now(), null);
insert into `t_menu` values (100012, 100001, 'reset', null, null, 'user:reset', null, '1', '1', null, now(), null);
insert into `t_menu` values (100013, 0, 'StreamPark', '/flink', 'PageView', null, 'build', '0', '1', 2, now(), null);
insert into `t_menu` values (100014, 100013, 'Project', '/flink/project', 'flink/project/View', 'project:view', 'github', '0', '1', 1, now(), null);
insert into `t_menu` values (100015, 100013, 'Application', '/flink/app', 'flink/app/View', 'app:view', 'mobile', '0', '1', 2, now(), null);
insert into `t_menu` values (100016, 100013, 'Add Application', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', '0', null, now(), null);
insert into `t_menu` values (100017, 100013, 'Add Project', '/flink/project/add', 'flink/project/Add', 'project:create', '', '0', '0', null, now(), null);
insert into `t_menu` values (100018, 100013, 'App Detail', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', '0', null, now(), null);
insert into `t_menu` values (100019, 100013, 'Notebook', '/flink/notebook/view', 'flink/notebook/Submit', 'notebook:submit', 'read', '0', '1', 3, now(), null);
insert into `t_menu` values (100020, 100013, 'Edit Flink App', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', '0', null, now(), null);
insert into `t_menu` values (100021, 100013, 'Edit StreamPark App', '/flink/app/edit_streampark', 'flink/app/EditStreamPark', 'app:update', '', '0', '0', null, now(), null);
insert into `t_menu` values (100022, 100014, 'build', null, null, 'project:build', null, '1', '1', null, now(), null);
insert into `t_menu` values (100023, 100014, 'delete', null, null, 'project:delete', null, '1', '1', null, now(), null);
insert into `t_menu` values (100024, 100015, 'mapping', null, null, 'app:mapping', null, '1', '1', null, now(), null);
insert into `t_menu` values (100025, 100015, 'launch', null, null, 'app:launch', null, '1', '1', null, now(), null);
insert into `t_menu` values (100026, 100015, 'start', null, null, 'app:start', null, '1', '1', null, now(), null);
insert into `t_menu` values (100027, 100015, 'clean', null, null, 'app:clean', null, '1', '1', null, now(), null);
insert into `t_menu` values (100028, 100015, 'cancel', null, null, 'app:cancel', null, '1', '1', null, now(), null);
insert into `t_menu` values (100029, 100015, 'savepoint delete', null, null, 'savepoint:delete', null, '1', '1', null, now(), null);
insert into `t_menu` values (100030, 100015, 'backup rollback', null, null, 'backup:rollback', null, '1', '1', null, now(), null);
insert into `t_menu` values (100031, 100015, 'backup delete', null, null, 'backup:delete', null, '1', '1', null, now(), null);
insert into `t_menu` values (100032, 100015, 'conf delete', null, null, 'conf:delete', null, '1', '1', null, now(), null);
insert into `t_menu` values (100033, 100015, 'flame Graph', null, null, 'app:flameGraph', null, '1', '1', null, now(), null);
insert into `t_menu` values (100034, 100013, 'Setting', '/flink/setting', 'flink/setting/View', 'setting:view', 'setting', '0', '1', 4, now(), null);
insert into `t_menu` values (100035, 100034, 'Setting Update', null, null, 'setting:update', null, '1', '1', null, now(), null);
insert into `t_menu` values (100036, 100013, 'Edit Project', '/flink/project/edit', 'flink/project/Edit', 'project:update', null, '0', '0', null, now(), now());
insert into `t_menu` values (100037, 100015, 'delete', null, null, 'app:delete', null, '1', '1', null, now(), null);
insert into `t_menu` values (100038, 100000, 'Token Management', '/system/token', 'system/token/Token', 'token:view', 'lock', '0', '1', 1.0, now(), now());
insert into `t_menu` values (100039, 100038, 'add', null, null, 'token:add', null, '1', '1', null, now(), null);
insert into `t_menu` values (100040, 100038, 'delete', null, null, 'token:delete', null, '1', '1', null, now(), null);
insert into `t_menu` values (100041, 100013, 'Add Cluster', '/flink/setting/add_cluster', 'flink/setting/AddCluster', 'cluster:create', '', '0', '0', null, now(), now());
insert into `t_menu` values (100042, 100013, 'Edit Cluster', '/flink/setting/edit_cluster', 'flink/setting/EditCluster', 'cluster:update', '', '0', '0', null, now(), now());
insert into `t_menu` values (100043, 100015, 'copy', null, null, 'app:copy', null, '1', '1', null, now(), null);


-- ----------------------------
-- Records of t_role
-- ----------------------------
insert into `t_role` values (100000, 'admin', 'admin', now(), null, null);
insert into `t_role` values (100001, 'developer', 'developer', now(), null, null);

-- ----------------------------
-- Records of t_role_menu
-- ----------------------------
insert into `t_role_menu` values (100000, 100000, 100000);
insert into `t_role_menu` values (100001, 100000, 100001);
insert into `t_role_menu` values (100002, 100000, 100002);
insert into `t_role_menu` values (100003, 100000, 100003);
insert into `t_role_menu` values (100004, 100000, 100004);
insert into `t_role_menu` values (100005, 100000, 100005);
insert into `t_role_menu` values (100006, 100000, 100006);
insert into `t_role_menu` values (100007, 100000, 100007);
insert into `t_role_menu` values (100008, 100000, 100008);
insert into `t_role_menu` values (100009, 100000, 100009);
insert into `t_role_menu` values (100010, 100000, 100010);
insert into `t_role_menu` values (100011, 100000, 100011);
insert into `t_role_menu` values (100012, 100000, 100012);
insert into `t_role_menu` values (100013, 100000, 100013);
insert into `t_role_menu` values (100014, 100000, 100014);
insert into `t_role_menu` values (100015, 100000, 100015);
insert into `t_role_menu` values (100016, 100000, 100016);
insert into `t_role_menu` values (100017, 100000, 100017);
insert into `t_role_menu` values (100018, 100000, 100018);
insert into `t_role_menu` values (100019, 100000, 100019);
insert into `t_role_menu` values (100020, 100000, 100020);
insert into `t_role_menu` values (100021, 100000, 100021);
insert into `t_role_menu` values (100022, 100000, 100022);
insert into `t_role_menu` values (100023, 100000, 100023);
insert into `t_role_menu` values (100024, 100000, 100024);
insert into `t_role_menu` values (100025, 100000, 100025);
insert into `t_role_menu` values (100026, 100000, 100026);
insert into `t_role_menu` values (100027, 100000, 100027);
insert into `t_role_menu` values (100028, 100000, 100028);
insert into `t_role_menu` values (100029, 100000, 100029);
insert into `t_role_menu` values (100030, 100000, 100030);
insert into `t_role_menu` values (100031, 100000, 100031);
insert into `t_role_menu` values (100032, 100000, 100032);
insert into `t_role_menu` values (100033, 100000, 100033);
insert into `t_role_menu` values (100034, 100000, 100034);
insert into `t_role_menu` values (100035, 100000, 100035);
insert into `t_role_menu` values (100036, 100000, 100036);
insert into `t_role_menu` values (100037, 100000, 100037);
insert into `t_role_menu` values (100038, 100000, 100038);
insert into `t_role_menu` values (100039, 100000, 100039);
insert into `t_role_menu` values (100040, 100000, 100040);
insert into `t_role_menu` values (100041, 100001, 100014);
insert into `t_role_menu` values (100042, 100001, 100016);
insert into `t_role_menu` values (100043, 100001, 100017);
insert into `t_role_menu` values (100044, 100001, 100018);
insert into `t_role_menu` values (100045, 100001, 100019);
insert into `t_role_menu` values (100046, 100001, 100020);
insert into `t_role_menu` values (100047, 100001, 100021);
insert into `t_role_menu` values (100048, 100001, 100022);
insert into `t_role_menu` values (100049, 100001, 100025);
insert into `t_role_menu` values (100050, 100001, 100026);
insert into `t_role_menu` values (100051, 100001, 100027);
insert into `t_role_menu` values (100052, 100001, 100028);
insert into `t_role_menu` values (100053, 100001, 100029);
insert into `t_role_menu` values (100054, 100001, 100030);
insert into `t_role_menu` values (100055, 100001, 100031);
insert into `t_role_menu` values (100056, 100001, 100032);
insert into `t_role_menu` values (100057, 100001, 100033);
insert into `t_role_menu` values (100058, 100001, 100013);
insert into `t_role_menu` values (100059, 100001, 100015);
insert into `t_role_menu` values (100060, 100000, 100041);
insert into `t_role_menu` values (100061, 100000, 100042);
insert into `t_role_menu` values (100062, 100000, 100043);

-- ----------------------------
-- Records of t_setting
-- ----------------------------
insert into `t_setting` values (1, 'streampark.maven.central.repository', null, 'Maven Central Repository', 'Maven 私服地址', 1);
insert into `t_setting` values (2, 'streampark.maven.auth.user', null, 'Maven Central Repository Auth User', 'Maven 私服认证用户名', 1);
insert into `t_setting` values (3, 'streampark.maven.auth.password', null, 'Maven Central Repository Auth Password', 'Maven 私服认证密码', 1);
insert into `t_setting` values (4, 'streampark.console.webapp.address', null, 'StreamPark Webapp address', 'StreamPark Console Web 应用程序HTTP URL', 1);
insert into `t_setting` values (5, 'alert.email.host', null, 'Alert Email Smtp Host', '告警邮箱Smtp Host', 1);
insert into `t_setting` values (6, 'alert.email.port', null, 'Alert Email Smtp Port', '告警邮箱的Smtp Port', 1);
insert into `t_setting` values (7, 'alert.email.from', null, 'Alert  Email From', '发送告警的邮箱', 1);
insert into `t_setting` values (8, 'alert.email.userName', null, 'Alert  Email User', '用来发送告警邮箱的认证用户名', 1);
insert into `t_setting` values (9, 'alert.email.password', null, 'Alert Email Password', '用来发送告警邮箱的认证密码', 1);
insert into `t_setting` values (10, 'alert.email.ssl', 'false', 'Alert Email Is SSL', '发送告警的邮箱是否开启SSL', 2);
insert into `t_setting` values (11, 'docker.register.address', null, 'Docker Register Address', 'Docker容器服务地址', 1);
insert into `t_setting` values (12, 'docker.register.user', null, 'Docker Register User', 'Docker容器服务认证用户名', 1);
insert into `t_setting` values (13, 'docker.register.password', null, 'Docker Register Password', 'Docker容器服务认证密码', 1);
insert into `t_setting` values (14, 'docker.register.namespace', null, 'Namespace for docker image used in docker building env and target image register', 'Docker命名空间', 1);

-- ----------------------------
-- Records of t_user
-- ----------------------------
insert into `t_user` values (100000, 'admin', '', 'ats6sdxdqf8vsqjtz0utj461wr', '829b009a6b9cc8ea486a4abbc38e56529f3c6f4c9c6fcd3604b41b1d6eca1a57', null, '1', now(), null,null,0,null,null );

-- ----------------------------
-- Records of t_user_role
-- ----------------------------
insert into `t_user_role` values (100000, 100000, 100000);

