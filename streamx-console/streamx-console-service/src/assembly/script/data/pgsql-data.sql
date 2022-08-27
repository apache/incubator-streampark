/*
 * copyright 2019 the streamx project
 *
 * licensed under the apache license, version 2.0 (the "license");
 * you may not use this file except in compliance with the license.
 * you may obtain a copy of the license at
 *
 *     http://www.apache.org/licenses/license-2.0
 *
 * unless required by applicable law or agreed to in writing, software
 * distributed under the license is distributed on an "as is" basis,
 * without warranties or conditions of any kind, either express or implied.
 * see the license for the specific language governing permissions and
 * limitations under the license.
 */
-- ----------------------------
-- records of t_flink_app
-- ----------------------------
insert into "public"."t_flink_app" values (100000, 2, 4, null, null, 'flink sql demo', null, null, null, null, null, null, null, 100000, null, 1, null, null, null, null, null, null, '0', 0, null, null, null, null, null, null, 'flink sql demo', 0, null, 0, null, null, null, null, null, null, 0, 0, '2022-08-25 15:16:48', '2022-08-25 15:16:48', null, 1, 1, null, null, null, null, null, null, 0, null, null, null, 1);

-- ----------------------------
-- records of t_flink_effective
-- ----------------------------
insert into "public"."t_flink_effective" values (100000, 100000, 2, 100000, '2022-08-25 15:16:48');

-- ----------------------------
-- records of t_flink_project
-- ----------------------------
insert into "public"."t_flink_project" values (100000, 'streamx-quickstart', 'https://github.com/streamxhub/streamx-quickstart.git', 'main', null, null, null, null, 1, 1, '2022-08-25 15:16:48', null, 'streamx-quickstart', 1, 1);

-- ----------------------------
-- records of t_flink_sql
-- ----------------------------
insert into "public"."t_flink_sql" values (100000, 100000, 'enqluutphdaqvu+vmfs1ayiht5s94aavqgxsspzikgxry2mxrdgfb4gs3c0+lnjo6mz36syapkmzqpk8vkbqmmt2kofmae5rk4nf3yhrhcwva1/ttdaqo61uxmoosprlt1pdgkgkekpmwviojwvdp3w2zpg+jfqfhjfu46xxrvvyzuwztye1khjrqzsbfrcfjuwsyqiqt1xjjvypcbwjp9wpcxvuouen0zavufcs0niujyn2l4s++yiy75eblr+2dnl3gyktwryfqkyrrr2xzxxmnvu9yh9ghamuo/sxymrkgnly4c714rz7zawtlhsx+n9njvvrwxm99jmyvehpouhujmiyfi5zkcoyzyij11a7qh7tyz+ne8bw', null, 1, 1, '2022-08-25 15:16:48');

