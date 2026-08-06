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

alter table `t_flink_cluster`
  add unique key `un_flink_cluster_id` (`id`) using btree;

alter table `t_flink_cluster`
  modify column `version_id` bigint null
    comment 'flink version id; null for managed environment';

create table if not exists `t_cloud_account` (
  `id` bigint not null auto_increment,
  `account_name` varchar(128) not null,
  `provider_type` varchar(32) not null,
  `region` varchar(64) not null,
  `endpoint` varchar(255) default null,
  `access_key_ciphertext` text not null,
  `secret_key_ciphertext` text not null,
  `credential_key_version` int not null,
  `access_key_mask` varchar(64) not null,
  `connectivity_state` tinyint not null default 0,
  `last_check_time` datetime default null,
  `last_error_code` varchar(64) default null,
  `last_error_message` varchar(512) default null,
  `status` tinyint not null default 1,
  `description` varchar(255) default null,
  `create_user_id` bigint not null,
  `create_time` datetime default null comment 'create time',
  `modify_time` datetime default null comment 'modify time',
  `version` int not null default 0,
  primary key (`id`) using btree,
  unique key `un_cloud_account_provider_name` (`provider_type`, `account_name`) using btree
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;

create table if not exists `t_cloud_account_team` (
  `cloud_account_id` bigint not null,
  `team_id` bigint not null,
  `permission_level` varchar(16) not null default 'USE',
  `create_user_id` bigint not null,
  `create_time` datetime default null comment 'create time',
  primary key (`cloud_account_id`, `team_id`) using btree,
  key `inx_cloud_account_team_team` (`team_id`) using btree,
  constraint `fk_cloud_account_team_account`
    foreign key (`cloud_account_id`) references `t_cloud_account` (`id`) on delete cascade,
  constraint `fk_cloud_account_team_team`
    foreign key (`team_id`) references `t_team` (`id`) on delete cascade
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci;

create table if not exists `t_managed_flink_env` (
  `cluster_id` bigint not null,
  `provider_type` varchar(32) not null,
  `cloud_account_id` bigint not null,
  `region` varchar(64) not null,
  `provider_config_json` text not null,
  `provider_config_version` int not null default 1,
  `console_url` varchar(512) default null,
  `capability_json` text default null,
  `last_probe_time` datetime default null,
  `last_probe_error` varchar(512) default null,
  `version` int not null default 0,
  primary key (`cluster_id`) using btree,
  key `inx_managed_flink_env_account` (`cloud_account_id`) using btree,
  constraint `fk_managed_flink_env_cluster`
    foreign key (`cluster_id`) references `t_flink_cluster` (`id`) on delete cascade,
  constraint `fk_managed_flink_env_account`
    foreign key (`cloud_account_id`) references `t_cloud_account` (`id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci;

create table if not exists `t_managed_flink_app` (
  `app_id` bigint not null,
  `managed_env_id` bigint not null,
  `provider_type` varchar(32) not null,
  `external_draft_id` varchar(128) default null,
  `external_application_id` varchar(128) default null,
  `external_instance_id` varchar(128) default null,
  `engine_version` varchar(64) not null,
  `execution_mode` varchar(32) not null,
  `runtime_config_json` text not null,
  `release_config_json` text not null,
  `local_definition_hash` char(64) not null,
  `deployed_definition_hash` char(64) default null,
  `provider_definition_hash` varchar(128) default null,
  `provider_raw_state` varchar(64) default null,
  `sync_state` varchar(32) default null,
  `last_sync_time` datetime default null,
  `consecutive_sync_failures` int not null default 0,
  `next_sync_time` datetime default null,
  `sync_owner` varchar(128) default null,
  `sync_lease_until` datetime default null,
  `console_url` varchar(512) default null,
  `version` int not null default 0,
  primary key (`app_id`) using btree,
  key `inx_managed_flink_app_env` (`managed_env_id`) using btree,
  key `inx_managed_flink_app_sync` (`sync_state`, `next_sync_time`) using btree,
  constraint `fk_managed_flink_app_application`
    foreign key (`app_id`) references `t_flink_app` (`id`) on delete cascade,
  constraint `fk_managed_flink_app_environment`
    foreign key (`managed_env_id`) references `t_managed_flink_env` (`cluster_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci;

create table if not exists `t_managed_flink_artifact` (
  `id` bigint not null auto_increment,
  `managed_env_id` bigint not null,
  `source_resource_id` bigint default null,
  `checksum` char(64) not null,
  `file_name` varchar(255) not null,
  `file_size` bigint not null,
  `provider_artifact_id` varchar(128) default null,
  `provider_artifact_version` int default null,
  `provider_uri` varchar(1024) default null,
  `provider_request_id` varchar(128) default null,
  `state` varchar(16) not null,
  `reference_count` int not null default 0,
  `stage_attempts` int not null default 0,
  `last_error_code` varchar(128) default null,
  `last_error_message` varchar(512) default null,
  `staging_owner` varchar(128) default null,
  `staging_lease_until` datetime default null,
  `version` int not null default 0,
  `create_time` datetime default null comment 'create time',
  `modify_time` datetime default null comment 'modify time',
  primary key (`id`) using btree,
  unique key `un_managed_flink_artifact_env_checksum`
    (`managed_env_id`, `checksum`) using btree,
  key `inx_managed_flink_artifact_state`
    (`state`, `staging_lease_until`) using btree,
  constraint `fk_managed_flink_artifact_environment`
    foreign key (`managed_env_id`) references `t_managed_flink_env` (`cluster_id`),
  constraint `fk_managed_flink_artifact_resource`
    foreign key (`source_resource_id`) references `t_resource` (`id`) on delete set null
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;

create table if not exists `t_managed_flink_snapshot` (
  `id` bigint not null auto_increment,
  `app_id` bigint not null,
  `external_snapshot_id` varchar(128) not null,
  `external_instance_id` varchar(128) default null,
  `snapshot_type` varchar(32) not null,
  `state` varchar(32) not null,
  `provider_state` varchar(64) default null,
  `location` varchar(1024) default null,
  `description` varchar(512) default null,
  `is_latest` tinyint not null default 0,
  `trigger_time` datetime default null,
  `completion_time` datetime default null,
  `metadata_json` text default null,
  `create_time` datetime default null comment 'create time',
  `modify_time` datetime default null comment 'modify time',
  primary key (`id`) using btree,
  unique key `un_managed_flink_snapshot_external`
    (`app_id`, `external_snapshot_id`) using btree,
  key `inx_managed_flink_snapshot_state`
    (`app_id`, `state`, `trigger_time`) using btree,
  constraint `fk_managed_flink_snapshot_application`
    foreign key (`app_id`) references `t_flink_app` (`id`) on delete cascade
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;

create table if not exists `t_managed_flink_state_event` (
  `id` bigint not null auto_increment,
  `app_id` bigint not null,
  `event_key` char(64) not null,
  `from_state` varchar(32) not null,
  `to_state` varchar(32) not null,
  `external_instance_id` varchar(128) default null,
  `alert_state` varchar(16) not null,
  `alert_attempts` int not null default 0,
  `last_error_code` varchar(128) default null,
  `create_time` datetime not null,
  `modify_time` datetime not null,
  primary key (`id`) using btree,
  unique key `un_managed_flink_state_event_key` (`event_key`) using btree,
  key `inx_managed_flink_state_event_app` (`app_id`, `create_time`) using btree,
  key `inx_managed_flink_state_event_alert` (`alert_state`, `modify_time`) using btree,
  constraint `fk_managed_flink_state_event_application`
    foreign key (`app_id`) references `t_flink_app` (`id`) on delete cascade
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;

create table if not exists `t_managed_flink_operation` (
  `id` bigint not null auto_increment,
  `app_id` bigint not null,
  `operation_type` varchar(32) not null,
  `idempotency_key` varchar(255) not null,
  `request_hash` char(64) not null,
  `parent_operation_id` bigint default null,
  `provider_request_id` varchar(128) default null,
  `provider_operation_id` varchar(128) default null,
  `state` varchar(16) not null,
  `request_json` text not null,
  `result_json` text default null,
  `error_code` varchar(128) default null,
  `error_message` varchar(512) default null,
  `retry_count` int not null default 0,
  `next_retry_time` datetime default null,
  `execution_owner` varchar(128) default null,
  `execution_lease_until` datetime default null,
  `create_user_id` bigint not null,
  `create_time` datetime not null,
  `start_time` datetime default null,
  `finish_time` datetime default null,
  `modify_time` datetime not null,
  `version` int not null default 0,
  primary key (`id`) using btree,
  unique key `un_managed_flink_operation_intent`
    (`app_id`, `operation_type`, `idempotency_key`) using btree,
  key `inx_managed_flink_operation_active`
    (`app_id`, `state`) using btree,
  key `inx_managed_flink_operation_retry`
    (`state`, `next_retry_time`) using btree,
  constraint `fk_managed_flink_operation_application`
    foreign key (`app_id`) references `t_flink_app` (`id`) on delete cascade,
  constraint `fk_managed_flink_operation_parent`
    foreign key (`parent_operation_id`) references `t_managed_flink_operation` (`id`)
) engine=innodb auto_increment=100000 default charset=utf8mb4 collate=utf8mb4_general_ci;

insert ignore into `t_menu`
values (140500, 140000, 'setting.cloudAccount.title', '/setting/cloud-account',
        'setting/cloud-account/View', 'cloud-account:view', null, '0', 1, 7, now(), now());
insert ignore into `t_menu`
values (140501, 140500, 'cloud account view', null, null,
        'cloud-account:view', null, '1', 1, null, now(), now());
insert ignore into `t_menu`
values (140502, 140500, 'cloud account create', null, null,
        'cloud-account:create', null, '1', 1, null, now(), now());
insert ignore into `t_menu`
values (140503, 140500, 'cloud account update', null, null,
        'cloud-account:update', null, '1', 1, null, now(), now());
insert ignore into `t_menu`
values (140504, 140500, 'cloud account delete', null, null,
        'cloud-account:delete', null, '1', 1, null, now(), now());
insert ignore into `t_menu`
values (140505, 140500, 'cloud account grant', null, null,
        'cloud-account:grant', null, '1', 1, null, now(), now());

set foreign_key_checks = 1;
