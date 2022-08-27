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
-- table structure for t_access_token
-- ----------------------------
drop table if exists "public"."t_access_token";
create table "public"."t_access_token" (
  "id" int4 not null,
  "user_id" int8,
  "token" varchar(1024) collate "pg_catalog"."default",
  "expire_time" timestamp(6),
  "description" varchar(512) collate "pg_catalog"."default",
  "status" int2,
  "create_time" timestamp(6),
  "modify_time" timestamp(6)
)
;
alter table "public"."t_access_token" owner to "postgres";
comment on column "public"."t_access_token"."id" is 'key';
comment on column "public"."t_access_token"."token" is 'token';
comment on column "public"."t_access_token"."expire_time" is '过期时间';
comment on column "public"."t_access_token"."description" is '使用场景描述';
comment on column "public"."t_access_token"."status" is '1:enable,0:disable';
comment on column "public"."t_access_token"."create_time" is 'create time';
comment on column "public"."t_access_token"."modify_time" is 'modify time';

-- ----------------------------
-- records of t_access_token
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_alert_config
-- ----------------------------
drop table if exists "public"."t_alert_config";
create table "public"."t_alert_config" (
  "id" int8 not null,
  "user_id" int8,
  "alert_name" varchar(128) collate "pg_catalog"."default",
  "alert_type" int4,
  "email_params" varchar(255) collate "pg_catalog"."default",
  "sms_params" text collate "pg_catalog"."default",
  "ding_talk_params" text collate "pg_catalog"."default",
  "we_com_params" varchar(255) collate "pg_catalog"."default",
  "http_callback_params" text collate "pg_catalog"."default",
  "lark_params" text collate "pg_catalog"."default",
  "create_time" timestamp(6) not null,
  "modify_time" timestamp(6) not null
)
;
alter table "public"."t_alert_config" owner to "postgres";
comment on column "public"."t_alert_config"."alert_name" is '报警组名称';
comment on column "public"."t_alert_config"."alert_type" is '报警类型';
comment on column "public"."t_alert_config"."email_params" is '邮件报警配置信息';
comment on column "public"."t_alert_config"."sms_params" is '短信报警配置信息';
comment on column "public"."t_alert_config"."ding_talk_params" is '钉钉报警配置信息';
comment on column "public"."t_alert_config"."we_com_params" is '企微报警配置信息';
comment on column "public"."t_alert_config"."http_callback_params" is '报警http回调配置信息';
comment on column "public"."t_alert_config"."lark_params" is '飞书报警配置信息';
comment on column "public"."t_alert_config"."create_time" is '创建时间';
comment on column "public"."t_alert_config"."modify_time" is '修改时间';

-- ----------------------------
-- records of t_alert_config
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_app_backup
-- ----------------------------
drop table if exists "public"."t_app_backup";
create table "public"."t_app_backup" (
  "id" int8 not null,
  "app_id" int8,
  "sql_id" int8,
  "config_id" int8,
  "version" int4,
  "path" varchar(255) collate "pg_catalog"."default",
  "description" varchar(255) collate "pg_catalog"."default",
  "create_time" timestamp(6)
)
;
alter table "public"."t_app_backup" owner to "postgres";

-- ----------------------------
-- records of t_app_backup
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_app_build_pipe
-- ----------------------------
drop table if exists "public"."t_app_build_pipe";
create table "public"."t_app_build_pipe" (
  "app_id" int8 not null,
  "pipe_type" int2,
  "pipe_status" int2,
  "cur_step" int2,
  "total_step" int2,
  "steps_status" text collate "pg_catalog"."default",
  "steps_status_ts" text collate "pg_catalog"."default",
  "error" text collate "pg_catalog"."default",
  "build_result" text collate "pg_catalog"."default",
  "update_time" timestamp(6)
)
;
alter table "public"."t_app_build_pipe" owner to "postgres";

-- ----------------------------
-- records of t_app_build_pipe
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_flame_graph
-- ----------------------------
drop table if exists "public"."t_flame_graph";
create table "public"."t_flame_graph" (
  "id" int8 not null,
  "app_id" int8,
  "profiler" varchar(255) collate "pg_catalog"."default",
  "timeline" timestamp(6),
  "content" text collate "pg_catalog"."default"
)
;
alter table "public"."t_flame_graph" owner to "postgres";

