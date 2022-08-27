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

-- ------------------------------------- version: 1.2.1 START ---------------------------------------
set names utf8mb4;
set foreign_key_checks = 0;

begin;

update `t_flink_app` set `resource_from` = 1 where `job_type` = 1;

-- ----------------------------
-- Table of t_app_build_pipe
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
-- table structure for t_role_menu
-- ----------------------------
drop table if exists `t_role_menu`;
create table `t_role_menu` (
`id` bigint not null auto_increment,
`role_id` bigint not null,
`menu_id` bigint not null,
primary key (`id`) using btree,
unique key `un_inx` (`role_id`,`menu_id`) using btree
) engine=innodb auto_increment=1 default charset=utf8mb4 collate=utf8mb4_general_ci;

-- ----------------------------
-- Records of t_role_menu
-- ----------------------------
insert into `t_role_menu` values (1, 1, 1);
insert into `t_role_menu` values (2, 1, 2);
insert into `t_role_menu` values (3, 1, 3);
insert into `t_role_menu` values (4, 1, 4);
insert into `t_role_menu` values (5, 1, 5);
insert into `t_role_menu` values (6, 1, 6);
insert into `t_role_menu` values (7, 1, 7);
insert into `t_role_menu` values (8, 1, 8);
insert into `t_role_menu` values (9, 1, 9);
insert into `t_role_menu` values (10, 1, 10);
insert into `t_role_menu` values (11, 1, 11);
insert into `t_role_menu` values (12, 1, 12);
insert into `t_role_menu` values (13, 1, 13);
insert into `t_role_menu` values (14, 1, 14);
insert into `t_role_menu` values (15, 1, 15);
insert into `t_role_menu` values (16, 1, 16);
insert into `t_role_menu` values (17, 1, 17);
insert into `t_role_menu` values (18, 1, 18);
insert into `t_role_menu` values (19, 1, 19);
insert into `t_role_menu` values (20, 1, 20);
insert into `t_role_menu` values (21, 1, 21);
insert into `t_role_menu` values (22, 1, 22);
insert into `t_role_menu` values (23, 1, 23);
insert into `t_role_menu` values (24, 1, 24);
insert into `t_role_menu` values (25, 1, 25);
insert into `t_role_menu` values (26, 1, 26);
insert into `t_role_menu` values (27, 1, 27);
insert into `t_role_menu` values (28, 1, 28);
insert into `t_role_menu` values (29, 1, 29);
insert into `t_role_menu` values (30, 1, 30);
insert into `t_role_menu` values (31, 1, 31);
insert into `t_role_menu` values (32, 1, 32);
insert into `t_role_menu` values (33, 1, 33);
insert into `t_role_menu` values (34, 1, 34);
insert into `t_role_menu` values (35, 1, 35);
insert into `t_role_menu` values (36, 1, 36);
insert into `t_role_menu` values (37, 1, 37);
insert into `t_role_menu` values (38, 2, 14);
insert into `t_role_menu` values (39, 2, 16);
insert into `t_role_menu` values (40, 2, 17);
insert into `t_role_menu` values (41, 2, 18);
insert into `t_role_menu` values (42, 2, 19);
insert into `t_role_menu` values (43, 2, 20);
insert into `t_role_menu` values (44, 2, 21);
insert into `t_role_menu` values (45, 2, 22);
insert into `t_role_menu` values (46, 2, 25);
insert into `t_role_menu` values (47, 2, 26);
insert into `t_role_menu` values (48, 2, 27);
insert into `t_role_menu` values (49, 2, 28);
insert into `t_role_menu` values (50, 2, 29);
insert into `t_role_menu` values (51, 2, 30);
insert into `t_role_menu` values (52, 2, 31);
insert into `t_role_menu` values (53, 2, 32);
insert into `t_role_menu` values (54, 2, 33);
insert into `t_role_menu` values (55, 2, 34);

commit;

set foreign_key_checks = 1;
-- ------------------------------------- version: 1.2.1 end ---------------------------------------


-- ------------------------------------- version: 1.2.2 start ---------------------------------------
set names utf8mb4;
set foreign_key_checks = 0;

