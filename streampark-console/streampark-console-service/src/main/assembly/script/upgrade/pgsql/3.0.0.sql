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

create unique index if not exists "un_flink_cluster_id"
  on "public"."t_flink_cluster" using btree ("id");

alter table "public"."t_flink_cluster"
  alter column "version_id" drop not null;

create sequence if not exists "public"."streampark_t_cloud_account_id_seq"
    increment 1 start 10000 cache 1 minvalue 10000 maxvalue 9223372036854775807;

create table if not exists "public"."t_cloud_account" (
  "id" int8 not null default nextval('streampark_t_cloud_account_id_seq'::regclass),
  "account_name" varchar(128) not null,
  "provider_type" varchar(32) not null,
  "region" varchar(64) not null,
  "endpoint" varchar(255),
  "access_key_ciphertext" text not null,
  "secret_key_ciphertext" text not null,
  "credential_key_version" int4 not null,
  "access_key_mask" varchar(64) not null,
  "connectivity_state" int2 not null default 0,
  "last_check_time" timestamp(6),
  "last_error_code" varchar(64),
  "last_error_message" varchar(512),
  "status" int2 not null default 1,
  "description" varchar(255),
  "create_user_id" int8 not null,
  "create_time" timestamp(6),
  "modify_time" timestamp(6),
  "version" int4 not null default 0,
  constraint "t_cloud_account_pkey" primary key ("id"),
  constraint "un_cloud_account_provider_name" unique ("provider_type", "account_name")
);

create table if not exists "public"."t_cloud_account_team" (
  "cloud_account_id" int8 not null,
  "team_id" int8 not null,
  "permission_level" varchar(16) not null default 'USE',
  "create_user_id" int8 not null,
  "create_time" timestamp(6),
  constraint "t_cloud_account_team_pkey" primary key ("cloud_account_id", "team_id"),
  constraint "fk_cloud_account_team_account"
    foreign key ("cloud_account_id") references "public"."t_cloud_account" ("id") on delete cascade,
  constraint "fk_cloud_account_team_team"
    foreign key ("team_id") references "public"."t_team" ("id") on delete cascade
);

create index if not exists "inx_cloud_account_team_team"
  on "public"."t_cloud_account_team" using btree ("team_id");

create table if not exists "public"."t_managed_flink_env" (
  "cluster_id" int8 not null,
  "provider_type" varchar(32) not null,
  "cloud_account_id" int8 not null,
  "region" varchar(64) not null,
  "project_id" varchar(128) not null,
  "project_name" varchar(128),
  "resource_pool_id" varchar(128) not null,
  "resource_pool_name" varchar(128),
  "draft_directory_id" int8 not null,
  "console_url" varchar(512),
  "capability_json" text,
  "last_probe_time" timestamp(6),
  "last_probe_error" varchar(512),
  "version" int4 not null default 0,
  constraint "t_managed_flink_env_pkey" primary key ("cluster_id"),
  constraint "fk_managed_flink_env_cluster"
    foreign key ("cluster_id") references "public"."t_flink_cluster" ("id") on delete cascade,
  constraint "fk_managed_flink_env_account"
    foreign key ("cloud_account_id") references "public"."t_cloud_account" ("id")
);

create index if not exists "inx_managed_flink_env_account"
  on "public"."t_managed_flink_env" using btree ("cloud_account_id");

create table if not exists "public"."t_managed_flink_app" (
  "app_id" int8 not null,
  "managed_env_id" int8 not null,
  "provider_type" varchar(32) not null,
  "external_draft_id" varchar(128),
  "external_application_id" varchar(128),
  "external_instance_id" varchar(128),
  "engine_version" varchar(64) not null,
  "execution_mode" varchar(32) not null,
  "runtime_config_json" text not null,
  "release_config_json" text not null,
  "local_definition_hash" char(64) not null,
  "deployed_definition_hash" char(64),
  "provider_definition_hash" varchar(128),
  "provider_raw_state" varchar(64),
  "sync_state" varchar(32),
  "last_sync_time" timestamp(6),
  "consecutive_sync_failures" int4 not null default 0,
  "next_sync_time" timestamp(6),
  "sync_owner" varchar(128),
  "sync_lease_until" timestamp(6),
  "console_url" varchar(512),
  "version" int4 not null default 0,
  constraint "t_managed_flink_app_pkey" primary key ("app_id"),
  constraint "fk_managed_flink_app_application"
    foreign key ("app_id") references "public"."t_flink_app" ("id") on delete cascade,
  constraint "fk_managed_flink_app_environment"
    foreign key ("managed_env_id") references "public"."t_managed_flink_env" ("cluster_id")
);

