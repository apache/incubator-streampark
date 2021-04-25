SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Records of t_flink_app
-- ----------------------------
INSERT INTO `t_flink_app` VALUES (1381548268640436225, 2, 4, NULL, 'Flink Sql Test', NULL, NULL, NULL, NULL, '{\"jobmanager.memory.process.size\":\"1024mb\",\"taskmanager.memory.process.size\":\"1024mb\",\"parallelism.default\":1,\"taskmanager.numberOfTaskSlots\":1}', 1, NULL, 1, NULL, NULL, '2', NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, '2021-04-12 18:02:30', 0, NULL, NULL);

-- ----------------------------
-- Records of t_flink_effective
-- ----------------------------
INSERT INTO `t_flink_effective` VALUES (1381548275443597313, 1381548268640436225, 2, 1381548268724322305, NULL);

-- ----------------------------
-- Records of t_flink_project
-- ----------------------------
INSERT INTO `t_flink_project` VALUES (1, 'streamx-quickstart', 'https://gitee.com/benjobs/streamx-quickstart.git', 'main', NULL, NULL, NULL, 1, 1, '2021-04-08 05:01:02', '2021-04-12 17:59:36', 'streamx-quickstart', 1);

-- ----------------------------
-- Records of t_flink_sql
-- ----------------------------
INSERT INTO `t_flink_sql` VALUES (1381548268724322305, 1381548268640436225, 'eNqNVM9PGkEUvu9fMbeFhsUF+kuMBwQUEgEDa5uezMAusrow25lZLLeaGGNbrR5o05q20dRGDw320miBpv8Mu9BT/4XO7ig/atN0b+973/fezPvebDyfjClJoMTmFpPAIhpeMdAq8AmAfV6oq+BBLB9PxfIBD9SpVr0BliDVVhFu3EgUtQqs6whPopQAJZ1JFpRYZskX8Qt+8DCtpFhXsYRqNa1EEQ7ShqmJYBaI67C8DsUAkCTQ+/6j3zwDHgKG1HFVXcNERzVPaNV0N4QGE7tqLus/23Heff7V3R2mgdM8d3Y3gRwMhUCv86l3+bx/uMV5EydCpl7ild3BXF9torrHGReZGJkaprpGgkWEKKEYmkEmd1uLsyLVCJUqUEXIlO5Fp+XpcGAcun8TmvYgMTDehFCIqWVKVaTymWkQG7orQuUy0ejV9DovB18v7NMXgKPA7j5l0eC8Y++/FkTLVJmLoxrQNLWa6jYqI1yFdOTIGmET5rd2Xn1x9lpO+8A56trd/d5lG7jZoUbVMJuyREoVrQo9McUWq8KPAxKJRcBzoH/csluHnhoMTj86Hw4Gp1v2zlvBPyMI8fEtNetWfYXotfWrNVXp5HaZdTCXXkhnFR5a1+G/12xNLZYmtswF/r5kFjaGkmi1QR4b0akpA5WgUUGERiMR+e6Uaxkv59VhkomusGjwtsPLcPLg+Mw+2JtoxpalBqucjdkKcSI7obNz8QfXhIRsIKx63FA4EgmHw7fDXGCfb/ePNsfZG5i95WDZsEglWIVPJIw2CFd6/vzsvBm0Tu7Isuy8P2bvhXnba2873Wb/pO00v7EwxBKePelsIZlXAJtxbuSOUEguJuMK8yDBvFuZz+UzMcVHSQCIDfZJmYykqiCVisqy6Gcmum7Fc8tZxXfLD2IFVmiEJNIFJZ2NK9e/JI9g1YX5fC4z/GsJC/nc8hKYe/Q/DWeE30u2w2Q=', '{\"pom\":[{\"groupId\":\"mysql\",\"artifactId\":\"mysql-connector-java\",\"version\":\"5.1.48\",\"exclusions\":[]},{\"groupId\":\"org.apache.flink\",\"artifactId\":\"flink-sql-connector-kafka_2.12\",\"version\":\"1.12.0\",\"exclusions\":[]},{\"groupId\":\"org.apache.flink\",\"artifactId\":\"flink-connector-jdbc_2.11\",\"version\":\"1.12.0\",\"exclusions\":[]},{\"groupId\":\"org.apache.flink\",\"artifactId\":\"flink-json\",\"version\":\"1.12.0\",\"exclusions\":[]}]}', 1, 0, '2021-04-12 18:02:30');