begin;
-- menu
update `t_menu` set menu_name='launch',perms='app:launch' where menu_name='deploy';
-- change default value
update `t_setting` set `key`='streamx.maven.central.repository' where `key` = 'maven.central.repository';
commit;

-- rename column
alter table `t_flink_project`
    change column `username` `user_name` varchar(255) character set utf8mb4 default null after `branches`,
    change column `lastbuild` `last_build` datetime(0) null default null after `date`,
    change column `buildstate` `build_state` tinyint(4) null default -1 after `description`,
    add column `build_args` varchar(255) null after `pom`;

-- rename column name deploy to launch
alter table `t_flink_app`
    change column `deploy` `launch` tinyint null default 2 after `create_time`,
    add column `build` tinyint default '1' after `launch`,
    add column `flink_cluster_id` bigint default null after `k8s_hadoop_integration`;

-- change column id to auto_increment
alter table `t_flink_sql`
    change column `id` `id` bigint not null auto_increment,
    modify column `candidate` tinyint(4) not null default 1;

alter table `t_flink_log`
    change column `start_time` `option_time` datetime(0) null default null after `exception`;

-- change launch value
begin;
update `t_flink_app` set launch = 0;
commit;

-- change state value
begin;
update `t_flink_app` set state = 0 where state in (1,2);
commit;

begin;
update `t_flink_app` set state = state - 2 where state > 1;
commit;

-- t_setting
begin;
update `t_setting` set `num` = `num` + 2 where `num` > 1;
commit;

begin;
insert into `t_setting` values (2, 'streamx.maven.auth.user', null, 'Maven Central Repository Auth User', 'Maven 私服认证用户名', 1);
insert into `t_setting` values (3, 'streamx.maven.auth.password', null, 'Maven Central Repository Auth Password', 'Maven 私服认证密码', 1);
commit;

-- change table auto_increment to 100000
begin;
alter table t_app_backup auto_increment = 100000 ;
alter table t_flame_graph auto_increment = 100000 ;
alter table t_flink_app auto_increment = 100000 ;
alter table t_flink_config auto_increment = 100000 ;
alter table t_flink_effective auto_increment = 100000 ;
alter table t_flink_env auto_increment = 100000 ;
alter table t_flink_log auto_increment = 100000 ;
alter table t_flink_project auto_increment = 100000 ;
alter table t_flink_savepoint auto_increment = 100000 ;
alter table t_flink_sql auto_increment = 100000 ;
alter table t_flink_tutorial auto_increment = 100000 ;
alter table t_menu auto_increment = 100037 ;
alter table t_message auto_increment = 100000 ;
alter table t_role auto_increment = 100003 ;
alter table t_role_menu auto_increment = 100055 ;
alter table t_user auto_increment = 100001 ;
alter table t_user_role auto_increment = 100001 ;
alter table t_app_build_pipe auto_increment = 100000 ;
commit;
-- update table id
begin;
update t_menu set parent_id=parent_id+99999 where parent_id != '0';
update t_menu set menu_id=menu_id+99999;
update t_role set role_id=role_id+99999;
update t_role_menu set id=id+99999,role_id=role_id+99999,menu_id=menu_id+99999;
update t_user set user_id=user_id+99999;
update t_user_role set id=id+99999,role_id=role_id+99999,user_id=user_id+99999;
update t_flink_app set user_id = user_id+99999;
commit;

