SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Records of t_flink_project
-- ----------------------------
INSERT INTO `t_flink_project` VALUES (1, 'streamx-quickstart', 'https://gitee.com/benjobs/streamx-quickstart.git', 'main', NULL, NULL, NULL, 1, 1, '2021-04-08 05:01:02', '2021-04-17 11:47:39', 'streamx-quickstart', 1);

-- ----------------------------
-- Records of t_flink_tutorial
-- ----------------------------
INSERT INTO `t_flink_tutorial` VALUES (1, 1, 'repl', '### Introduction\n\n[Apache Flink](https://flink.apache.org/) is a framework and distributed processing engine for stateful computations over unbounded and bounded data streams. This is Flink tutorial for running classical wordcount in both batch and streaming mode.\n\nThere\'re 3 things you need to do before using flink in StreamX Notebook.\n\n* Download [Flink 1.11](https://flink.apache.org/downloads.html) for scala 2.11 (Only scala-2.11 is supported, scala-2.12 is not supported yet in StreamX Notebook), unpack it and set `FLINK_HOME` in flink interpreter setting to this location.\n* Copy flink-python_2.11–1.11.1.jar from flink opt folder to flink lib folder (it is used by pyflink which is supported)\n* If you want to run yarn mode, you need to set `HADOOP_CONF_DIR` in flink interpreter setting. And make sure `hadoop` is in your `PATH`, because internally flink will call command `hadoop classpath` and put all the hadoop related jars in the classpath of flink interpreter process.\n\nThere\'re 6 sub interpreters in flink interpreter, each is used for different purpose. However they are in the the JVM and share the same ExecutionEnviroment/StremaExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment.\n\n* `flink`	- Creates ExecutionEnvironment/StreamExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment and provides a Scala environment\n* `pyflink`	- Provides a python environment\n* `ipyflink`	- Provides an ipython environment\n* `ssql`	 - Provides a stream sql environment\n* `bsql`	- Provides a batch sql environment\n', '2020-10-22');

