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

-- ISSUE-2192 DDL & DML Start

alter table "public"."t_flink_savepoint" alter column "path" type varchar(1024) collate "pg_catalog"."default";

insert into "public"."t_menu" values (100070, 100015, 'savepoint trigger', null, null, 'savepoint:trigger', null, '1', '1', null, now(), now());

-- ISSUE-2192 DDL & DML End


-- ISSUE-2366 DDL & DML Start
alter table "public"."t_flink_app" rename "launch" to "release";
update "public"."t_menu" set "menu_name"='release',"perms" = 'app:release' where "menu_id" = 100025;
-- ISSUE-2366 DDL & DML End


-- Issue-2191/2215 Start
drop table if exists "public"."t_external_link";
drop sequence if exists "public"."streampark_t_external_link_id_seq";
-- ----------------------------
-- table structure for t_external_link
-- ----------------------------
create sequence "public"."streampark_t_external_link_id_seq"
    increment 1 start 10000 cache 1 minvalue 10000 maxvalue 9223372036854775807;

create table "public"."t_external_link" (
  "id" int8 not null default nextval('streampark_t_external_link_id_seq'::regclass),
  "badge_label" varchar(100) collate "pg_catalog"."default",
  "badge_name" varchar(100) collate "pg_catalog"."default",
  "badge_color" varchar(100) collate "pg_catalog"."default",
  "link_url" varchar(1000) collate "pg_catalog"."default",
  "create_time" timestamp(6) not null default timezone('UTC-8'::text, (now())::timestamp(0) without time zone),
  "modify_time" timestamp(6) not null default timezone('UTC-8'::text, (now())::timestamp(0) without time zone))
;
alter table "public"."t_external_link" add constraint "t_external_link_pkey" primary key ("id");

insert into "public"."t_menu" values (100071, 100033, 'link view', null, null, 'externalLink:view', null, '1', '1', NULL, now(), now());
insert into "public"."t_menu" values (100072, 100033, 'link create', null, null, 'externalLink:create', null, '1', '1', NULL, now(), now());
insert into "public"."t_menu" values (100073, 100033, 'link update', null, null, 'externalLink:update', null, '1', '1', NULL, now(), now());
insert into "public"."t_menu" values (100074, 100033, 'link delete', null, null, 'externalLink:delete', null, '1', '1', NULL, now(), now());

insert into "public"."t_role_menu" values (100061, 100002, 100071);
insert into "public"."t_role_menu" values (100062, 100002, 100072);
insert into "public"."t_role_menu" values (100063, 100002, 100073);
insert into "public"."t_role_menu" values (100064, 100002, 100074);
-- Issue-2191/2215 End

-- ISSUE-2401 Start
insert into "public"."t_menu" values (100075, 100015, 'sql delete', null, null, 'sql:delete', null, '1', '1', null, now(), now());
insert into "public"."t_role_menu" values (100065, 100001, 100075);
insert into "public"."t_role_menu" values (100066, 100002, 100075);
-- ISSUE-2401 End

-- Issue-2324 Start --
insert into "public"."t_menu" values (100076, 100033, 'add yarn queue', null, null, 'yarnQueue:create', '', '1', '0', null, now(), now());
insert into "public"."t_menu" values (100077, 100033, 'edit yarn queue', null, null, 'yarnQueue:update', '', '1', '0', null, now(), now());
insert into "public"."t_menu" values (100078, 100033, 'delete yarn queue', null, null, 'yarnQueue:delete', '', '1', '0', null, now(), now());

insert into "public"."t_role_menu" values (100067, 100002, 100076);
insert into "public"."t_role_menu" values (100068, 100002, 100077);
insert into "public"."t_role_menu" values (100069, 100002, 100078);

drop table if exists "public"."t_yarn_queue";

drop sequence if exists "public"."streampark_t_yarn_queue_id_seq";

-- ----------------------------
-- table structure for t_yarn_queue
-- ----------------------------
create sequence "public"."streampark_t_yarn_queue_id_seq"
    increment 1 start 10000 cache 1 minvalue 10000 maxvalue 9223372036854775807;

create table "public"."t_yarn_queue" (
    "id" int8 not null default nextval('streampark_t_yarn_queue_id_seq'::regclass),
    `team_id` int8 not null comment 'team id',
    "queue_label" varchar(255) not null collate "pg_catalog"."default",
    "description" varchar(512) collate "pg_catalog"."default",
    "create_time" timestamp(6) not null default timezone('UTC-8'::text, (now())::timestamp(0) without time zone),
    "modify_time" timestamp(6) not null default timezone('UTC-8'::text, (now())::timestamp(0) without time zone)
)
;
comment on column "public"."t_yarn_queue"."id" is 'queue id';
comment on column "public"."t_yarn_queue"."team_id" is 'team id';
comment on column "public"."t_yarn_queue"."queue_label" is 'queue label expression';
comment on column "public"."t_yarn_queue"."description" is 'description of the queue';
comment on column "public"."t_yarn_queue"."create_time" is 'create time';
comment on column "public"."t_yarn_queue"."modify_time" is 'modify time';

alter table "public"."t_yarn_queue" add constraint "t_yarn_queue_pkey" primary key ("id");
alter table "public"."t_yarn_queue" add constraint "unique_team_id_queue_label" unique("team_id", "queue_label");

-- Issue-2324 End --