-- ----------------------------
-- records of t_flame_graph
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_flink_app
-- ----------------------------
drop table if exists "public"."t_flink_app";
create table "public"."t_flink_app" (
  "id" int8 not null,
  "job_type" int2,
  "execution_mode" int2,
  "resource_from" int2,
  "project_id" bigint,
  "job_name" varchar(255) collate "pg_catalog"."default",
  "module" varchar(255) collate "pg_catalog"."default",
  "jar" varchar(255) collate "pg_catalog"."default",
  "jar_check_sum" int8,
  "main_class" varchar(255) collate "pg_catalog"."default",
  "args" text collate "pg_catalog"."default",
  "options" text collate "pg_catalog"."default",
  "hot_params" text collate "pg_catalog"."default",
  "user_id" int8,
  "app_id" varchar(255) collate "pg_catalog"."default",
  "app_type" int2,
  "duration" int8,
  "job_id" varchar(64) collate "pg_catalog"."default",
  "version_id" int8,
  "cluster_id" varchar(255) collate "pg_catalog"."default",
  "k8s_namespace" varchar(255) collate "pg_catalog"."default",
  "flink_image" varchar(255) collate "pg_catalog"."default",
  "state" varchar(50) collate "pg_catalog"."default",
  "restart_size" int4,
  "restart_count" int4,
  "cp_threshold" int4,
  "cp_max_failure_interval" int4,
  "cp_failure_rate_interval" int4,
  "cp_failure_action" int2,
  "dynamic_options" text collate "pg_catalog"."default",
  "description" varchar(255) collate "pg_catalog"."default",
  "resolve_order" int2,
  "k8s_rest_exposed_type" int2,
  "flame_graph" int2,
  "jm_memory" int4,
  "tm_memory" int4,
  "total_task" int4,
  "total_tm" int4,
  "total_slot" int4,
  "available_slot" int4,
  "option_state" int2,
  "tracking" int2,
  "create_time" timestamp(6),
  "modify_time" timestamp(6) not null,
  "option_time" timestamp(6),
  "launch" int2,
  "build" int2,
  "start_time" timestamp(6),
  "end_time" timestamp(6),
  "alert_id" int8,
  "k8s_pod_template" text collate "pg_catalog"."default",
  "k8s_jm_pod_template" text collate "pg_catalog"."default",
  "k8s_tm_pod_template" text collate "pg_catalog"."default",
  "k8s_hadoop_integration" int2,
  "flink_cluster_id" int8,
  "ingress_template" text collate "pg_catalog"."default",
  "default_mode_ingress" text collate "pg_catalog"."default",
  "team_id" int8 not null
)
;
alter table "public"."t_flink_app" owner to "postgres";
comment on column "public"."t_flink_app"."team_id" is '任务所属组';

-- ----------------------------
-- records of t_flink_app
-- ----------------------------
begin;
insert into "public"."t_flink_app" values (100000, 2, 4, null, null, 'flink sql demo', null, null, null, null, null, null, null, 100000, null, 1, null, null, null, null, null, null, '0', 0, null, null, null, null, null, null, 'flink sql demo', 0, null, 0, null, null, null, null, null, null, 0, 0, '2022-08-25 15:16:48', '2022-08-25 15:16:48', null, 1, 1, null, null, null, null, null, null, 0, null, null, null, 1);
commit;

-- ----------------------------
-- table structure for t_flink_cluster
-- ----------------------------
drop table if exists "public"."t_flink_cluster";
create table "public"."t_flink_cluster" (
  "id" int8 not null,
  "address" varchar(255) collate "pg_catalog"."default",
  "cluster_id" varchar(255) collate "pg_catalog"."default",
  "cluster_name" varchar(255) collate "pg_catalog"."default" not null,
  "options" text collate "pg_catalog"."default",
  "yarn_queue" varchar(100) collate "pg_catalog"."default",
  "execution_mode" int2 not null,
  "version_id" int8 not null,
  "k8s_namespace" varchar(255) collate "pg_catalog"."default",
  "service_account" varchar(50) collate "pg_catalog"."default",
  "description" varchar(255) collate "pg_catalog"."default",
  "user_id" int8,
  "flink_image" varchar(255) collate "pg_catalog"."default",
  "dynamic_options" text collate "pg_catalog"."default",
  "k8s_rest_exposed_type" int2,
  "k8s_hadoop_integration" int2,
  "flame_graph" int2,
  "k8s_conf" varchar(255) collate "pg_catalog"."default",
  "resolve_order" int4,
  "exception" text collate "pg_catalog"."default",
  "cluster_state" int2,
  "create_time" timestamp(6)
)
;
alter table "public"."t_flink_cluster" owner to "postgres";
comment on column "public"."t_flink_cluster"."address" is 'jobmanager的url地址';
comment on column "public"."t_flink_cluster"."cluster_id" is 'session模式的clusterid(yarn-session:application-id,k8s-session:cluster-id)';
comment on column "public"."t_flink_cluster"."cluster_name" is '集群名称';
comment on column "public"."t_flink_cluster"."options" is '参数集合json形式';
comment on column "public"."t_flink_cluster"."yarn_queue" is '任务所在yarn队列';
comment on column "public"."t_flink_cluster"."execution_mode" is 'session类型(1:remote,3:yarn-session,5:kubernetes-session)';
comment on column "public"."t_flink_cluster"."version_id" is 'flink对应id';
comment on column "public"."t_flink_cluster"."k8s_namespace" is 'k8s namespace';
comment on column "public"."t_flink_cluster"."service_account" is 'k8s service account';
comment on column "public"."t_flink_cluster"."flink_image" is 'flink使用镜像';
comment on column "public"."t_flink_cluster"."dynamic_options" is '动态参数';
comment on column "public"."t_flink_cluster"."k8s_rest_exposed_type" is 'k8s 暴露类型(0:loadbalancer,1:clusterip,2:nodeport)';
comment on column "public"."t_flink_cluster"."flame_graph" is '是否开启火焰图，默认不开启';
comment on column "public"."t_flink_cluster"."k8s_conf" is 'k8s配置文件所在路径';
comment on column "public"."t_flink_cluster"."exception" is '异常信息';
comment on column "public"."t_flink_cluster"."cluster_state" is '集群状态(0:创建未启动,1:已启动,2:停止)';