-- ----------------------------
-- table of t_flink_cluster
-- ----------------------------
drop table if exists `t_flink_cluster`;
create table `t_flink_cluster`(
`id`              bigint not null auto_increment,
`cluster_name`    varchar(255) collate utf8mb4_general_ci default null comment '集群名称',
`address`         text collate utf8mb4_general_ci default null comment '集群地址,http://$host:$port多个地址用,分割',
`description`     varchar(255) collate utf8mb4_general_ci default null,
`create_time`     datetime default null,
primary key (`id`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;


begin;
insert into `t_menu` values (100037, 100013, 'Add Cluster', '/flink/setting/add_cluster', 'flink/setting/AddCluster', 'cluster:create', '', '0', '0', null, now(), now());
insert into `t_menu` values (100038, 100013, 'Edit Cluster', '/flink/setting/edit_cluster', 'flink/setting/EditCluster', 'cluster:update', '', '0', '0', null, now(), now());
commit;

set foreign_key_checks = 1;
-- ------------------------------------- version: 1.2.2 end ---------------------------------------

-- ------------------------------------- version: 1.2.3 start ---------------------------------------
set names utf8mb4;
set foreign_key_checks = 0;

begin;

-- ----------------------------
-- table of t_access_token definition
-- ----------------------------
drop table if exists `t_access_token`;
create table `t_access_token` (
`id` int not null auto_increment comment 'key',
`user_id`     bigint,
`token` varchar(1024) character set utf8mb4 collate utf8mb4_general_ci default null comment 'token',
`expire_time` datetime default null comment '过期时间',
`description` varchar(512) character set utf8mb4 collate utf8mb4_general_ci default null comment '使用场景描述',
`status` tinyint default null comment '1:enable,0:disable',
`create_time` datetime default null comment 'create time',
`modify_time` datetime default null comment 'modify time',
primary key (`id`)
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;

-- ----------------------------
-- table of t_flink_cluster
-- ----------------------------
alter table `t_flink_cluster`
change column `address` `address` varchar(255) character set utf8mb4 not null,
add column  `cluster_id` varchar(255) default null comment 'session模式的clusterId(yarn-session:application-id,k8s-session:cluster-id)',
add column  `options` text comment '参数集合json形式',
add column  `yarn_queue` varchar(100) default null comment '任务所在yarn队列',
add column  `execution_mode` tinyint(4) not null default '1' comment 'session类型(1:remote,3:yarn-session,5:kubernetes-session)',
add column  `version_id` bigint(20) not null comment 'flink对应id',
add column  `k8s_namespace` varchar(255) default 'default' comment 'k8s namespace',
add column  `service_account` varchar(50) default null comment 'k8s service account',
add column  `user_id` bigint(20) default null,
add column  `flink_image` varchar(255) default null comment 'flink使用镜像',
add column  `dynamic_options` text comment '动态参数',
add column  `k8s_rest_exposed_type` tinyint(4) default '2' comment 'k8s 暴露类型(0:loadbalancer,1:clusterIp,2:nodePort)',
add column  `k8s_hadoop_integration` tinyint(4) default '0',
add column  `flame_graph` tinyint(4) default '0' comment '是否开启火焰图，默认不开启',
add column  `k8s_conf` varchar(255) default null comment 'k8s配置文件所在路径',
add column  `resolve_order` int(11) default null,
add column  `exception` text comment '异常信息',
add column  `cluster_state` tinyint(4) default '0' comment '集群状态(0:创建未启动,1:已启动,2:停止)',
add unique index `inx_name`(`cluster_name`),
add unique index `inx_cluster`(`cluster_id`, `address`, `execution_mode`);

insert into `t_menu` values (100038, 100000, 'Token Management', '/system/token', 'system/token/Token', 'token:view', 'lock', '0', '1', 1.0, now(), now());
insert into `t_menu` values (100039, 100038, 'add', null, null, 'token:add', null, '1', '1', null, now(), null);
insert into `t_menu` values (100040, 100038, 'delete', null, null, 'token:delete', null, '1', '1', null, now(), null);
insert into `t_menu` values (100041, 100013, 'Add Cluster', '/flink/setting/add_cluster', 'flink/setting/AddCluster', 'cluster:create', '', '0', '0', null, now(), now());
insert into `t_menu` values (100042, 100013, 'Edit Cluster', '/flink/setting/edit_cluster', 'flink/setting/EditCluster', 'cluster:update', '', '0', '0', null, now(), now());

insert into `t_role_menu` values (100057, 100000, 100038);
insert into `t_role_menu` values (100058, 100000, 100039);
insert into `t_role_menu` values (100059, 100000, 100040);
insert into `t_role_menu` values (100060, 100000, 100041);
insert into `t_role_menu` values (100061, 100000, 100042);

commit;

set foreign_key_checks = 1;

-- ------------------------------------- version: 1.2.4 start ---------------------------------------
set names utf8mb4;
set foreign_key_checks = 0;

drop table if exists `t_alert_config`;
create table `t_alert_config` (
  `id`                   bigint   not null auto_increment primary key,
  `user_id`              bigint   default null,
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
  index `inx_user_id` (`user_id`) using btree
) engine = innodb
  default charset = utf8mb4
  collate = utf8mb4_general_ci;

begin;
-- 增加 alert_id 字段
alter table t_flink_app add column alert_id bigint after end_time;

-- 转存历史邮件报警配置
insert into t_alert_config(user_id, alert_name, alert_type, email_params)
select a.user_id, concat('emailAlertConf-', (@rownum := @rownum + 1)) as alert_name, 1 as alert_type, a.alert_email
from (select user_id, alert_email from t_flink_app where alert_email is not null group by user_id, alert_email) a,
     (select @rownum := 0) t;

-- 更新原表邮件配置 id
update t_flink_app a inner join t_alert_config b on a.alert_email = b.email_params
    set a.alert_id = b.id
where a.alert_email = b.email_params;

-- 调整报警配置表 params 内容
update t_alert_config
set email_params     = concat('{"contacts":"', email_params, '"}'),
    ding_talk_params = '{}',
    we_com_params='{}',
    lark_params='{}'
where alert_type = 1;
-- 删除原 alert_email 字段
alter table t_flink_app drop column alert_email;

alter table `t_flink_app` add column `option_time` datetime default null after `create_time`;
alter table t_setting modify column `value` text ;
insert into `t_setting` values (14, 'docker.register.namespace', null, 'Docker Register Image namespace', 'Docker命名空间', 1);
alter table `t_flink_app` add column `ingress_template` text collate utf8mb4_general_ci comment 'ingress模版文件';
alter table `t_flink_app` add column `default_mode_ingress` text collate utf8mb4_general_ci comment '配置ingress的域名';
alter table `t_flink_app` add column `modify_time` datetime not null default current_timestamp on update current_timestamp after create_time;


-- 项目组
drop table if exists `t_team`;
create table `t_team`
(
    `team_id`     bigint       not null auto_increment comment 'id',
    `team_code`   varchar(255) not null comment '团队标识 后续可以用于队列 资源隔离相关',
    `team_name`   varchar(255) not null comment '团队名',
    `create_time` datetime     not null comment '创建时间',
    primary key (`team_id`) using btree,
    unique key `team_code` (team_code) using btree
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci;

insert into t_team values (1,'bigdata','BIGDATA','2022-02-21 18:00:00');

-- 组与user 的对应关系
drop table if exists `t_team_user`;
create table `t_team_user`
(
    `team_id`    bigint   not null,
    `user_id`     bigint   not null,
    `create_time` datetime not null,
    unique key `group_user` (`team_id`,`user_id`) using btree
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci;


-- 给 app 和 project 加字段
alter table `t_flink_app` add column `team_id` bigint not null default 1 comment '任务所属组';
alter table `t_flink_project` add column `team_id` bigint not null default 1 comment '项目所属组';



-- 添加用户组管理的权限
insert into `t_menu` values (100043, 100000, 'Team Management', '/system/team', 'system/team/Team', 'team:view', 'team', '0', '1', 1, now(), null);
insert into `t_menu` values (100044, 100043, 'add', null, null, 'team:add', null, '1', '1', null, now(), null);
insert into `t_menu` values (100045, 100043, 'update', null, null, 'team:update', null, '1', '1', null, now(), null);
insert into `t_menu` values (100046, 100043, 'delete', null, null, 'team:delete', null, '1', '1', null, now(), null);
insert into `t_menu` values (100047, 100015, 'copy', null, null, 'app:copy', null, '1', '1', null, now(), null);


-- 给Admin添加权限
insert into `t_role_menu` values (100062, 100000, 100043);
insert into `t_role_menu` values (100063, 100000, 100044);
insert into `t_role_menu` values (100064, 100000, 100045);
insert into `t_role_menu` values (100065, 100000, 100046);
insert into `t_role_menu` values (100066, 100000, 100047);

-- 移除用户表联系电话字段
alter table `t_user` drop column `mobile`;


commit;

set foreign_key_checks = 1;
---------------------------------------- version: 1.2.4 end ---------------------------------------
