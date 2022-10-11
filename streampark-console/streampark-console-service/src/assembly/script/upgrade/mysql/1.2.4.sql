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

use streampark;

set names utf8mb4;
set foreign_key_checks = 0;

drop table if exists `t_alert_config`;
create table `t_alert_config` (
  `id` bigint   not null auto_increment primary key,
  `user_id` bigint   default null,
  `alert_name` varchar(128) collate utf8mb4_general_ci default null comment 'alert group name',
  `alert_type` int default 0 comment 'alert type',
  `email_params` varchar(255) collate utf8mb4_general_ci comment 'email params',
  `sms_params` text collate utf8mb4_general_ci comment 'sms params',
  `ding_talk_params` text collate utf8mb4_general_ci comment 'ding talk params',
  `we_com_params` varchar(255) collate utf8mb4_general_ci comment 'wechat params',
  `http_callback_params` text collate utf8mb4_general_ci comment 'http callback params',
  `lark_params` text collate utf8mb4_general_ci comment 'lark params',
  `create_time` datetime not null default current_timestamp comment 'create time',
  `modify_time` datetime not null default current_timestamp on update current_timestamp comment 'modify time',
  index `inx_user_id` (`user_id`) using btree
) engine = innodb default charset = utf8mb4 collate = utf8mb4_general_ci;

-- add alert_id field
alter table t_flink_app add column alert_id bigint after end_time;

-- dump history email alarm configuration
insert into t_alert_config(user_id, alert_name, alert_type, email_params)
select a.user_id, concat('emailAlertConf-', (@rownum := @rownum + 1)) AS alert_name, 1 as alert_type, a.alert_email
from (select user_id, alert_email from t_flink_app where alert_email is not null group by user_id, alert_email) a,
     (select @rownum := 0) t;

-- update the original table mail configuration id
update t_flink_app a inner join t_alert_config b on a.alert_email = b.email_params
    set a.alert_id = b.id
where a.alert_email = b.email_params;

-- Adjust the content of the alarm configuration table params
update t_alert_config
set email_params = concat('{"contacts":"', email_params, '"}'),
    ding_talk_params = '{}',
    we_com_params='{}',
    lark_params='{}'
where alert_type = 1;
-- remove the original alert_email field
alter table t_flink_app drop column alert_email;

alter table `t_flink_app` add column `option_time` datetime default null after `create_time`;
alter table t_setting modify column `value` text ;
insert into `t_setting` values (14, 'docker.register.namespace', null, 'Docker Register Image namespace', 'Docker命名空间', 1);
alter table `t_flink_app` add column `ingress_template` text collate utf8mb4_general_ci comment 'ingress模版文件';
alter table `t_flink_app` add column `default_mode_ingress` text collate utf8mb4_general_ci comment '配置ingress的域名';
alter table `t_flink_app` add column `modify_time` datetime not null default current_timestamp on update current_timestamp after `create_time`;
-- add tags field
alter table `t_flink_app` add column `tags` varchar(500) default null;
-- add job_manager_url field
alter table `t_flink_app` add column `job_manager_url` varchar(255) default null after `job_id`;
-- add job_manager_url field
alter table `t_flink_log` add column `job_manager_url` varchar(255) default null after `yarn_app_id`;

alter table `t_flink_project`
change column `date` `create_time` datetime default current_timestamp not null,
add column `modify_time` datetime not null default current_timestamp on update current_timestamp after `create_time`;


-- change `update_time` to `modify_time`
alter table `t_app_build_pipe` change column `update_time` `modify_time` datetime not null default current_timestamp on update current_timestamp;


-- change `readed` to `is_read`
alter table `t_message` change column `readed` `is_read` tinyint default 0;


-- add chk_id field
alter table `t_flink_savepoint` add column `chk_id` bigint after `app_id`;

-- change create_time field and modify_time field
update `t_access_token` set `modify_time` = current_timestamp where `modify_time` is null;
update `t_menu` set `modify_time` = current_timestamp where `modify_time` is null;
update `t_role` set `modify_time` = current_timestamp where `modify_time` is null;
update `t_user` set `modify_time` = current_timestamp where `modify_time` is null;