-- ----------------------------
-- records of t_flink_cluster
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_flink_config
-- ----------------------------
drop table if exists "public"."t_flink_config";
create table "public"."t_flink_config" (
  "id" int8 not null,
  "app_id" int8 not null,
  "format" int2 not null,
  "version" int4 not null,
  "latest" int2 not null,
  "content" text collate "pg_catalog"."default" not null,
  "create_time" timestamp(6)
)
;
alter table "public"."t_flink_config" owner to "postgres";

-- ----------------------------
-- records of t_flink_config
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_flink_effective
-- ----------------------------
drop table if exists "public"."t_flink_effective";
create table "public"."t_flink_effective" (
  "id" int8 not null,
  "app_id" int8 not null,
  "target_type" int2 not null,
  "target_id" int8 not null,
  "create_time" timestamp(6)
)
;
alter table "public"."t_flink_effective" owner to "postgres";
comment on column "public"."t_flink_effective"."target_type" is '1) config 2) flink sql';
comment on column "public"."t_flink_effective"."target_id" is 'configid or sqlid';

-- ----------------------------
-- records of t_flink_effective
-- ----------------------------
begin;
insert into "public"."t_flink_effective" values (100000, 100000, 2, 100000, '2022-08-25 15:16:48');
commit;

-- ----------------------------
-- table structure for t_flink_env
-- ----------------------------
drop table if exists "public"."t_flink_env";
create table "public"."t_flink_env" (
  "id" int8 not null,
  "flink_name" varchar(128) collate "pg_catalog"."default" not null,
  "flink_home" varchar(255) collate "pg_catalog"."default" not null,
  "version" varchar(50) collate "pg_catalog"."default" not null,
  "scala_version" varchar(50) collate "pg_catalog"."default" not null,
  "flink_conf" text collate "pg_catalog"."default" not null,
  "is_default" int2 not null,
  "description" varchar(255) collate "pg_catalog"."default",
  "create_time" timestamp(6) not null
)
;
alter table "public"."t_flink_env" owner to "postgres";
comment on column "public"."t_flink_env"."id" is 'id';
comment on column "public"."t_flink_env"."flink_name" is 'flink实例名称';
comment on column "public"."t_flink_env"."flink_home" is 'flink home路径';
comment on column "public"."t_flink_env"."version" is 'flink对应的版本号';
comment on column "public"."t_flink_env"."scala_version" is 'flink对应的scala版本号';
comment on column "public"."t_flink_env"."flink_conf" is 'flink-conf配置内容';
comment on column "public"."t_flink_env"."is_default" is '是否为默认版本';
comment on column "public"."t_flink_env"."description" is '描述信息';
comment on column "public"."t_flink_env"."create_time" is '创建时间';

-- ----------------------------
-- records of t_flink_env
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_flink_log
-- ----------------------------
drop table if exists "public"."t_flink_log";
create table "public"."t_flink_log" (
  "id" int8 not null,
  "app_id" int8,
  "yarn_app_id" varchar(50) collate "pg_catalog"."default",
  "success" int2,
  "exception" text collate "pg_catalog"."default",
  "option_time" timestamp(6)
)
;
alter table "public"."t_flink_log" owner to "postgres";

-- ----------------------------
-- records of t_flink_log
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_flink_project
-- ----------------------------
drop table if exists "public"."t_flink_project";
create table "public"."t_flink_project" (
  "id" int8 not null,
  "name" varchar(255) collate "pg_catalog"."default",
  "url" varchar(1000) collate "pg_catalog"."default",
  "branches" varchar(1000) collate "pg_catalog"."default",
  "user_name" varchar(255) collate "pg_catalog"."default",
  "password" varchar(255) collate "pg_catalog"."default",
  "pom" varchar(255) collate "pg_catalog"."default",
  "build_args" varchar(255) collate "pg_catalog"."default",
  "type" int2,
  "repository" int2,
  "date" timestamp(6),
  "last_build" timestamp(6),
  "description" varchar(255) collate "pg_catalog"."default",
  "build_state" int2,
  "team_id" int8 not null
)
;
alter table "public"."t_flink_project" owner to "postgres";
comment on column "public"."t_flink_project"."team_id" is '项目所属组';

-- ----------------------------
-- records of t_flink_project
-- ----------------------------
begin;
insert into "public"."t_flink_project" values (100000, 'streamx-quickstart', 'https://github.com/streamxhub/streamx-quickstart.git', 'main', null, null, null, null, 1, 1, '2022-08-25 15:16:48', null, 'streamx-quickstart', 1, 1);
commit;

