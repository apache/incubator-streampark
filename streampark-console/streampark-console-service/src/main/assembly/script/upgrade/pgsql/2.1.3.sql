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

alter table "public"."t_flink_project"
    drop column "git_credential";

update "public"."t_menu" set menu_name='Apache Flink',order_num=1 where menu_id = 120000;
update "public"."t_menu" set order_num=3 where menu_id = 110000;
update "public"."t_menu" set order_num=2 where menu_id = 130000;
delete from "public"."t_menu" where menu_id=110300;