alter table `t_app_backup` modify `create_time` datetime not null default current_timestamp;
alter table `t_flink_app` modify `create_time` datetime not null default current_timestamp;
alter table `t_flink_config` modify `create_time` datetime not null default current_timestamp;
alter table `t_flink_effective` modify `create_time` datetime not null default current_timestamp;
alter table `t_flink_env` modify `create_time` datetime not null default current_timestamp;
alter table `t_flink_savepoint` modify `create_time` datetime not null default current_timestamp;
alter table `t_flink_sql` modify `create_time` datetime not null default current_timestamp;
alter table `t_flink_tutorial` modify `create_time` datetime not null default current_timestamp;
alter table `t_message` modify `create_time` datetime not null default current_timestamp;
alter table `t_role` modify `create_time` datetime not null default current_timestamp;
alter table `t_user` modify `create_time` datetime not null default current_timestamp;
alter table `t_flink_cluster` modify `create_time` datetime not null default current_timestamp;
alter table `t_access_token` modify `create_time` datetime not null default current_timestamp;

alter table `t_access_token` modify `modify_time` datetime not null default current_timestamp on update current_timestamp;
alter table `t_menu` modify `modify_time` datetime not null default current_timestamp on update current_timestamp;
alter table `t_role` modify `modify_time` datetime not null default current_timestamp on update current_timestamp;
alter table `t_user` modify `modify_time` datetime not null default current_timestamp on update current_timestamp;

-- add new modules to the menu
insert into `t_menu` values (100043, 100015, 'copy', null, null, 'app:copy', null, 1, 1, null, now(), now());
insert into `t_menu` values (100044, 100000, 'Team Management', '/system/team', 'system/team/Team', 'team:view', 'team', '0', 1, 2, now(), now());
insert into `t_menu` values (100045, 100044, 'add', null, null, 'team:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (100046, 100044, 'update', null, null, 'team:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (100047, 100044, 'delete', null, null, 'team:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (100048, 100000, 'Member Management', '/system/member', 'system/member/Member', 'member:view', 'usergroup-add', '0', 1, 2, now(), now());
insert into `t_menu` values (100049, 100048, 'add', null, null, 'member:add', null, '1', 1, null, now(), now());
insert into `t_menu` values (100050, 100048, 'update', null, null, 'member:update', null, '1', 1, null, now(), now());
insert into `t_menu` values (100051, 100048, 'delete', null, null, 'member:delete', null, '1', 1, null, now(), now());
insert into `t_menu` values (100052, 100048, 'role view', null, null, 'role:view', null, '1', 1, null, now(), now());
insert into `t_menu` values (100053, 100001, 'types', null, null, 'user:types', null, '1', 1, null, now(), now());

-- after adding team module, admin has all permissions by default, so admin does not need to add permissions separately, so delete admin related roles and associations
delete from t_role_menu where role_id = 100000;
delete from t_role where role_id = 100000;

-- remove user table contact phone field
alter table `t_user` drop column `mobile`;

-- update t_menu name
update `t_menu` set `menu_name` = 'Edit StreamPark App', `path` = '/flink/app/edit_streampark', `component` = 'flink/app/EditStreamPark'
where `menu_id` = 100021;

update `t_menu` set `menu_name` = 'StreamPark' where `menu_id` = 100013;

insert into `t_setting` values (15, 'streampark.maven.settings', null, 'Maven Settings File Path', 'Maven Settings.xml 完整路径', 1);

-- update the index field for t_user;
alter table `t_user` drop index `un_username`;
alter table `t_user`
modify `username` varchar(255) collate utf8mb4_general_ci not null comment 'user name',
add unique key `un_username` (`username`) using btree;

-- add team_id for t_user;
alter table `t_user` add column `team_id` bigint default null comment 'latest team id' after `user_type`;

-- alter t_role_user to t_member
alter table t_user_role rename t_member;

set foreign_key_checks = 1;