-- ----------------------------
-- table structure for t_flink_savepoint
-- ----------------------------
drop table if exists "public"."t_flink_savepoint";
create table "public"."t_flink_savepoint" (
  "id" int8 not null,
  "app_id" int8 not null,
  "type" int2,
  "path" varchar(255) collate "pg_catalog"."default",
  "latest" int2 not null,
  "trigger_time" timestamp(6),
  "create_time" timestamp(6)
)
;
alter table "public"."t_flink_savepoint" owner to "postgres";

-- ----------------------------
-- records of t_flink_savepoint
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_flink_sql
-- ----------------------------
drop table if exists "public"."t_flink_sql";
create table "public"."t_flink_sql" (
  "id" int8 not null,
  "app_id" int8,
  "sql" text collate "pg_catalog"."default",
  "dependency" text collate "pg_catalog"."default",
  "version" int4,
  "candidate" int2 not null,
  "create_time" timestamp(6)
)
;
alter table "public"."t_flink_sql" owner to "postgres";

-- ----------------------------
-- records of t_flink_sql
-- ----------------------------
begin;
insert into "public"."t_flink_sql" values (100000, 100000, 'eNqlUUtPhDAQvu+vmFs1AYIHT5s94AaVqGxSSPZIKgxrY2mxrdGfb4GS3c0+LnJo6Mz36syapkmZQpk8vKbQMMt2KOFmAe5rK4Nf3yhrhCwvA1/TTDaqO61UxmooSprlT1PDGkgKEKpmwvIOjWVdP3W2zpG+JfQFHjfU46xxrVvYZuWztye1khJrqzSBFRCfjUwSYQiqt1xJJvyPcbWJp9WPCXvUoUEn0ZAVufcs0nIUjYn2L4s++YiY75eBLr+2Dnl3GYKTWRyfQKYRRR2XZxXmNvu9yh9GHAmUO/sxyMRkGNly4c714RZ7zaWtLHsX+N9NjvVrWxm99jmyvEhpOUhujmIYFI5zkCOYzYIj11a7QH7Tyz+nE8bw', null, 1, 1, '2022-08-25 15:16:48');
commit;

-- ----------------------------
-- table structure for t_flink_tutorial
-- ----------------------------
drop table if exists "public"."t_flink_tutorial";
create table "public"."t_flink_tutorial" (
  "id" int4 not null,
  "type" int2,
  "name" varchar(255) collate "pg_catalog"."default",
  "content" text collate "pg_catalog"."default",
  "create_time" timestamp(6)
)
;
alter table "public"."t_flink_tutorial" owner to "postgres";

