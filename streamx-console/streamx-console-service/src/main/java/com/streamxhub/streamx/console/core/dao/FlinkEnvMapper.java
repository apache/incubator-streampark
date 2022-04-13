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

import com.streamxhub.streamx.console.core.entity.FlinkEnv;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author benjobs
 */
public interface FlinkEnvMapper extends BaseMapper<FlinkEnv> {

    /**
     * 设置为默认
     *
     * @param id
     */
    @Update("update t_flink_env set is_default = case id when #{id} then 1 else 0 end")
    void setDefault(@Param("id") Long id);

    /**
     * 根据appId获取对象的flinkVersion
     *
     * @param appId
     * @return
     */
    @Select("select v.* from t_flink_env v inner join (select version_id from t_flink_app where id=#{appId}) as t on v.id = t.version_id")
    FlinkEnv getByAppId(@Param("appId") Long appId);
}