-- ----------------------------
-- Records of t_flink_tutorial
-- ----------------------------
INSERT INTO `t_flink_tutorial` VALUES (1, 1, 'repl', '### Introduction\n\n[Apache Flink](https://flink.apache.org/) is a framework and distributed processing engine for stateful computations over unbounded and bounded data streams. This is Flink tutorial for running classical wordcount in both batch and streaming mode.\n\nThere\'re 3 things you need to do before using flink in StreamX Notebook.\n\n* Download [Flink 1.11](https://flink.apache.org/downloads.html) for scala 2.11 (Only scala-2.11 is supported, scala-2.12 is not supported yet in StreamX Notebook), unpack it and set `FLINK_HOME` in flink interpreter setting to this location.\n* Copy flink-python_2.11–1.11.1.jar from flink opt folder to flink lib folder (it is used by pyflink which is supported)\n* If you want to run yarn mode, you need to set `HADOOP_CONF_DIR` in flink interpreter setting. And make sure `hadoop` is in your `PATH`, because internally flink will call command `hadoop classpath` and put all the hadoop related jars in the classpath of flink interpreter process.\n\nThere\'re 6 sub interpreters in flink interpreter, each is used for different purpose. However they are in the the JVM and share the same ExecutionEnviroment/StremaExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment.\n\n* `flink`	- Creates ExecutionEnvironment/StreamExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment and provides a Scala environment\n* `pyflink`	- Provides a python environment\n* `ipyflink`	- Provides an ipython environment\n* `ssql`	 - Provides a stream sql environment\n* `bsql`	- Provides a batch sql environment\n', '2020-10-22');

