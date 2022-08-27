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

create database if not exists streamx;
use streamx;

set names utf8mb4;
set foreign_key_checks = 0;

-- ----------------------------
-- table structure for t_app_backup
-- ----------------------------
drop table if exists `t_app_backup`;
create table `t_app_backup` (
`id` bigint not null auto_increment,
`app_id` bigint default null,
`sql_id` bigint default null,
`config_id` bigint default null,
`version` int default null,
`path` varchar(255) collate utf8mb4_general_ci default null,
`description` varchar(255) collate utf8mb4_general_ci default null,
`create_time` datetime default null,
primary key (`id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_flame_graph
-- ----------------------------
drop table if exists `t_flame_graph`;
create table `t_flame_graph` (
`id` bigint not null auto_increment,
`app_id` bigint default null,
`profiler` varchar(255) collate utf8mb4_general_ci default null,
`timeline` datetime default null,
`content` text collate utf8mb4_general_ci,
primary key (`id`) using btree,
key `inx_time` (`timeline`),
key `inx_appid` (`app_id`)
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_flink_app
-- ----------------------------
drop table if exists `t_flink_app`;
create table `t_flink_app` (
`id` bigint not null auto_increment,
`job_type` tinyint default null,
`execution_mode` tinyint default null,
`resource_from` tinyint default null,
`project_id` varchar(64) collate utf8mb4_general_ci default null,
`job_name` varchar(255) collate utf8mb4_general_ci default null,
`module` varchar(255) collate utf8mb4_general_ci default null,
`jar` varchar(255) collate utf8mb4_general_ci default null,
`jar_check_sum` bigint default null,
`main_class` varchar(255) collate utf8mb4_general_ci default null,
`args` text collate utf8mb4_general_ci,
`options` text collate utf8mb4_general_ci,
`hot_params` text collate utf8mb4_general_ci,
`user_id` bigint default null,
`app_id` varchar(255) collate utf8mb4_general_ci default null,
`app_type` tinyint default null,
`duration` bigint default null,
`job_id` varchar(64) collate utf8mb4_general_ci default null,
`version_id` bigint default null,
`cluster_id` varchar(255) collate utf8mb4_general_ci default null,
`k8s_namespace` varchar(255) collate utf8mb4_general_ci default null,
`flink_image` varchar(255) collate utf8mb4_general_ci default null,
`state` varchar(50) collate utf8mb4_general_ci default null,
`restart_size` int default null,
`restart_count` int default null,
`cp_threshold` int default null,
`cp_max_failure_interval` int default null,
`cp_failure_rate_interval` int default null,
`cp_failure_action` tinyint default null,
`dynamic_options` text collate utf8mb4_general_ci,
`description` varchar(255) collate utf8mb4_general_ci default null,
`resolve_order` tinyint default null,
`k8s_rest_exposed_type` tinyint default null,
`flame_graph` tinyint default '0',
`jm_memory` int default null,
`tm_memory` int default null,
`total_task` int default null,
`total_tm` int default null,
`total_slot` int default null,
`available_slot` int default null,
`option_state` tinyint default null,
`tracking` tinyint default null,
`create_time` datetime default null,
`modify_time` datetime not null default current_timestamp on update current_timestamp,
`option_time` datetime default null,
`launch` tinyint default '1',
`build` tinyint default '1',
`start_time` datetime default null,
`end_time` datetime default null,
`alert_id` bigint default null,
`k8s_pod_template` text collate utf8mb4_general_ci,
`k8s_jm_pod_template` text collate utf8mb4_general_ci,
`k8s_tm_pod_template` text collate utf8mb4_general_ci,
`k8s_hadoop_integration` tinyint default '0',
`flink_cluster_id` bigint default null,
`ingress_template` text collate utf8mb4_general_ci,
`default_mode_ingress` text collate utf8mb4_general_ci,
`team_id` bigint not null default 1 comment '任务所属组',
primary key (`id`) using btree,
key `inx_state` (`state`) using btree,
key `inx_job_type` (`job_type`) using btree,
key `inx_track` (`tracking`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_flink_config
-- ----------------------------
drop table if exists `t_flink_config`;
create table `t_flink_config` (
`id` bigint not null auto_increment,
`app_id` bigint not null,
`format` tinyint not null default '0',
`version` int not null,
`latest` tinyint not null default '0',
`content` text collate utf8mb4_general_ci not null,
`create_time` datetime default null,
primary key (`id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_flink_effective
-- ----------------------------
drop table if exists `t_flink_effective`;
create table `t_flink_effective` (
`id` bigint not null auto_increment,
`app_id` bigint not null,
`target_type` tinyint not null comment '1) config 2) flink sql',
`target_id` bigint not null comment 'configId or sqlId',
`create_time` datetime default null,
primary key (`id`) using btree,
unique key `un_effective_inx` (`app_id`,`target_type`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_flink_env
-- ----------------------------
drop table if exists `t_flink_env`;
create table `t_flink_env` (
`id` bigint not null auto_increment comment 'id',
`flink_name` varchar(128) collate utf8mb4_general_ci not null comment 'flink实例名称',
`flink_home` varchar(255) collate utf8mb4_general_ci not null comment 'flink home路径',
`version` varchar(50) collate utf8mb4_general_ci not null comment 'flink对应的版本号',
`scala_version` varchar(50) collate utf8mb4_general_ci not null comment 'flink对应的scala版本号',
`flink_conf` text collate utf8mb4_general_ci not null comment 'flink-conf配置内容',
`is_default` tinyint not null default '0' comment '是否为默认版本',
`description` varchar(255) collate utf8mb4_general_ci default null comment '描述信息',
`create_time` datetime not null comment '创建时间',
primary key (`id`) using btree,
unique key `un_env_name` (`flink_name`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_flink_log
-- ----------------------------
drop table if exists `t_flink_log`;
create table `t_flink_log` (
`id` bigint not null auto_increment,
`app_id` bigint default null,
`yarn_app_id` varchar(50) collate utf8mb4_general_ci default null,
`success` tinyint default null,
`exception` text collate utf8mb4_general_ci,
`option_time` datetime default null,
primary key (`id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_flink_project
-- ----------------------------
drop table if exists `t_flink_project`;
create table `t_flink_project` (
`id` bigint not null auto_increment,
`name` varchar(255) collate utf8mb4_general_ci default null,
`url` varchar(1000) collate utf8mb4_general_ci default null,
`branches` varchar(1000) collate utf8mb4_general_ci default null,
`user_name` varchar(255) collate utf8mb4_general_ci default null,
`password` varchar(255) collate utf8mb4_general_ci default null,
`pom` varchar(255) collate utf8mb4_general_ci default null,
`build_args` varchar(255) default null,
`type` tinyint default null,
`repository` tinyint default null,
`date` datetime default null,
`last_build` datetime default null,
`description` varchar(255) collate utf8mb4_general_ci default null,
`build_state` tinyint default '-1',
`team_id` bigint not null default 1,
primary key (`id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_flink_savepoint
-- ----------------------------
drop table if exists `t_flink_savepoint`;
create table `t_flink_savepoint` (
`id` bigint not null auto_increment,
`app_id` bigint not null,
`type` tinyint default null,
`path` varchar(255) collate utf8mb4_general_ci default null,
`latest` tinyint not null,
`trigger_time` datetime default null,
`create_time` datetime default null,
primary key (`id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_flink_sql
-- ----------------------------
drop table if exists `t_flink_sql`;
create table `t_flink_sql` (
`id` bigint not null auto_increment,
`app_id` bigint default null,
`sql` text collate utf8mb4_general_ci,
`dependency` text collate utf8mb4_general_ci,
`version` int default null,
`candidate` tinyint not null default '1',
`create_time` datetime default null,
primary key (`id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_flink_tutorial
-- ----------------------------
drop table if exists `t_flink_tutorial`;
create table `t_flink_tutorial` (
`id` int not null auto_increment,
`type` tinyint default null,
`name` varchar(255) collate utf8mb4_general_ci default null,
`content` text collate utf8mb4_general_ci,
`create_time` datetime default null,
primary key (`id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_menu
-- ----------------------------
drop table if exists `t_menu`;
create table `t_menu` (
`menu_id` bigint not null auto_increment comment '菜单/按钮id',
`parent_id` bigint not null comment '上级菜单id',
`menu_name` varchar(50) collate utf8mb4_general_ci not null comment '菜单/按钮名称',
`path` varchar(255) collate utf8mb4_general_ci default null comment '对应路由path',
`component` varchar(255) collate utf8mb4_general_ci default null comment '对应路由组件component',
`perms` varchar(50) collate utf8mb4_general_ci default null comment '权限标识',
`icon` varchar(50) collate utf8mb4_general_ci default null comment '图标',
`type` char(2) collate utf8mb4_general_ci default null comment '类型 0菜单 1按钮',
`display` char(2) collate utf8mb4_general_ci not null default '1' comment '菜单是否显示',
`order_num` double(20,0) default null comment '排序',
`create_time` datetime not null comment '创建时间',
`modify_time` datetime default null comment '修改时间',
primary key (`menu_id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_message
-- ----------------------------
drop table if exists `t_message`;
create table `t_message` (
`id` bigint not null auto_increment,
`app_id` bigint default null,
`user_id` bigint default null,
`type` tinyint default null,
`title` varchar(255) collate utf8mb4_general_ci default null,
`context` text collate utf8mb4_general_ci,
`readed` tinyint default '0',
`create_time` datetime default null,
primary key (`id`) using btree,
key `inx_mes_user` (`user_id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_role
-- ----------------------------
drop table if exists `t_role`;
create table `t_role` (
`role_id` bigint not null auto_increment comment '角色id',
`role_name` varchar(50) collate utf8mb4_general_ci not null comment '角色名称',
`remark` varchar(100) collate utf8mb4_general_ci default null comment '角色描述',
`create_time` datetime not null comment '创建时间',
`modify_time` datetime default null comment '修改时间',
`role_code` varchar(255) collate utf8mb4_general_ci default null comment '角色标识',
primary key (`role_id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_role_menu
-- ----------------------------
drop table if exists `t_role_menu`;
create table `t_role_menu` (
`id` bigint not null auto_increment,
`role_id` bigint not null,
`menu_id` bigint not null,
primary key (`id`) using btree,
unique key `un_role_menu_inx` (`role_id`,`menu_id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_setting
-- ----------------------------
drop table if exists `t_setting`;
create table `t_setting` (
`order_num` int default null,
`setting_key` varchar(50) collate utf8mb4_general_ci not null,
`setting_value` text collate utf8mb4_general_ci default null,
`setting_name` varchar(255) collate utf8mb4_general_ci default null,
`description` varchar(255) collate utf8mb4_general_ci default null,
`type` tinyint not null comment '1: input 2: boolean 3: number',
primary key (`setting_key`) using btree
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_user
-- ----------------------------
drop table if exists `t_user`;
create table `t_user` (
`user_id` bigint not null auto_increment comment '用户id',
`username` varchar(255) collate utf8mb4_general_ci default null comment '登录用户名',
`nick_name` varchar(50) collate utf8mb4_general_ci not null comment '昵称',
`salt` varchar(255) collate utf8mb4_general_ci default null comment '密码加盐',
`password` varchar(128) collate utf8mb4_general_ci not null comment '密码',
`email` varchar(128) collate utf8mb4_general_ci default null comment '邮箱',
`status` char(1) collate utf8mb4_general_ci not null comment '状态 0锁定 1有效',
`create_time` datetime not null comment '创建时间',
`modify_time` datetime default null comment '修改时间',
`last_login_time` datetime default null comment '最近访问时间',
`sex` char(1) collate utf8mb4_general_ci default null comment '性别 0男 1女 2保密',
`avatar` varchar(100) collate utf8mb4_general_ci default null comment '用户头像',
`description` varchar(100) collate utf8mb4_general_ci default null comment '描述',
primary key (`user_id`) using btree,
unique key `un_username` (`nick_name`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table structure for t_user_role
-- ----------------------------
drop table if exists `t_user_role`;
create table `t_user_role` (
`id` bigint not null auto_increment,
`user_id` bigint default null comment '用户id',
`role_id` bigint default null comment '角色id',
primary key (`id`) using btree,
unique key `un_user_role_inx` (`user_id`,`role_id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table of t_app_build_pipe
-- ----------------------------
drop table if exists `t_app_build_pipe`;
create table `t_app_build_pipe`(
`app_id`          bigint auto_increment,
`pipe_type`       tinyint,
`pipe_status`     tinyint,
`cur_step`        smallint,
`total_step`      smallint,
`steps_status`    text,
`steps_status_ts` text,
`error`           text,
`build_result`    text,
`update_time`     datetime,
primary key (`app_id`) using btree
) engine = innodb auto_increment=100000 default charset = utf8mb4 collate = utf8mb4_general_ci;


-- ----------------------------
-- table of t_flink_cluster
-- ----------------------------
drop table if exists `t_flink_cluster`;
create table `t_flink_cluster` (
   `id` bigint not null auto_increment,
   `address` varchar(255) default null comment 'jobManager的url地址',
   `cluster_id` varchar(255) default null comment 'session模式的clusterId(yarn-session:application-id,k8s-session:cluster-id)',
   `cluster_name` varchar(255) not null comment '集群名称',
   `options` text comment '参数集合json形式',
   `yarn_queue` varchar(100) default null comment '任务所在yarn队列',
   `execution_mode` tinyint not null default '1' comment 'session类型(1:remote,3:yarn-session,5:kubernetes-session)',
   `version_id` bigint not null comment 'flink对应id',
   `k8s_namespace` varchar(255) default 'default' comment 'k8s namespace',
   `service_account` varchar(50) default null comment 'k8s service account',
   `description` varchar(255) default null,
   `user_id` bigint default null,
   `flink_image` varchar(255) default null comment 'flink使用镜像',
   `dynamic_options` text comment '动态参数',
   `k8s_rest_exposed_type` tinyint default '2' comment 'k8s 暴露类型(0:loadbalancer,1:clusterIp,2:nodePort)',
   `k8s_hadoop_integration` tinyint default '0',
   `flame_graph` tinyint default '0' comment '是否开启火焰图，默认不开启',
   `k8s_conf` varchar(255) default null comment 'k8s配置文件所在路径',
   `resolve_order` int(11) default null,
   `exception` text comment '异常信息',
   `cluster_state` tinyint default '0' comment '集群状态(0:创建未启动,1:已启动,2:停止)',
   `create_time` datetime default null,
   primary key (`id`,`cluster_name`),
   unique key `id` (`cluster_id`,`address`,`execution_mode`)
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table of t_access_token definition
-- ----------------------------
drop table if exists `t_access_token`;
create table `t_access_token` (
`id`            int not null auto_increment comment 'key',
`user_id`       bigint,
`token`         varchar(1024) character set utf8mb4 collate utf8mb4_general_ci default null comment 'token',
`expire_time`   datetime default null comment '过期时间',
`description`   varchar(512) character set utf8mb4 collate utf8mb4_general_ci default null comment '使用场景描述',
`status`        tinyint default null comment '1:enable,0:disable',
`create_time`   datetime default null comment 'create time',
`modify_time`   datetime default null comment 'modify time',
primary key (`id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table of t_alert_config
-- ----------------------------
drop table if exists `t_alert_config`;
create table `t_alert_config` (
  `id`                   bigint not null auto_increment primary key,
  `user_id`              bigint default null,
  `alert_name`           varchar(128) collate utf8mb4_general_ci default null comment '报警组名称',
  `alert_type`           int default 0 comment '报警类型',
  `email_params`         varchar(255) collate utf8mb4_general_ci comment '邮件报警配置信息',
  `sms_params`           text collate utf8mb4_general_ci comment '短信报警配置信息',
  `ding_talk_params`     text collate utf8mb4_general_ci comment '钉钉报警配置信息',
  `we_com_params`        varchar(255) collate utf8mb4_general_ci comment '企微报警配置信息',
  `http_callback_params` text collate utf8mb4_general_ci comment '报警http回调配置信息',
  `lark_params`          text collate utf8mb4_general_ci comment '飞书报警配置信息',
  `create_time`          datetime not null default current_timestamp comment '创建时间',
  `modify_time`          datetime not null default current_timestamp on update current_timestamp comment '修改时间',
  index `inx_alert_user` (`user_id`) using btree
) engine = innodb
  default charset = utf8mb4
  collate = utf8mb4_general_ci;


-- ----------------------------
-- table of t_team
-- ----------------------------
drop table if exists `t_team`;
create table `t_team`
(
    `team_id`     bigint       not null auto_increment comment 'id',
    `team_code`   varchar(255) not null comment '团队标识 后续可以用于队列 资源隔离相关',
    `team_name`   varchar(255) not null comment '团队名',
    `create_time` datetime     not null comment '创建时间',
    primary key (`team_id`) using btree,
    unique key `inx_team_team_code` (`team_code`) using btree
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci;


-- ----------------------------
-- table of t_team_user
-- ----------------------------
drop table if exists `t_team_user`;
create table `t_team_user`
(
    `team_id`    bigint   not null comment 'teamId',
    `user_id`     bigint   not null comment 'userid',
    `create_time` datetime not null comment 'create time',
    unique key `inx_team_group_user` (`team_id`,`user_id`) using btree
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci;


