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

UPDATE t_flink_app
SET flink_cluster_id = t_flink_cluster.id
    FROM t_flink_cluster
WHERE t_flink_app.cluster_id = t_flink_cluster.cluster_id
  AND t_flink_app.execution_mode = 5;

UPDATE t_flink_app
SET cluster_id = app_id
WHERE execution_mode IN (2,3,4);

ALTER TABLE t_flink_app DROP COLUMN app_id;
