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

-- ----------------------------
-- Table of t_flink_gateway
-- ----------------------------
create sequence "public"."streampark_t_flink_gateway_id_seq"
    increment 1 start 10000 cache 1 minvalue 10000 maxvalue 9223372036854775807;

create table "public"."t_flink_gateway" (
                                            "id" int8 not null default nextval('streampark_t_resource_id_seq'::regclass),
                                            "gateway_name" varchar(128) collate "pg_catalog"."default" not null,
                                            "description" text collate "pg_catalog"."default" default null,
                                            "gateway_type" int4,
                                            "address" varchar(150) collate "pg_catalog"."default",
                                            "create_time" timestamp(6) not null default timezone('UTC-8'::text, (now())::timestamp(0) without time zone),
                                            "modify_time" timestamp(6) not null default timezone('UTC-8'::text, (now())::timestamp(0) without time zone)
);
comment on column "public"."t_flink_gateway"."id" is 'The id of the gateway';
comment on column "public"."t_flink_gateway"."gateway_name" is 'The name of the gateway';
comment on column "public"."t_flink_gateway"."description" is 'More detailed description of resource';
comment on column "public"."t_flink_gateway"."gateway_type" is 'The type of the gateway';
comment on column "public"."t_flink_gateway"."address" is 'url address of gateway endpoint';
comment on column "public"."t_flink_gateway"."create_time" is 'create time';
comment on column "public"."t_flink_gateway"."modify_time" is 'modify time';

alter table "public"."t_flink_gateway" add constraint "t_flink_gateway_pkey" primary key ("id");

insert into "public"."t_menu" values (120500, 130000, 'setting.flinkGateway', '/setting/FlinkGateway', 'setting/FlinkGateway/index', null, 'apartment', '0', '1', 3, now(), now());
insert into "public"."t_menu" values (110501, 110500, 'add', null, null, 'gateway:add', null, '1', '1', null, now(), now());
insert into "public"."t_menu" values (110502, 110500, 'update', null, null, 'gateway:update', null, '1', '1', null, now(), now());
insert into "public"."t_menu" values (110503, 110500, 'delete', null, null, 'gateway:delete', null, '1', '1', null, now(), now());

insert into "public"."t_role_menu" (role_id, menu_id) values (100001, 120500);
insert into "public"."t_role_menu" (role_id, menu_id) values (100001, 120501);
insert into "public"."t_role_menu" (role_id, menu_id) values (100001, 120502);
insert into "public"."t_role_menu" (role_id, menu_id) values (100001, 120503);

insert into "public"."t_role_menu" (role_id, menu_id) values (100002, 120500);
insert into "public"."t_role_menu" (role_id, menu_id) values (100002, 120501);
insert into "public"."t_role_menu" (role_id, menu_id) values (100002, 120502);
insert into "public"."t_role_menu" (role_id, menu_id) values (100002, 120503);