-- ----------------------------
-- Records of t_menu
-- ----------------------------
INSERT INTO `t_menu` VALUES (1, 0, 'System', '/system', 'PageView', NULL, 'desktop', '0', '1', 1, '2017-12-27 16:39:07', '2021-02-18 10:45:18');
INSERT INTO `t_menu` VALUES (2, 1, 'User Management', '/system/user', 'system/user/User', 'user:view', 'user', '0', '1', 1, '2017-12-27 16:47:13', '2021-02-09 19:01:36');
INSERT INTO `t_menu` VALUES (3, 1, 'Role Management', '/system/role', 'system/role/Role', 'role:view', 'smile', '0', '1', 2, '2017-12-27 16:48:09', '2021-02-09 19:01:41');
INSERT INTO `t_menu` VALUES (4, 1, 'Router Management', '/system/menu', 'system/menu/Menu', 'menu:view', 'bars', '0', '1', 3, '2017-12-27 16:48:57', '2021-02-09 19:01:47');
INSERT INTO `t_menu` VALUES (5, 2, 'add', '', '', 'user:add', NULL, '1', '1', NULL, '2017-12-27 17:02:58', '2020-10-11 10:38:35');
INSERT INTO `t_menu` VALUES (6, 2, 'update', '', '', 'user:update', NULL, '1', '1', NULL, '2017-12-27 17:04:07', '2020-10-11 10:38:29');
INSERT INTO `t_menu` VALUES (7, 2, 'delete', '', '', 'user:delete', NULL, '1', '1', NULL, '2017-12-27 17:04:58', '2020-10-11 10:38:22');
INSERT INTO `t_menu` VALUES (8, 3, 'add', '', '', 'role:add', NULL, '1', '1', NULL, '2017-12-27 17:06:38', '2020-10-11 10:37:23');
INSERT INTO `t_menu` VALUES (9, 3, 'update', '', '', 'role:update', NULL, '1', '1', NULL, '2017-12-27 17:06:38', '2020-10-11 10:37:32');
INSERT INTO `t_menu` VALUES (10, 3, 'delete', '', '', 'role:delete', NULL, '1', '1', NULL, '2017-12-27 17:06:38', '2020-10-11 10:37:42');
INSERT INTO `t_menu` VALUES (11, 4, 'add', '', '', 'menu:add', NULL, '1', '1', NULL, '2017-12-27 17:08:02', '2020-10-11 10:40:33');
INSERT INTO `t_menu` VALUES (12, 4, 'update', '', '', 'menu:update', NULL, '1', '1', NULL, '2017-12-27 17:08:02', '2020-10-11 10:40:28');
INSERT INTO `t_menu` VALUES (13, 2, 'reset', NULL, NULL, 'user:reset', NULL, '1', '1', NULL, '2019-01-23 06:37:00', '2020-10-11 10:38:07');
INSERT INTO `t_menu` VALUES (14, 0, 'StreamX', '/flink', 'PageView', NULL, 'build', '0', '1', 2, '2019-12-10 10:06:54', '2021-02-18 10:45:26');
INSERT INTO `t_menu` VALUES (15, 14, 'Project', '/flink/project', 'flink/project/View', 'project:view', 'github', '0', '1', 1, '2019-12-10 10:08:30', '2020-09-27 18:33:41');
INSERT INTO `t_menu` VALUES (16, 14, 'Application', '/flink/app', 'flink/app/View', 'app:view', 'mobile', '0', '1', 2, '2019-12-10 10:10:26', '2020-09-27 18:33:47');
INSERT INTO `t_menu` VALUES (17, 14, 'Add Application', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', '0', NULL, '2019-12-11 11:54:24', '2020-10-12 08:05:02');
INSERT INTO `t_menu` VALUES (18, 14, 'Add Project', '/flink/project/add', 'flink/project/Add', 'project:create', '', '0', '0', NULL, '2020-08-04 07:36:00', '2020-10-12 08:04:19');
INSERT INTO `t_menu` VALUES (19, 14, 'App Detail', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', '0', NULL, '2020-08-20 09:56:47', '2020-10-12 08:04:26');
INSERT INTO `t_menu` VALUES (20, 14, 'Notebook', '/flink/notebook/view', 'flink/notebook/Submit', 'notebook:submit', 'read', '0', '1', 3, '2020-09-07 17:10:57', '2020-11-09 15:19:57');
INSERT INTO `t_menu` VALUES (21, 14, 'Edit Flink App', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', '0', NULL, '2020-09-10 16:03:49', '2020-10-12 08:04:51');
INSERT INTO `t_menu` VALUES (22, 14, 'Edit StreamX App', '/flink/app/edit_streamx', 'flink/app/EditStreamX', 'app:update', '', '0', '0', NULL, '2020-09-22 21:11:51', '2020-10-12 08:04:43');
INSERT INTO `t_menu` VALUES (23, 15, 'build', NULL, NULL, 'project:build', NULL, '1', '1', NULL, '2020-09-30 14:18:38', '2020-10-11 10:38:48');
INSERT INTO `t_menu` VALUES (24, 15, 'delete', NULL, NULL, 'project:delete', NULL, '1', '1', NULL, '2020-09-30 14:21:10', '2020-10-11 10:38:57');
INSERT INTO `t_menu` VALUES (25, 16, 'mapping', NULL, NULL, 'app:mapping', NULL, '1', '1', NULL, '2020-09-30 14:33:32', '2020-10-11 10:39:05');
INSERT INTO `t_menu` VALUES (26, 16, 'deploy', NULL, NULL, 'app:deploy', NULL, '1', '1', NULL, '2020-09-30 14:33:49', '2020-10-11 10:39:14');
INSERT INTO `t_menu` VALUES (27, 16, 'start', NULL, NULL, 'app:start', NULL, '1', '1', NULL, '2020-09-30 14:34:05', '2020-10-11 10:39:22');
INSERT INTO `t_menu` VALUES (28, 16, 'clean', NULL, NULL, 'app:clean', NULL, '1', '1', NULL, '2020-09-30 14:34:25', '2020-10-11 10:39:57');
INSERT INTO `t_menu` VALUES (29, 16, 'cancel', NULL, NULL, 'app:cancel', NULL, '1', '1', NULL, '2020-09-30 14:34:40', '2020-11-16 10:32:21');
INSERT INTO `t_menu` VALUES (30, 16, 'savepoint delete', NULL, NULL, 'savepoint:delete', NULL, '1', '1', NULL, '2020-10-17 14:49:29', NULL);
INSERT INTO `t_menu` VALUES (31, 16, 'backup rollback', NULL, NULL, 'backup:rollback', NULL, '1', '1', NULL, '2020-10-17 14:50:05', '2021-02-20 15:55:56');
INSERT INTO `t_menu` VALUES (32, 16, 'backup delete', NULL, NULL, 'backup:delete', NULL, '1', '1', NULL, '2020-10-20 13:49:58', NULL);
INSERT INTO `t_menu` VALUES (33, 16, 'conf delete', NULL, NULL, 'conf:delete', NULL, '1', '1', NULL, '2020-10-20 14:06:39', '2020-10-20 14:15:21');
INSERT INTO `t_menu` VALUES (34, 16, 'flame Graph', NULL, NULL, 'app:flameGraph', NULL, '1', '1', NULL, '2020-12-23 21:59:00', '2020-12-23 22:00:10');
INSERT INTO `t_menu` VALUES (35, 14, 'Setting', '/flink/setting', 'flink/setting/View', 'setting:view', 'setting', '0', '1', 4, '2021-02-08 22:54:41', '2021-02-09 09:32:09');
INSERT INTO `t_menu` VALUES (36, 35, 'Setting Update', NULL, NULL, 'setting:update', NULL, '1', '1', NULL, '2021-02-08 23:17:29', NULL);
-- ----------------------------
-- Records of t_role
-- ----------------------------
INSERT INTO `t_role` VALUES (1, 'admin', 'admin', '2020-12-10 12:00:56', '2021-04-22 09:35:44', 'admin');
INSERT INTO `t_role` VALUES (2, 'developer', 'developer', '2021-04-11 19:02:56', '2021-04-12 00:00:14', NULL);

-- ----------------------------
-- Records of t_role_menu
-- ----------------------------


-- ----------------------------
-- Records of t_role_menu
-- ----------------------------
INSERT INTO `t_role_menu` VALUES (1, 1);
INSERT INTO `t_role_menu` VALUES (1, 2);
INSERT INTO `t_role_menu` VALUES (1, 3);
INSERT INTO `t_role_menu` VALUES (1, 4);
INSERT INTO `t_role_menu` VALUES (1, 5);
INSERT INTO `t_role_menu` VALUES (1, 6);
INSERT INTO `t_role_menu` VALUES (1, 7);
INSERT INTO `t_role_menu` VALUES (1, 8);
INSERT INTO `t_role_menu` VALUES (1, 9);
INSERT INTO `t_role_menu` VALUES (1, 10);
INSERT INTO `t_role_menu` VALUES (1, 11);
INSERT INTO `t_role_menu` VALUES (1, 12);
INSERT INTO `t_role_menu` VALUES (1, 13);
INSERT INTO `t_role_menu` VALUES (1, 14);
INSERT INTO `t_role_menu` VALUES (1, 15);
INSERT INTO `t_role_menu` VALUES (1, 16);
INSERT INTO `t_role_menu` VALUES (1, 17);
INSERT INTO `t_role_menu` VALUES (1, 18);
INSERT INTO `t_role_menu` VALUES (1, 19);
INSERT INTO `t_role_menu` VALUES (1, 20);
INSERT INTO `t_role_menu` VALUES (1, 21);
INSERT INTO `t_role_menu` VALUES (1, 22);
INSERT INTO `t_role_menu` VALUES (1, 23);
INSERT INTO `t_role_menu` VALUES (1, 24);
INSERT INTO `t_role_menu` VALUES (1, 25);
INSERT INTO `t_role_menu` VALUES (1, 26);
INSERT INTO `t_role_menu` VALUES (1, 27);
INSERT INTO `t_role_menu` VALUES (1, 28);
INSERT INTO `t_role_menu` VALUES (1, 29);
INSERT INTO `t_role_menu` VALUES (1, 30);
INSERT INTO `t_role_menu` VALUES (1, 31);
INSERT INTO `t_role_menu` VALUES (1, 32);
INSERT INTO `t_role_menu` VALUES (1, 33);
INSERT INTO `t_role_menu` VALUES (1, 34);
INSERT INTO `t_role_menu` VALUES (1, 35);
INSERT INTO `t_role_menu` VALUES (1, 36);
INSERT INTO `t_role_menu` VALUES (2, 16);
INSERT INTO `t_role_menu` VALUES (2, 17);
INSERT INTO `t_role_menu` VALUES (2, 18);
INSERT INTO `t_role_menu` VALUES (2, 19);
INSERT INTO `t_role_menu` VALUES (2, 20);
INSERT INTO `t_role_menu` VALUES (2, 21);
INSERT INTO `t_role_menu` VALUES (2, 22);
INSERT INTO `t_role_menu` VALUES (2, 25);
INSERT INTO `t_role_menu` VALUES (2, 26);
INSERT INTO `t_role_menu` VALUES (2, 27);
INSERT INTO `t_role_menu` VALUES (2, 28);
INSERT INTO `t_role_menu` VALUES (2, 29);
INSERT INTO `t_role_menu` VALUES (2, 30);
INSERT INTO `t_role_menu` VALUES (2, 31);
INSERT INTO `t_role_menu` VALUES (2, 32);
INSERT INTO `t_role_menu` VALUES (2, 33);
INSERT INTO `t_role_menu` VALUES (2, 34);
-- ----------------------------
-- Records of t_setting
-- ----------------------------
INSERT INTO `t_setting` VALUES (1, 'env.flink.home', NULL, 'Flink Home', 'Flink Home', 1);
INSERT INTO `t_setting` VALUES (2, 'maven.central.repository', NULL, 'Maven Central Repository', 'Maven 私服地址', 1);
INSERT INTO `t_setting` VALUES (3, 'streamx.console.webapp.address', NULL, 'StreamX Webapp address', 'StreamX Console Web 应用程序 HTTP 端口', 1);
INSERT INTO `t_setting` VALUES (4, 'streamx.console.workspace', '/streamx/workspace', 'StreamX Console Workspace', 'StreamX Console 的工作空间,用于存放项目源码,编译后的项目等', 1);
INSERT INTO `t_setting` VALUES (5, 'alert.email.host', NULL, 'Alert Email Smtp Host', '告警邮箱Smtp Host', 1);
INSERT INTO `t_setting` VALUES (6, 'alert.email.port', NULL, 'Alert Email Smtp Port', '告警邮箱的Smtp Port', 1);
INSERT INTO `t_setting` VALUES (7, 'alert.email.address', NULL, 'Alert  Email Sender', '用来发送告警邮箱的mail', 1);
INSERT INTO `t_setting` VALUES (8, 'alert.email.password', NULL, 'Alert Email Password', '发送告警的邮箱的密码', 1);
INSERT INTO `t_setting` VALUES (9, 'alert.email.ssl', 'false', 'Alert Email Is SSL', '发送告警的邮箱是否开启SSL', 2);
-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES (1, 'admin', '', 'ats6sdxdqf8vsqjtz0utj461wr', '829b009a6b9cc8ea486a4abbc38e56529f3c6f4c9c6fcd3604b41b1d6eca1a57', 1, 'benjobs@qq.com', '18500193260', '1', '2017-12-27 15:47:19', '2019-08-09 15:42:57', '2021-04-24 19:32:36', '0', 'author。', 'ubnKSIfAJTxIgXOKlciN.png', '1');

-- ----------------------------
-- Records of t_user_role
-- ----------------------------
INSERT INTO `t_user_role` VALUES (1, 1);

SET FOREIGN_KEY_CHECKS = 1;