-- ----------------------------
-- records of t_menu
-- ----------------------------
insert into "public"."t_menu" values (100000, 0, 'system', '/system', 'pageview', null, 'desktop', '0 ', '1 ', 1, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100001, 100000, 'user management', '/system/user', 'system/user/user', 'user:view', 'user', '0 ', '1 ', 1, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100002, 100000, 'role management', '/system/role', 'system/role/role', 'role:view', 'smile', '0 ', '1 ', 2, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100003, 100000, 'router management', '/system/menu', 'system/menu/menu', 'menu:view', 'bars', '0 ', '1 ', 3, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100004, 100001, 'add', null, null, 'user:add', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100005, 100001, 'update', null, null, 'user:update', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100006, 100001, 'delete', null, null, 'user:delete', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100007, 100002, 'add', null, null, 'role:add', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100008, 100002, 'update', null, null, 'role:update', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100009, 100002, 'delete', null, null, 'role:delete', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100010, 100003, 'add', null, null, 'menu:add', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100011, 100003, 'update', null, null, 'menu:update', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100012, 100001, 'reset', null, null, 'user:reset', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100013, 0, 'streamx', '/flink', 'pageview', null, 'build', '0 ', '1 ', 2, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100014, 100013, 'project', '/flink/project', 'flink/project/view', 'project:view', 'github', '0 ', '1 ', 1, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100015, 100013, 'application', '/flink/app', 'flink/app/view', 'app:view', 'mobile', '0 ', '1 ', 2, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100016, 100013, 'add application', '/flink/app/add', 'flink/app/add', 'app:create', '', '0 ', '0 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100017, 100013, 'add project', '/flink/project/add', 'flink/project/add', 'project:create', '', '0 ', '0 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100018, 100013, 'app detail', '/flink/app/detail', 'flink/app/detail', 'app:detail', '', '0 ', '0 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100019, 100013, 'notebook', '/flink/notebook/view', 'flink/notebook/submit', 'notebook:submit', 'read', '0 ', '1 ', 3, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100020, 100013, 'edit flink app', '/flink/app/edit_flink', 'flink/app/editflink', 'app:update', '', '0 ', '0 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100021, 100013, 'edit streamx app', '/flink/app/edit_streamx', 'flink/app/editstreamx', 'app:update', '', '0 ', '0 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100022, 100014, 'build', null, null, 'project:build', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100023, 100014, 'delete', null, null, 'project:delete', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100024, 100015, 'mapping', null, null, 'app:mapping', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100025, 100015, 'launch', null, null, 'app:launch', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100026, 100015, 'start', null, null, 'app:start', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100027, 100015, 'clean', null, null, 'app:clean', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100028, 100015, 'cancel', null, null, 'app:cancel', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100029, 100015, 'savepoint delete', null, null, 'savepoint:delete', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100030, 100015, 'backup rollback', null, null, 'backup:rollback', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100031, 100015, 'backup delete', null, null, 'backup:delete', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100032, 100015, 'conf delete', null, null, 'conf:delete', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100033, 100015, 'flame graph', null, null, 'app:flamegraph', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100034, 100013, 'setting', '/flink/setting', 'flink/setting/view', 'setting:view', 'setting', '0 ', '1 ', 4, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100035, 100034, 'setting update', null, null, 'setting:update', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100036, 100013, 'edit project', '/flink/project/edit', 'flink/project/edit', 'project:update', null, '0 ', '0 ', null, '2022-08-25 15:16:48', '2022-08-25 15:16:48');
insert into "public"."t_menu" values (100037, 100015, 'delete', null, null, 'app:delete', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100038, 100000, 'token management', '/system/token', 'system/token/token', 'token:view', 'lock', '0 ', '1 ', 1, '2022-08-25 15:16:48', '2022-08-25 15:16:48');
insert into "public"."t_menu" values (100039, 100038, 'add', null, null, 'token:add', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100040, 100038, 'delete', null, null, 'token:delete', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100041, 100013, 'add cluster', '/flink/setting/add_cluster', 'flink/setting/addcluster', 'cluster:create', '', '0 ', '0 ', null, '2022-08-25 15:16:48', '2022-08-25 15:16:48');
insert into "public"."t_menu" values (100042, 100013, 'edit cluster', '/flink/setting/edit_cluster', 'flink/setting/editcluster', 'cluster:update', '', '0 ', '0 ', null, '2022-08-25 15:16:48', '2022-08-25 15:16:48');
insert into "public"."t_menu" values (100043, 100000, 'team management', '/system/team', 'system/team/team', 'team:view', 'team', '0 ', '1 ', 1, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100044, 100043, 'add', null, null, 'team:add', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100045, 100043, 'update', null, null, 'team:update', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100046, 100043, 'delete', null, null, 'team:delete', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" values (100047, 100015, 'copy', null, null, 'app:copy', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);

-- ----------------------------
-- records of t_role
-- ----------------------------
insert into "public"."t_role" values (100000, 'admin', 'admin', '2022-08-25 15:16:48', null, null);
insert into "public"."t_role" values (100001, 'developer', 'developer', '2022-08-25 15:16:48', null, null);

-- ----------------------------
-- records of t_role_menu
-- ----------------------------
insert into "public"."t_role_menu" values (100000, 100000, 100000);
insert into "public"."t_role_menu" values (100001, 100000, 100001);
insert into "public"."t_role_menu" values (100002, 100000, 100002);
insert into "public"."t_role_menu" values (100003, 100000, 100003);
insert into "public"."t_role_menu" values (100004, 100000, 100004);
insert into "public"."t_role_menu" values (100005, 100000, 100005);
insert into "public"."t_role_menu" values (100006, 100000, 100006);
insert into "public"."t_role_menu" values (100007, 100000, 100007);
insert into "public"."t_role_menu" values (100008, 100000, 100008);
insert into "public"."t_role_menu" values (100009, 100000, 100009);
insert into "public"."t_role_menu" values (100010, 100000, 100010);
insert into "public"."t_role_menu" values (100011, 100000, 100011);
insert into "public"."t_role_menu" values (100012, 100000, 100012);
insert into "public"."t_role_menu" values (100013, 100000, 100013);
insert into "public"."t_role_menu" values (100014, 100000, 100014);
insert into "public"."t_role_menu" values (100015, 100000, 100015);
insert into "public"."t_role_menu" values (100016, 100000, 100016);
insert into "public"."t_role_menu" values (100017, 100000, 100017);
insert into "public"."t_role_menu" values (100018, 100000, 100018);
insert into "public"."t_role_menu" values (100019, 100000, 100019);
insert into "public"."t_role_menu" values (100020, 100000, 100020);
insert into "public"."t_role_menu" values (100021, 100000, 100021);
insert into "public"."t_role_menu" values (100022, 100000, 100022);
insert into "public"."t_role_menu" values (100023, 100000, 100023);
insert into "public"."t_role_menu" values (100024, 100000, 100024);
insert into "public"."t_role_menu" values (100025, 100000, 100025);
insert into "public"."t_role_menu" values (100026, 100000, 100026);
insert into "public"."t_role_menu" values (100027, 100000, 100027);
insert into "public"."t_role_menu" values (100028, 100000, 100028);
insert into "public"."t_role_menu" values (100029, 100000, 100029);
insert into "public"."t_role_menu" values (100030, 100000, 100030);
insert into "public"."t_role_menu" values (100031, 100000, 100031);
insert into "public"."t_role_menu" values (100032, 100000, 100032);
insert into "public"."t_role_menu" values (100033, 100000, 100033);
insert into "public"."t_role_menu" values (100034, 100000, 100034);
insert into "public"."t_role_menu" values (100035, 100000, 100035);
insert into "public"."t_role_menu" values (100036, 100000, 100036);
insert into "public"."t_role_menu" values (100037, 100000, 100037);
insert into "public"."t_role_menu" values (100038, 100000, 100038);
insert into "public"."t_role_menu" values (100039, 100000, 100039);
insert into "public"."t_role_menu" values (100040, 100000, 100040);
insert into "public"."t_role_menu" values (100060, 100000, 100041);
insert into "public"."t_role_menu" values (100061, 100000, 100042);
insert into "public"."t_role_menu" values (100062, 100000, 100043);
insert into "public"."t_role_menu" values (100063, 100000, 100044);
insert into "public"."t_role_menu" values (100064, 100000, 100045);
insert into "public"."t_role_menu" values (100065, 100000, 100046);
insert into "public"."t_role_menu" values (100066, 100000, 100047);
insert into "public"."t_role_menu" values (100058, 100001, 100013);
insert into "public"."t_role_menu" values (100041, 100001, 100014);
insert into "public"."t_role_menu" values (100059, 100001, 100015);
insert into "public"."t_role_menu" values (100042, 100001, 100016);
insert into "public"."t_role_menu" values (100043, 100001, 100017);
insert into "public"."t_role_menu" values (100044, 100001, 100018);
insert into "public"."t_role_menu" values (100045, 100001, 100019);
insert into "public"."t_role_menu" values (100046, 100001, 100020);
insert into "public"."t_role_menu" values (100047, 100001, 100021);
insert into "public"."t_role_menu" values (100048, 100001, 100022);
insert into "public"."t_role_menu" values (100049, 100001, 100025);
insert into "public"."t_role_menu" values (100050, 100001, 100026);
insert into "public"."t_role_menu" values (100051, 100001, 100027);
insert into "public"."t_role_menu" values (100052, 100001, 100028);
insert into "public"."t_role_menu" values (100053, 100001, 100029);
insert into "public"."t_role_menu" values (100054, 100001, 100030);
insert into "public"."t_role_menu" values (100055, 100001, 100031);
insert into "public"."t_role_menu" values (100056, 100001, 100032);
insert into "public"."t_role_menu" values (100057, 100001, 100033);

-- ----------------------------
-- records of t_setting
-- ----------------------------
insert into "public"."t_setting" values (7, 'alert.email.from', null, 'alert  email from', '发送告警的邮箱', 1);
insert into "public"."t_setting" values (5, 'alert.email.host', null, 'alert email smtp host', '告警邮箱smtp host', 1);
insert into "public"."t_setting" values (9, 'alert.email.password', null, 'alert email password', '用来发送告警邮箱的认证密码', 1);
insert into "public"."t_setting" values (6, 'alert.email.port', null, 'alert email smtp port', '告警邮箱的smtp port', 1);
insert into "public"."t_setting" values (10, 'alert.email.ssl', 'false', 'alert email is ssl', '发送告警的邮箱是否开启ssl', 2);
insert into "public"."t_setting" values (8, 'alert.email.username', null, 'alert  email user', '用来发送告警邮箱的认证用户名', 1);
insert into "public"."t_setting" values (11, 'docker.register.address', null, 'docker register address', 'docker容器服务地址', 1);
insert into "public"."t_setting" values (14, 'docker.register.namespace', null, 'namespace for docker image used in docker building env and target image register', 'docker命名空间', 1);
insert into "public"."t_setting" values (13, 'docker.register.password', null, 'docker register password', 'docker容器服务认证密码', 1);
insert into "public"."t_setting" values (12, 'docker.register.user', null, 'docker register user', 'docker容器服务认证用户名', 1);
insert into "public"."t_setting" values (4, 'streamx.console.webapp.address', null, 'streamx webapp address', 'streamx console web 应用程序http url', 1);
insert into "public"."t_setting" values (3, 'streamx.maven.auth.password', null, 'maven central repository auth password', 'maven 私服认证密码', 1);
insert into "public"."t_setting" values (2, 'streamx.maven.auth.user', null, 'maven central repository auth user', 'maven 私服认证用户名', 1);
insert into "public"."t_setting" values (1, 'streamx.maven.central.repository', null, 'maven central repository', 'maven 私服地址', 1);

-- ----------------------------
-- records of t_team
-- ----------------------------
insert into "public"."t_team" values (1, 'bigdata', 'bigdata', '2022-02-21 18:00:00');

-- ----------------------------
-- records of t_user
-- ----------------------------
insert into "public"."t_user" values (100000, 'admin', '', 'ats6sdxdqf8vsqjtz0utj461wr', '829b009a6b9cc8ea486a4abbc38e56529f3c6f4c9c6fcd3604b41b1d6eca1a57', null, '1', '2022-08-25 15:16:48', null, null, '0', null, null);

-- ----------------------------
-- records of t_user_role
-- ----------------------------
insert into "public"."t_user_role" values (100000, 100000, 100000);
