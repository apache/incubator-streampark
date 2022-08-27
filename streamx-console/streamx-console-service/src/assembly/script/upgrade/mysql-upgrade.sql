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

-- ------------------------------------- version: 1.2.3 START ---------------------------------------
set names utf8mb4;
set foreign_key_checks = 0;

-- ----------------------------
-- Table of t_access_token definition
-- ----------------------------
drop table if exists `t_access_token`;
create table `t_access_token` (
`id` int not null auto_increment comment 'key',
`user_id` bigint,
`token` varchar(1024) character set utf8mb4 collate utf8mb4_general_ci default null comment 'token',
`expire_time` datetime default null comment '过期时间',
`description` varchar(512) character set utf8mb4 collate utf8mb4_general_ci default null comment '使用场景描述',
`status` tinyint default null comment '1:enable,0:disable',
`create_time` datetime default null comment 'create time',
`modify_time` datetime default null comment 'modify time',
primary key (`id`)
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;

-- ----------------------------
-- Table of t_flink_cluster
-- ----------------------------
alter table `t_flink_cluster` change column `address` `address` varchar(255) character set utf8mb4 not null,
add column  `cluster_id` varchar(255) default null comment 'session模式的clusterid(yarn-session:application-id,k8s-session:cluster-id)',
add column  `options` text comment '参数集合json形式',
add column  `yarn_queue` varchar(100) default null comment '任务所在yarn队列',
add column  `execution_mode` tinyint not null default '1' comment 'session类型(1:remote,3:yarn-session,5:kubernetes-session)',
add column  `version_id` bigint not null comment 'flink对应id',
add column  `k8s_namespace` varchar(255) default 'default' comment 'k8s namespace',
add column  `service_account` varchar(50) default null comment 'k8s service account',
add column  `user_id` bigint default null,
add column  `flink_image` varchar(255) default null comment 'flink使用镜像',
add column  `dynamic_options` text comment '动态参数',
add column  `k8s_rest_exposed_type` tinyint default '2' comment 'k8s 暴露类型(0:loadbalancer,1:clusterip,2:nodeport)',
add column  `k8s_hadoop_integration` tinyint default '0',
add column  `flame_graph` tinyint default '0' comment '是否开启火焰图，默认不开启',
add column  `k8s_conf` varchar(255) default null comment 'k8s配置文件所在路径',
add column  `resolve_order` int(11) default null,
add column  `exception` text comment '异常信息',
add column  `cluster_state` tinyint default '0' comment '集群状态(0:创建未启动,1:已启动,2:停止)',
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

set foreign_key_checks = 1;



-- ------------------------------------- version: 1.2.4 START ---------------------------------------
set names utf8mb4;
set foreign_key_checks = 0;

drop table if exists `t_alert_config`;
create table `t_alert_config` (
  `id` bigint   not null auto_increment primary key,
  `user_id` bigint   default null,
  `alert_name` varchar(128) collate utf8mb4_general_ci default null comment '报警组名称',
  `alert_type` int default 0 comment '报警类型',
  `email_params` varchar(255) collate utf8mb4_general_ci comment '邮件报警配置信息',
  `sms_params` text collate utf8mb4_general_ci comment '短信报警配置信息',
  `ding_talk_params` text collate utf8mb4_general_ci comment '钉钉报警配置信息',
  `we_com_params` varchar(255) collate utf8mb4_general_ci comment '企微报警配置信息',
  `http_callback_params` text collate utf8mb4_general_ci comment '报警http回调配置信息',
  `lark_params` text collate utf8mb4_general_ci comment '飞书报警配置信息',
  `create_time` datetime not null default current_timestamp comment '创建时间',
  `modify_time` datetime not null default current_timestamp on update current_timestamp comment '修改时间',
  index `inx_user_id` (`user_id`) using btree
) engine = innodb default charset = utf8mb4 collate = utf8mb4_general_ci;

-- 增加 ALERT_ID 字段
alter table t_flink_app add column alert_id bigint after end_time;

-- 转存历史邮件报警配置
insert into t_alert_config(user_id, alert_name, alert_type, email_params)
select a.user_id, concat('emailAlertConf-', (@rownum := @rownum + 1)) AS alert_name, 1 as alert_type, a.alert_email
from (select user_id, alert_email from t_flink_app where alert_email is not null group by user_id, alert_email) a,
     (select @rownum := 0) t;

-- 更新原表邮件配置 id
update t_flink_app a inner join t_alert_config b on a.alert_email = b.email_params
    set a.alert_id = b.id
where a.alert_email = b.email_params;

-- 调整报警配置表 params 内容
update t_alert_config
set email_params = concat('{"contacts":"', email_params, '"}'),
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
create table `t_team` (
    `team_id` bigint       not null auto_increment comment 'id',
    `team_code` varchar(255) not null comment '团队标识 后续可以用于队列 资源隔离相关',
    `team_name` varchar(255) not null comment '团队名',
    `create_time` datetime     not null comment '创建时间',
    primary key (`team_id`) using btree,
    unique key `team_code` (team_code) using btree
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci;

insert into t_team values (1,'bigdata','BIGDATA', now());

-- 组与user 的对应关系
drop table if exists `t_team_user`;
create table `t_team_user` (
    `team_id` bigint not null,
    `user_id` bigint not null,
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


-- 给admin添加权限
insert into `t_role_menu` values (100062, 100000, 100043);
insert into `t_role_menu` values (100063, 100000, 100044);
insert into `t_role_menu` values (100064, 100000, 100045);
insert into `t_role_menu` values (100065, 100000, 100046);
insert into `t_role_menu` values (100066, 100000, 100047);

-- 移除用户表联系电话字段
alter table `t_user` drop column `mobile`;

set foreign_key_checks = 1;
---------------------------------------- version: 1.2.4 END ---------------------------------------