-- ----------------------------
-- Records of t_menu
-- ----------------------------
INSERT INTO `t_menu` VALUES (1, 0, 'System', '/system', 'PageView', NULL, 'appstore', '0', '1', 1, '2017-12-27 16:39:07', '2021-02-18 10:45:18');
INSERT INTO `t_menu` VALUES (3, 1, 'User Management', '/system/user', 'system/user/User', 'user:view', 'user', '0', '1', 1, '2017-12-27 16:47:13', '2021-02-09 19:01:36');
INSERT INTO `t_menu` VALUES (4, 1, 'Role Management', '/system/role', 'system/role/Role', 'role:view', 'smile', '0', '1', 2, '2017-12-27 16:48:09', '2021-02-09 19:01:41');
INSERT INTO `t_menu` VALUES (5, 1, 'Router Management', '/system/menu', 'system/menu/Menu', 'menu:view', 'profile', '0', '1', 3, '2017-12-27 16:48:57', '2021-02-09 19:01:47');
INSERT INTO `t_menu` VALUES (11, 3, 'add', '', '', 'user:add', NULL, '1', '1', NULL, '2017-12-27 17:02:58', '2020-10-11 10:38:35');
INSERT INTO `t_menu` VALUES (12, 3, 'update', '', '', 'user:update', NULL, '1', '1', NULL, '2017-12-27 17:04:07', '2020-10-11 10:38:29');
INSERT INTO `t_menu` VALUES (13, 3, 'delete', '', '', 'user:delete', NULL, '1', '1', NULL, '2017-12-27 17:04:58', '2020-10-11 10:38:22');
INSERT INTO `t_menu` VALUES (14, 4, 'add', '', '', 'role:add', NULL, '1', '1', NULL, '2017-12-27 17:06:38', '2020-10-11 10:37:23');
INSERT INTO `t_menu` VALUES (15, 4, 'update', '', '', 'role:update', NULL, '1', '1', NULL, '2017-12-27 17:06:38', '2020-10-11 10:37:32');
INSERT INTO `t_menu` VALUES (16, 4, 'delete', '', '', 'role:delete', NULL, '1', '1', NULL, '2017-12-27 17:06:38', '2020-10-11 10:37:42');
INSERT INTO `t_menu` VALUES (17, 5, 'add', '', '', 'menu:add', NULL, '1', '1', NULL, '2017-12-27 17:08:02', '2020-10-11 10:40:33');
INSERT INTO `t_menu` VALUES (18, 5, 'update', '', '', 'menu:update', NULL, '1', '1', NULL, '2017-12-27 17:08:02', '2020-10-11 10:40:28');
INSERT INTO `t_menu` VALUES (19, 5, 'delete', '', '', 'menu:delete', NULL, '1', '1', NULL, '2017-12-27 17:08:02', '2020-10-11 10:40:22');
INSERT INTO `t_menu` VALUES (130, 3, 'export', NULL, NULL, 'user:export', NULL, '1', '1', NULL, '2019-01-23 06:35:16', '2020-10-11 10:38:15');
INSERT INTO `t_menu` VALUES (131, 4, 'export', NULL, NULL, 'role:export', NULL, '1', '1', NULL, '2019-01-23 06:35:36', '2020-10-11 10:37:51');
INSERT INTO `t_menu` VALUES (132, 5, 'export', NULL, NULL, 'menu:export', NULL, '1', '1', NULL, '2019-01-23 06:36:05', '2020-10-11 10:40:16');
INSERT INTO `t_menu` VALUES (135, 3, 'reset', NULL, NULL, 'user:reset', NULL, '1', '1', NULL, '2019-01-23 06:37:00', '2020-10-11 10:38:07');
INSERT INTO `t_menu` VALUES (183, 0, 'StreamX', '/flink', 'PageView', NULL, 'dashboard', '0', '1', 2, '2019-12-10 10:06:54', '2021-02-18 10:45:26');
INSERT INTO `t_menu` VALUES (184, 183, 'Project', '/flink/project', 'flink/project/View', 'project:view', 'github', '0', '1', 1, '2019-12-10 10:08:30', '2020-09-27 18:33:41');
INSERT INTO `t_menu` VALUES (185, 183, 'Application', '/flink/app', 'flink/app/View', 'app:view', 'hdd', '0', '1', 2, '2019-12-10 10:10:26', '2020-09-27 18:33:47');
INSERT INTO `t_menu` VALUES (188, 183, 'Add Application', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', '0', NULL, '2019-12-11 11:54:24', '2020-10-12 08:05:02');
INSERT INTO `t_menu` VALUES (190, 183, 'Add Project', '/flink/project/add', 'flink/project/Add', 'project:create', '', '0', '0', NULL, '2020-08-04 07:36:00', '2020-10-12 08:04:19');
INSERT INTO `t_menu` VALUES (191, 183, 'App Detail', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', '0', NULL, '2020-08-20 09:56:47', '2020-10-12 08:04:26');
INSERT INTO `t_menu` VALUES (192, 183, 'Notebook', '/flink/notebook/view', 'flink/notebook/Submit', 'notebook:submit', 'read', '0', '1', 3, '2020-09-07 17:10:57', '2020-11-09 15:19:57');
INSERT INTO `t_menu` VALUES (193, 183, 'Edit Flink App', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', '0', NULL, '2020-09-10 16:03:49', '2020-10-12 08:04:51');
INSERT INTO `t_menu` VALUES (194, 183, 'Edit Streamx App', '/flink/app/edit_streamx', 'flink/app/EditStreamX', 'app:update', '', '0', '0', NULL, '2020-09-22 21:11:51', '2020-10-12 08:04:43');
INSERT INTO `t_menu` VALUES (195, 184, 'build', NULL, NULL, 'project:build', NULL, '1', '1', NULL, '2020-09-30 14:18:38', '2020-10-11 10:38:48');
INSERT INTO `t_menu` VALUES (196, 184, 'delete', NULL, NULL, 'project:delete', NULL, '1', '1', NULL, '2020-09-30 14:21:10', '2020-10-11 10:38:57');
INSERT INTO `t_menu` VALUES (197, 185, 'mapping', NULL, NULL, 'app:mapping', NULL, '1', '1', NULL, '2020-09-30 14:33:32', '2020-10-11 10:39:05');
INSERT INTO `t_menu` VALUES (198, 185, 'deploy', NULL, NULL, 'app:deploy', NULL, '1', '1', NULL, '2020-09-30 14:33:49', '2020-10-11 10:39:14');
INSERT INTO `t_menu` VALUES (199, 185, 'start', NULL, NULL, 'app:start', NULL, '1', '1', NULL, '2020-09-30 14:34:05', '2020-10-11 10:39:22');
INSERT INTO `t_menu` VALUES (200, 185, 'clean', NULL, NULL, 'app:clean', NULL, '1', '1', NULL, '2020-09-30 14:34:25', '2020-10-11 10:39:57');
INSERT INTO `t_menu` VALUES (201, 185, 'cancel', NULL, NULL, 'app:cancel', NULL, '1', '1', NULL, '2020-09-30 14:34:40', '2020-11-16 10:32:21');
INSERT INTO `t_menu` VALUES (202, 185, 'savepoint delete', NULL, NULL, 'savepoint:delete', NULL, '1', '1', NULL, '2020-10-17 14:49:29', NULL);
INSERT INTO `t_menu` VALUES (203, 185, 'backup rollback', NULL, NULL, 'backup:rollback', NULL, '1', '1', NULL, '2020-10-17 14:50:05', '2021-02-20 15:55:56');
INSERT INTO `t_menu` VALUES (204, 185, 'backup delete', NULL, NULL, 'backup:delete', NULL, '1', '1', NULL, '2020-10-20 13:49:58', NULL);
INSERT INTO `t_menu` VALUES (205, 185, 'conf delete', NULL, NULL, 'conf:delete', NULL, '1', '1', NULL, '2020-10-20 14:06:39', '2020-10-20 14:15:21');
INSERT INTO `t_menu` VALUES (206, 185, 'flame Graph', NULL, NULL, 'app:flameGraph', NULL, '1', '1', NULL, '2020-12-23 21:59:00', '2020-12-23 22:00:10');
INSERT INTO `t_menu` VALUES (207, 183, 'Setting', '/flink/setting', 'flink/setting/View', 'setting:view', 'setting', '0', '1', 4, '2021-02-08 22:54:41', '2021-02-09 09:32:09');
INSERT INTO `t_menu` VALUES (208, 207, 'Setting Update', NULL, NULL, 'setting:update', NULL, '1', '1', NULL, '2021-02-08 23:17:29', NULL);
INSERT INTO `t_menu` VALUES (209, 1, 'User Profile', '/user/profile', 'system/user/Profile', '', '', '0', '0', NULL, '2021-04-11 23:59:10', '2021-04-12 00:05:30');

-- ----------------------------
-- Records of t_role
-- ----------------------------
INSERT INTO `t_role` VALUES (1, 'admin', 'admin', '2017-12-27 16:23:11', '2021-04-11 23:59:52', 'admin');
INSERT INTO `t_role` VALUES (142, 'developer', '普通开发者', '2021-04-11 19:02:56', '2021-04-12 00:00:14', NULL);

-- ----------------------------
-- Records of t_role_menu
-- ----------------------------
INSERT INTO `t_role_menu` VALUES (1, 1);
INSERT INTO `t_role_menu` VALUES (1, 3);
INSERT INTO `t_role_menu` VALUES (1, 11);
INSERT INTO `t_role_menu` VALUES (1, 12);
INSERT INTO `t_role_menu` VALUES (1, 17);
INSERT INTO `t_role_menu` VALUES (1, 18);
INSERT INTO `t_role_menu` VALUES (1, 19);
INSERT INTO `t_role_menu` VALUES (1, 132);
INSERT INTO `t_role_menu` VALUES (1, 135);
INSERT INTO `t_role_menu` VALUES (1, 5);
INSERT INTO `t_role_menu` VALUES (1, 4);
INSERT INTO `t_role_menu` VALUES (1, 14);
INSERT INTO `t_role_menu` VALUES (1, 15);
INSERT INTO `t_role_menu` VALUES (1, 16);
INSERT INTO `t_role_menu` VALUES (1, 131);
INSERT INTO `t_role_menu` VALUES (1, 13);
INSERT INTO `t_role_menu` VALUES (1, 130);
INSERT INTO `t_role_menu` VALUES (1, 183);
INSERT INTO `t_role_menu` VALUES (1, 184);
INSERT INTO `t_role_menu` VALUES (1, 191);
INSERT INTO `t_role_menu` VALUES (1, 192);
INSERT INTO `t_role_menu` VALUES (1, 193);
INSERT INTO `t_role_menu` VALUES (1, 194);
INSERT INTO `t_role_menu` VALUES (1, 195);
INSERT INTO `t_role_menu` VALUES (1, 196);
INSERT INTO `t_role_menu` VALUES (1, 197);
INSERT INTO `t_role_menu` VALUES (1, 200);
INSERT INTO `t_role_menu` VALUES (1, 201);
INSERT INTO `t_role_menu` VALUES (1, 185);
INSERT INTO `t_role_menu` VALUES (1, 198);
INSERT INTO `t_role_menu` VALUES (1, 199);
INSERT INTO `t_role_menu` VALUES (1, 188);
INSERT INTO `t_role_menu` VALUES (1, 190);
INSERT INTO `t_role_menu` VALUES (1, 202);
INSERT INTO `t_role_menu` VALUES (1, 203);
INSERT INTO `t_role_menu` VALUES (1, 204);
INSERT INTO `t_role_menu` VALUES (1, 205);
INSERT INTO `t_role_menu` VALUES (1, 206);
INSERT INTO `t_role_menu` VALUES (1, 207);
INSERT INTO `t_role_menu` VALUES (1, 208);
INSERT INTO `t_role_menu` VALUES (1, 209);
INSERT INTO `t_role_menu` VALUES (142, 197);
INSERT INTO `t_role_menu` VALUES (142, 198);
INSERT INTO `t_role_menu` VALUES (142, 199);
INSERT INTO `t_role_menu` VALUES (142, 200);
INSERT INTO `t_role_menu` VALUES (142, 185);
INSERT INTO `t_role_menu` VALUES (142, 201);
INSERT INTO `t_role_menu` VALUES (142, 202);
INSERT INTO `t_role_menu` VALUES (142, 203);
INSERT INTO `t_role_menu` VALUES (142, 205);
INSERT INTO `t_role_menu` VALUES (142, 206);
INSERT INTO `t_role_menu` VALUES (142, 204);
INSERT INTO `t_role_menu` VALUES (142, 188);
INSERT INTO `t_role_menu` VALUES (142, 191);
INSERT INTO `t_role_menu` VALUES (142, 192);
INSERT INTO `t_role_menu` VALUES (142, 193);
INSERT INTO `t_role_menu` VALUES (142, 194);
INSERT INTO `t_role_menu` VALUES (142, 183);
INSERT INTO `t_role_menu` VALUES (142, 209);

-- ----------------------------
-- Records of t_setting
-- ----------------------------
INSERT INTO `t_setting` VALUES ('maven.central.repository', '', 'Maven Central Repository', 'Maven 私服地址');
INSERT INTO `t_setting` VALUES ('streamx.console.webapp.address', 'http://test-hadoop-2:10001', 'StreamX Webapp address', 'StreamX Console Web 应用程序 HTTP 端口');
INSERT INTO `t_setting` VALUES ('streamx.console.workspace', '/streamx/workspace', 'StreamX Console Workspace', 'StreamX Console 的工作空间,用于存放项目源码,编译后的项目等');

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES (1, 'admin', '', 'ats6sdxdqf8vsqjtz0utj461wr', '829b009a6b9cc8ea486a4abbc38e56529f3c6f4c9c6fcd3604b41b1d6eca1a57', 1, 'benjobs@qq.com', '18500193260', '1', '2017-12-27 15:47:19', '2019-08-09 15:42:57', '2021-04-12 18:06:31', '0', 'author。', 'ubnKSIfAJTxIgXOKlciN.png', '1');

-- ----------------------------
-- Records of t_user_role
-- ----------------------------
INSERT INTO `t_user_role` VALUES (1, 1);

SET FOREIGN_KEY_CHECKS = 1;
