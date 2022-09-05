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

package org.apache.streampark.console.core.mapper;

import org.apache.streampark.console.core.entity.FlinkSql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FlinkSqlMapper extends BaseMapper<FlinkSql> {
    @Select("select s.* from t_flink_sql s inner join t_flink_effective e on s.id = e.target_id where e.target_type=2 and e.app_id=#{appId}")
    FlinkSql getEffective(@Param("appId") Long appId);

    @Select("select max(`version`) as maxVersion from t_flink_sql where app_id=#{appId}")
    Integer getLatestVersion(@Param("appId") Long appId);

}