create index if not exists "inx_managed_flink_app_env"
  on "public"."t_managed_flink_app" using btree ("managed_env_id");
create index if not exists "inx_managed_flink_app_sync"
  on "public"."t_managed_flink_app" using btree ("sync_state", "next_sync_time");

create sequence if not exists "public"."streampark_t_managed_flink_artifact_id_seq"
    increment 1 start 10000 cache 1 minvalue 10000 maxvalue 9223372036854775807;

create table if not exists "public"."t_managed_flink_artifact" (
  "id" int8 not null
    default nextval('streampark_t_managed_flink_artifact_id_seq'::regclass),
  "managed_env_id" int8 not null,
  "source_resource_id" int8,
  "checksum" char(64) not null,
  "file_name" varchar(255) not null,
  "file_size" int8 not null,
  "provider_artifact_id" varchar(128),
  "provider_artifact_version" int4,
  "provider_uri" varchar(1024),
  "provider_request_id" varchar(128),
  "state" varchar(16) not null,
  "reference_count" int4 not null default 0,
  "stage_attempts" int4 not null default 0,
  "last_error_code" varchar(128),
  "last_error_message" varchar(512),
  "staging_owner" varchar(128),
  "staging_lease_until" timestamp(6),
  "version" int4 not null default 0,
  "create_time" timestamp(6),
  "modify_time" timestamp(6),
  constraint "t_managed_flink_artifact_pkey" primary key ("id"),
  constraint "un_managed_flink_artifact_env_checksum"
    unique ("managed_env_id", "checksum"),
  constraint "fk_managed_flink_artifact_environment"
    foreign key ("managed_env_id")
      references "public"."t_managed_flink_env" ("cluster_id"),
  constraint "fk_managed_flink_artifact_resource"
    foreign key ("source_resource_id")
      references "public"."t_resource" ("id") on delete set null
);

create index if not exists "inx_managed_flink_artifact_state"
  on "public"."t_managed_flink_artifact"
    using btree ("state", "staging_lease_until");

create sequence if not exists "public"."streampark_t_managed_flink_snapshot_id_seq"
    increment 1 start 10000 cache 1 minvalue 10000 maxvalue 9223372036854775807;

create table if not exists "public"."t_managed_flink_snapshot" (
  "id" int8 not null
    default nextval('streampark_t_managed_flink_snapshot_id_seq'::regclass),
  "app_id" int8 not null,
  "external_snapshot_id" varchar(128) not null,
  "external_instance_id" varchar(128),
  "snapshot_type" varchar(32) not null,
  "state" varchar(32) not null,
  "provider_state" varchar(64),
  "location" varchar(1024),
  "description" varchar(512),
  "is_latest" int2 not null default 0,
  "trigger_time" timestamp(6),
  "completion_time" timestamp(6),
  "metadata_json" text,
  "create_time" timestamp(6),
  "modify_time" timestamp(6),
  constraint "t_managed_flink_snapshot_pkey" primary key ("id"),
  constraint "un_managed_flink_snapshot_external"
    unique ("app_id", "external_snapshot_id"),
  constraint "fk_managed_flink_snapshot_application"
    foreign key ("app_id") references "public"."t_flink_app" ("id") on delete cascade
);

create index if not exists "inx_managed_flink_snapshot_state"
  on "public"."t_managed_flink_snapshot"
    using btree ("app_id", "state", "trigger_time");

create sequence if not exists "public"."streampark_t_managed_flink_state_event_id_seq"
    increment 1 start 10000 cache 1 minvalue 10000 maxvalue 9223372036854775807;