-- ----------------------------
-- records of t_flink_tutorial
-- ----------------------------
begin;
insert into "public"."t_flink_tutorial" values (100000, 1, 'repl', '### introduction

[apache flink](https://flink.apache.org/) is a framework and distributed processing engine for stateful computations over unbounded and bounded data streams. this is flink tutorial for running classical wordcount in both batch and streaming mode.

there''re 3 things you need to do before using flink in streamx notebook.

* download [flink 1.11](https://flink.apache.org/downloads.html) for scala 2.11 (only scala-2.11 is supported, scala-2.12 is not supported yet in streamx notebook), unpack it and set `flink_home` in flink interpreter setting to this location.
* copy flink-python_2.11–1.11.1.jar from flink opt folder to flink lib folder (it is used by pyflink which is supported)
* if you want to run yarn mode, you need to set `hadoop_conf_dir` in flink interpreter setting. and make sure `hadoop` is in your `path`, because internally flink will call command `hadoop classpath` and put all the hadoop related jars in the classpath of flink interpreter process.

there''re 6 sub interpreters in flink interpreter, each is used for different purpose. however they are in the the jvm and share the same executionenviroment/stremaexecutionenvironment/batchtableenvironment/streamtableenvironment.

* `flink`	- creates executionenvironment/streamexecutionenvironment/batchtableenvironment/streamtableenvironment and provides a scala environment
* `pyflink`	- provides a python environment
* `ipyflink`	- provides an ipython environment
* `ssql`	 - provides a stream sql environment
* `bsql`	- provides a batch sql environment
', '2022-08-25 15:16:48');
commit;

-- ----------------------------
-- table structure for t_menu
-- ----------------------------
drop table if exists "public"."t_menu";
create table "public"."t_menu" (
  "menu_id" int8 not null,
  "parent_id" int8 not null,
  "menu_name" varchar(50) collate "pg_catalog"."default" not null,
  "path" varchar(255) collate "pg_catalog"."default",
  "component" varchar(255) collate "pg_catalog"."default",
  "perms" varchar(50) collate "pg_catalog"."default",
  "icon" varchar(50) collate "pg_catalog"."default",
  "type" int2,
  "display" char(2) collate "pg_catalog"."default" not null,
  "order_num" float8,
  "create_time" timestamp(6) not null,
  "modify_time" timestamp(6)
)
;
alter table "public"."t_menu" owner to "postgres";
comment on column "public"."t_menu"."menu_id" is '菜单/按钮id';
comment on column "public"."t_menu"."parent_id" is '上级菜单id';
comment on column "public"."t_menu"."menu_name" is '菜单/按钮名称';
comment on column "public"."t_menu"."path" is '对应路由path';
comment on column "public"."t_menu"."component" is '对应路由组件component';
comment on column "public"."t_menu"."perms" is '权限标识';
comment on column "public"."t_menu"."icon" is '图标';
comment on column "public"."t_menu"."type" is '类型 0菜单 1按钮';
comment on column "public"."t_menu"."display" is '菜单是否显示';
comment on column "public"."t_menu"."order_num" is '排序';
comment on column "public"."t_menu"."create_time" is '创建时间';
comment on column "public"."t_menu"."modify_time" is '修改时间';

-- ----------------------------
-- records of t_menu
-- ----------------------------
begin;
INSERT INTO "public"."t_menu" VALUES (100000, 0, 'System', '/system', 'PageView', null, 'desktop', '0', '1', 1, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100001, 100000, 'User Management', '/system/user', 'system/user/User', 'user:view', 'user', '0', '1', 1, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100002, 100000, 'Role Management', '/system/role', 'system/role/Role', 'role:view', 'smile', '0', '1', 2, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100003, 100000, 'Router Management', '/system/menu', 'system/menu/Menu', 'menu:view', 'bars', '0', '1', 3, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100004, 100001, 'add', null, null, 'user:add', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100005, 100001, 'update', null, null, 'user:update', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100006, 100001, 'delete', null, null, 'user:delete', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100007, 100002, 'add', null, null, 'role:add', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100008, 100002, 'update', null, null, 'role:update', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100009, 100002, 'delete', null, null, 'role:delete', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100010, 100003, 'add', null, null, 'menu:add', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100011, 100003, 'update', null, null, 'menu:update', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100012, 100001, 'reset', null, null, 'user:reset', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100013, 0, 'StreamX', '/flink', 'PageView', null, 'build', '0', '1', 2, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100014, 100013, 'Project', '/flink/project', 'flink/project/View', 'project:view', 'github', '0', '1', 1, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100015, 100013, 'Application', '/flink/app', 'flink/app/View', 'app:view', 'mobile', '0', '1', 2, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100016, 100013, 'Add Application', '/flink/app/add', 'flink/app/Add', 'app:create', '', '0', '0', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100017, 100013, 'Add Project', '/flink/project/add', 'flink/project/Add', 'project:create', '', '0', '0', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100018, 100013, 'App Detail', '/flink/app/detail', 'flink/app/Detail', 'app:detail', '', '0', '0', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100019, 100013, 'Notebook', '/flink/notebook/view', 'flink/notebook/Submit', 'notebook:submit', 'read', '0', '1', 3, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100020, 100013, 'Edit Flink App', '/flink/app/edit_flink', 'flink/app/EditFlink', 'app:update', '', '0', '0', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100021, 100013, 'Edit StreamX App', '/flink/app/edit_streamx', 'flink/app/EditStreamX', 'app:update', '', '0', '0', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100022, 100014, 'build', null, null, 'project:build', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100023, 100014, 'delete', null, null, 'project:delete', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100024, 100015, 'mapping', null, null, 'app:mapping', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100025, 100015, 'launch', null, null, 'app:launch', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100026, 100015, 'start', null, null, 'app:start', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100027, 100015, 'clean', null, null, 'app:clean', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100028, 100015, 'cancel', null, null, 'app:cancel', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100029, 100015, 'savepoint delete', null, null, 'savepoint:delete', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100030, 100015, 'backup rollback', null, null, 'backup:rollback', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100031, 100015, 'backup delete', null, null, 'backup:delete', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100032, 100015, 'conf delete', null, null, 'conf:delete', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100033, 100015, 'flame Graph', null, null, 'app:flameGraph', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100034, 100013, 'Setting', '/flink/setting', 'flink/setting/View', 'setting:view', 'setting', '0', '1', 4, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100035, 100034, 'Setting Update', null, null, 'setting:update', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100036, 100013, 'Edit Project', '/flink/project/edit', 'flink/project/Edit', 'project:update', null, '0', '0', null, '2022-08-25 15:16:48', '2022-08-25 15:16:48');
INSERT INTO "public"."t_menu" VALUES (100037, 100015, 'delete', null, null, 'app:delete', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100038, 100000, 'Token Management', '/system/token', 'system/token/Token', 'token:view', 'lock', '0', '1', 1.0, '2022-08-25 15:16:48', '2022-08-25 15:16:48');
INSERT INTO "public"."t_menu" VALUES (100039, 100038, 'add', null, null, 'token:add', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100040, 100038, 'delete', null, null, 'token:delete', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100041, 100013, 'Add Cluster', '/flink/setting/add_cluster', 'flink/setting/AddCluster', 'cluster:create', '', '0', '0', null, '2022-08-25 15:16:48', '2022-08-25 15:16:48');
INSERT INTO "public"."t_menu" VALUES (100042, 100013, 'Edit Cluster', '/flink/setting/edit_cluster', 'flink/setting/EditCluster', 'cluster:update', '', '0', '0', null, '2022-08-25 15:16:48', '2022-08-25 15:16:48');
INSERT INTO "public"."t_menu" VALUES (100043, 100000, 'Team Management', '/system/team', 'system/team/Team', 'team:view', 'team', '0', '1', 1, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100044, 100043, 'add', null, null, 'team:add', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100045, 100043, 'update', null, null, 'team:update', null, '1', '1', null, '2022-08-25 15:16:48', null);
INSERT INTO "public"."t_menu" VALUES (100046, 100043, 'delete', null, null, 'team:delete', null, '1', '1', null, '2022-08-25 15:16:48', null);
insert into "public"."t_menu" VALUES (100047, 100015, 'copy', null, null, 'app:copy', null, '1 ', '1 ', null, '2022-08-25 15:16:48', null);
commit;

-- ----------------------------
-- table structure for t_message
-- ----------------------------
drop table if exists "public"."t_message";
create table "public"."t_message" (
  "id" int8 not null,
  "app_id" int8,
  "user_id" int8,
  "type" int2,
  "title" varchar(255) collate "pg_catalog"."default",
  "context" text collate "pg_catalog"."default",
  "readed" int2,
  "create_time" timestamp(6)
)
;
alter table "public"."t_message" owner to "postgres";

-- ----------------------------
-- records of t_message
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_role
-- ----------------------------
drop table if exists "public"."t_role";
create table "public"."t_role" (
  "role_id" int8 not null,
  "role_name" varchar(50) collate "pg_catalog"."default" not null,
  "remark" varchar(100) collate "pg_catalog"."default",
  "create_time" timestamp(6) not null,
  "modify_time" timestamp(6),
  "role_code" varchar(255) collate "pg_catalog"."default"
)
;
alter table "public"."t_role" owner to "postgres";
comment on column "public"."t_role"."role_id" is '角色id';
comment on column "public"."t_role"."role_name" is '角色名称';
comment on column "public"."t_role"."remark" is '角色描述';
comment on column "public"."t_role"."create_time" is '创建时间';
comment on column "public"."t_role"."modify_time" is '修改时间';
comment on column "public"."t_role"."role_code" is '角色标识';

-- ----------------------------
-- records of t_role
-- ----------------------------
begin;
insert into "public"."t_role" values (100000, 'admin', 'admin', '2022-08-25 15:16:48', null, null);
insert into "public"."t_role" values (100001, 'developer', 'developer', '2022-08-25 15:16:48', null, null);
commit;

-- ----------------------------
-- table structure for t_role_menu
-- ----------------------------
drop table if exists "public"."t_role_menu";
create table "public"."t_role_menu" (
  "id" int8 not null,
  "role_id" int8 not null,
  "menu_id" int8 not null
)
;
alter table "public"."t_role_menu" owner to "postgres";

-- ----------------------------
-- records of t_role_menu
-- ----------------------------
begin;
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
commit;

-- ----------------------------
-- table structure for t_setting
-- ----------------------------
drop table if exists "public"."t_setting";
create table "public"."t_setting" (
  "order_num" int4,
  "setting_key" varchar(50) collate "pg_catalog"."default" not null,
  "setting_value" text collate "pg_catalog"."default",
  "setting_name" varchar(255) collate "pg_catalog"."default",
  "description" varchar(255) collate "pg_catalog"."default",
  "type" int2 not null
)
;
alter table "public"."t_setting" owner to "postgres";
comment on column "public"."t_setting"."type" is '1: input 2: boolean 3: number';

-- ----------------------------
-- records of t_setting
-- ----------------------------
begin;
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
commit;

-- ----------------------------
-- table structure for t_team
-- ----------------------------
drop table if exists "public"."t_team";
create table "public"."t_team" (
  "team_id" int8 not null,
  "team_code" varchar(255) collate "pg_catalog"."default" not null,
  "team_name" varchar(255) collate "pg_catalog"."default" not null,
  "create_time" timestamp(6) not null
)
;
alter table "public"."t_team" owner to "postgres";
comment on column "public"."t_team"."team_id" is 'id';
comment on column "public"."t_team"."team_code" is '团队标识 后续可以用于队列 资源隔离相关';
comment on column "public"."t_team"."team_name" is '团队名';
comment on column "public"."t_team"."create_time" is '创建时间';

-- ----------------------------
-- records of t_team
-- ----------------------------
begin;
insert into "public"."t_team" values (1, 'bigdata', 'bigdata', '2022-02-21 18:00:00');
commit;

-- ----------------------------
-- table structure for t_team_user
-- ----------------------------
drop table if exists "public"."t_team_user";
create table "public"."t_team_user" (
  "team_id" int8 not null,
  "user_id" int8 not null,
  "create_time" timestamp(6) not null
)
;
alter table "public"."t_team_user" owner to "postgres";
comment on column "public"."t_team_user"."team_id" is 'teamid';
comment on column "public"."t_team_user"."user_id" is 'userid';
comment on column "public"."t_team_user"."create_time" is '创建时间';

-- ----------------------------
-- records of t_team_user
-- ----------------------------
begin;
commit;

-- ----------------------------
-- table structure for t_user
-- ----------------------------
drop table if exists "public"."t_user";
create table "public"."t_user" (
  "user_id" int8 not null,
  "username" varchar(255) collate "pg_catalog"."default",
  "nick_name" varchar(50) collate "pg_catalog"."default" not null,
  "salt" varchar(255) collate "pg_catalog"."default",
  "password" varchar(128) collate "pg_catalog"."default" not null,
  "email" varchar(128) collate "pg_catalog"."default",
  "status" int2,
  "create_time" timestamp(6) not null,
  "modify_time" timestamp(6),
  "last_login_time" timestamp(6),
  "sex" char(1) collate "pg_catalog"."default",
  "avatar" varchar(100) collate "pg_catalog"."default",
  "description" varchar(100) collate "pg_catalog"."default"
)
;
alter table "public"."t_user" owner to "postgres";
comment on column "public"."t_user"."user_id" is '用户id';
comment on column "public"."t_user"."username" is '登录用户名';
comment on column "public"."t_user"."nick_name" is '昵称';
comment on column "public"."t_user"."salt" is '密码加盐';
comment on column "public"."t_user"."password" is '密码';
comment on column "public"."t_user"."email" is '邮箱';
comment on column "public"."t_user"."status" is '状态 0锁定 1有效';
comment on column "public"."t_user"."create_time" is '创建时间';
comment on column "public"."t_user"."modify_time" is '修改时间';
comment on column "public"."t_user"."last_login_time" is '最近访问时间';
comment on column "public"."t_user"."sex" is '性别 0男 1女 2保密';
comment on column "public"."t_user"."avatar" is '用户头像';
comment on column "public"."t_user"."description" is '描述';

-- ----------------------------
-- records of t_user
-- ----------------------------
begin;
insert into "public"."t_user" values (100000, 'admin', '', 'ats6sdxdqf8vsqjtz0utj461wr', '829b009a6b9cc8ea486a4abbc38e56529f3c6f4c9c6fcd3604b41b1d6eca1a57', null, '1', '2022-08-25 15:16:48', null, null, '0', null, null);
commit;

-- ----------------------------
-- table structure for t_user_role
-- ----------------------------
drop table if exists "public"."t_user_role";
create table "public"."t_user_role" (
  "id" int8 not null,
  "user_id" int8,
  "role_id" int8
)
;
alter table "public"."t_user_role" owner to "postgres";
comment on column "public"."t_user_role"."user_id" is '用户id';
comment on column "public"."t_user_role"."role_id" is '角色id';

-- ----------------------------
-- records of t_user_role
-- ----------------------------
begin;
insert into "public"."t_user_role" values (100000, 100000, 100000);
commit;

-- ----------------------------
-- primary key structure for table t_access_token
-- ----------------------------
alter table "public"."t_access_token" add constraint "t_access_token_pkey" primary key ("id");

-- ----------------------------
-- indexes structure for table t_alert_config
-- ----------------------------
create index "inx_alert_user" on "public"."t_alert_config" using btree (
  "user_id" "pg_catalog"."int8_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_alert_config
-- ----------------------------
alter table "public"."t_alert_config" add constraint "t_alert_config_pkey" primary key ("id");

-- ----------------------------
-- primary key structure for table t_app_backup
-- ----------------------------
alter table "public"."t_app_backup" add constraint "t_app_backup_pkey" primary key ("id");

-- ----------------------------
-- primary key structure for table t_app_build_pipe
-- ----------------------------
alter table "public"."t_app_build_pipe" add constraint "t_app_build_pipe_pkey" primary key ("app_id");

-- ----------------------------
-- indexes structure for table t_flame_graph
-- ----------------------------
create index "inx_appid" on "public"."t_flame_graph" using btree (
  "app_id" "pg_catalog"."int8_ops" asc nulls last
);
create index "inx_time" on "public"."t_flame_graph" using btree (
  "timeline" "pg_catalog"."timestamp_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_flame_graph
-- ----------------------------
alter table "public"."t_flame_graph" add constraint "t_flame_graph_pkey" primary key ("id");

-- ----------------------------
-- indexes structure for table t_flink_app
-- ----------------------------
create index "inx_job_type" on "public"."t_flink_app" using btree (
  "job_type" "pg_catalog"."int2_ops" asc nulls last
);
create index "inx_state" on "public"."t_flink_app" using btree (
  "state" collate "pg_catalog"."default" "pg_catalog"."text_ops" asc nulls last
);
create index "inx_track" on "public"."t_flink_app" using btree (
  "tracking" "pg_catalog"."int2_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_flink_app
-- ----------------------------
alter table "public"."t_flink_app" add constraint "t_flink_app_pkey" primary key ("id");

-- ----------------------------
-- indexes structure for table t_flink_cluster
-- ----------------------------
create index "id" on "public"."t_flink_cluster" using btree (
  "cluster_id" collate "pg_catalog"."default" "pg_catalog"."text_ops" asc nulls last,
  "address" collate "pg_catalog"."default" "pg_catalog"."text_ops" asc nulls last,
  "execution_mode" "pg_catalog"."int2_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_flink_cluster
-- ----------------------------
alter table "public"."t_flink_cluster" add constraint "t_flink_cluster_pkey" primary key ("id", "cluster_name");

-- ----------------------------
-- primary key structure for table t_flink_config
-- ----------------------------
alter table "public"."t_flink_config" add constraint "t_flink_config_pkey" primary key ("id");

-- ----------------------------
-- indexes structure for table t_flink_effective
-- ----------------------------
create index "un_effective_inx" on "public"."t_flink_effective" using btree (
  "app_id" "pg_catalog"."int8_ops" asc nulls last,
  "target_type" "pg_catalog"."int2_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_flink_effective
-- ----------------------------
alter table "public"."t_flink_effective" add constraint "t_flink_effective_pkey" primary key ("id");

-- ----------------------------
-- indexes structure for table t_flink_env
-- ----------------------------
create index "un_env_name" on "public"."t_flink_env" using btree (
  "flink_name" collate "pg_catalog"."default" "pg_catalog"."text_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_flink_env
-- ----------------------------
alter table "public"."t_flink_env" add constraint "t_flink_env_pkey" primary key ("id");

-- ----------------------------
-- primary key structure for table t_flink_log
-- ----------------------------
alter table "public"."t_flink_log" add constraint "t_flink_log_pkey" primary key ("id");

-- ----------------------------
-- primary key structure for table t_flink_project
-- ----------------------------
alter table "public"."t_flink_project" add constraint "t_flink_project_pkey" primary key ("id");

-- ----------------------------
-- primary key structure for table t_flink_savepoint
-- ----------------------------
alter table "public"."t_flink_savepoint" add constraint "t_flink_savepoint_pkey" primary key ("id");

-- ----------------------------
-- primary key structure for table t_flink_sql
-- ----------------------------
alter table "public"."t_flink_sql" add constraint "t_flink_sql_pkey" primary key ("id");

-- ----------------------------
-- primary key structure for table t_flink_tutorial
-- ----------------------------
alter table "public"."t_flink_tutorial" add constraint "t_flink_tutorial_pkey" primary key ("id");

-- ----------------------------
-- primary key structure for table t_menu
-- ----------------------------
alter table "public"."t_menu" add constraint "t_menu_pkey" primary key ("menu_id");

-- ----------------------------
-- indexes structure for table t_message
-- ----------------------------
create index "inx_mes_user" on "public"."t_message" using btree (
  "user_id" "pg_catalog"."int8_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_message
-- ----------------------------
alter table "public"."t_message" add constraint "t_message_pkey" primary key ("id");

-- ----------------------------
-- primary key structure for table t_role
-- ----------------------------
alter table "public"."t_role" add constraint "t_role_pkey" primary key ("role_id");

-- ----------------------------
-- indexes structure for table t_role_menu
-- ----------------------------
create index "un_role_menu_inx" on "public"."t_role_menu" using btree (
  "role_id" "pg_catalog"."int8_ops" asc nulls last,
  "menu_id" "pg_catalog"."int8_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_role_menu
-- ----------------------------
alter table "public"."t_role_menu" add constraint "t_role_menu_pkey" primary key ("id");

-- ----------------------------
-- primary key structure for table t_setting
-- ----------------------------
alter table "public"."t_setting" add constraint "t_setting_pkey" primary key ("setting_key");

-- ----------------------------
-- indexes structure for table t_team
-- ----------------------------
create index "team_code" on "public"."t_team" using btree (
  "team_code" collate "pg_catalog"."default" "pg_catalog"."text_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_team
-- ----------------------------
alter table "public"."t_team" add constraint "t_team_pkey" primary key ("team_id");

-- ----------------------------
-- indexes structure for table t_team_user
-- ----------------------------
create index "group_user" on "public"."t_team_user" using btree (
  "team_id" "pg_catalog"."int8_ops" asc nulls last,
  "user_id" "pg_catalog"."int8_ops" asc nulls last
);

-- ----------------------------
-- indexes structure for table t_user
-- ----------------------------
create index "un_username" on "public"."t_user" using btree (
  "nick_name" collate "pg_catalog"."default" "pg_catalog"."text_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_user
-- ----------------------------
alter table "public"."t_user" add constraint "t_user_pkey" primary key ("user_id");

-- ----------------------------
-- indexes structure for table t_user_role
-- ----------------------------
create index "un_user_role_inx" on "public"."t_user_role" using btree (
  "user_id" "pg_catalog"."int8_ops" asc nulls last,
  "role_id" "pg_catalog"."int8_ops" asc nulls last
);

-- ----------------------------
-- primary key structure for table t_user_role
-- ----------------------------
alter table "public"."t_user_role" add constraint "t_user_role_pkey" primary key ("id");
