/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.dao;

import com.streamxhub.streamx.console.core.entity.FlinkSql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author benjobs
 */
public interface FlinkSqlMapper extends BaseMapper<FlinkSql> {
    @Select("select s.* from t_flink_sql s inner join t_flink_effective e on s.id = e.target_id where e.app_id=#{appId} and e.target_type=2")
    FlinkSql getEffective(@Param("appId") Long appId);

    @Select("select max(`version`) as maxVersion from t_flink_sql where app_id=#{appId}")
    Integer getLastVersion(@Param("appId") Long appId);

    @Select("select * from t_flink_sql where app_id=#{appId} and candidate>0 ")
    FlinkSql getCandidate(@Param("appId") Long appId);

    @Select("select * from t_flink_sql where app_id=#{appId} and candidate=#{candidate}")
    FlinkSql getCandidateByType(@Param("appId") Long appId, @Param("candidate") Integer candidate);

    @Update("update t_flink_sql set candidate=0 where id=#{id}")
    void cleanCandidate(@Param("id") Long id);

    @Delete("delete from t_flink_sql where app_id=#{appId}")
    void removeApp(@Param("appId") Long appId);

}