create table if not exists "public"."t_managed_flink_state_event" (
  "id" int8 not null
    default nextval('streampark_t_managed_flink_state_event_id_seq'::regclass),
  "app_id" int8 not null,
  "event_key" char(64) not null,
  "from_state" varchar(32) not null,
  "to_state" varchar(32) not null,
  "external_instance_id" varchar(128),
  "alert_state" varchar(16) not null,
  "alert_attempts" int4 not null default 0,
  "last_error_code" varchar(128),
  "create_time" timestamp(6) not null,
  "modify_time" timestamp(6) not null,
  constraint "t_managed_flink_state_event_pkey" primary key ("id"),
  constraint "un_managed_flink_state_event_key" unique ("event_key"),
  constraint "fk_managed_flink_state_event_application"
    foreign key ("app_id") references "public"."t_flink_app" ("id") on delete cascade
);

create index if not exists "inx_managed_flink_state_event_app"
  on "public"."t_managed_flink_state_event" using btree ("app_id", "create_time");
create index if not exists "inx_managed_flink_state_event_alert"
  on "public"."t_managed_flink_state_event" using btree ("alert_state", "modify_time");

create sequence if not exists "public"."streampark_t_managed_flink_operation_id_seq"
    increment 1 start 10000 cache 1 minvalue 10000 maxvalue 9223372036854775807;

create table if not exists "public"."t_managed_flink_operation" (
  "id" int8 not null
    default nextval('streampark_t_managed_flink_operation_id_seq'::regclass),
  "app_id" int8 not null,
  "operation_type" varchar(32) not null,
  "idempotency_key" varchar(255) not null,
  "request_hash" char(64) not null,
  "parent_operation_id" int8,
  "provider_request_id" varchar(128),
  "provider_operation_id" varchar(128),
  "state" varchar(16) not null,
  "request_json" text not null,
  "result_json" text,
  "error_code" varchar(128),
  "error_message" varchar(512),
  "retry_count" int4 not null default 0,
  "next_retry_time" timestamp(6),
  "execution_owner" varchar(128),
  "execution_lease_until" timestamp(6),
  "create_user_id" int8 not null,
  "create_time" timestamp(6) not null,
  "start_time" timestamp(6),
  "finish_time" timestamp(6),
  "modify_time" timestamp(6) not null,
  "version" int4 not null default 0,
  constraint "t_managed_flink_operation_pkey" primary key ("id"),
  constraint "un_managed_flink_operation_intent"
    unique ("app_id", "operation_type", "idempotency_key"),
  constraint "fk_managed_flink_operation_application"
    foreign key ("app_id") references "public"."t_flink_app" ("id") on delete cascade,
  constraint "fk_managed_flink_operation_parent"
    foreign key ("parent_operation_id")
      references "public"."t_managed_flink_operation" ("id")
);

create index if not exists "inx_managed_flink_operation_active"
  on "public"."t_managed_flink_operation" using btree ("app_id", "state");
create index if not exists "inx_managed_flink_operation_retry"
  on "public"."t_managed_flink_operation" using btree ("state", "next_retry_time");

drop trigger if exists "streampark_t_cloud_account_modify_time_tri"
  on "public"."t_cloud_account";
create trigger "streampark_t_cloud_account_modify_time_tri"
  before update on "public"."t_cloud_account"
  for each row execute procedure "public"."update_modify_time"();

insert into "public"."t_menu"
values (130700, 130000, 'setting.cloudAccount.title', '/setting/cloud-account',
        'setting/cloud-account/View', 'cloud-account:view', 'cloud', '0', '1', 7, now(), now())
on conflict ("menu_id") do nothing;
insert into "public"."t_menu"
values (130701, 130700, 'cloud account view', null, null,
        'cloud-account:view', null, '1', '1', null, now(), now())
on conflict ("menu_id") do nothing;
insert into "public"."t_menu"
values (130702, 130700, 'cloud account create', null, null,
        'cloud-account:create', null, '1', '1', null, now(), now())
on conflict ("menu_id") do nothing;
insert into "public"."t_menu"
values (130703, 130700, 'cloud account update', null, null,
        'cloud-account:update', null, '1', '1', null, now(), now())
on conflict ("menu_id") do nothing;
insert into "public"."t_menu"
values (130704, 130700, 'cloud account delete', null, null,
        'cloud-account:delete', null, '1', '1', null, now(), now())
on conflict ("menu_id") do nothing;
insert into "public"."t_menu"
values (130705, 130700, 'cloud account grant', null, null,
        'cloud-account:grant', null, '1', '1', null, now(), now())
on conflict ("menu_id") do nothing;
