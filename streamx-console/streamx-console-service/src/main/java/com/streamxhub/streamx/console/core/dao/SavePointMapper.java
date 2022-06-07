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

import com.streamxhub.streamx.console.core.entity.SavePoint;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/**
 * @author benjobs
 */
public interface SavePointMapper extends BaseMapper<SavePoint> {

    @Update("update t_flink_savepoint set latest=0 where app_id=#{appId}")
    void obsolete(@Param("appId") Long appId);

    @Select("select * from t_flink_savepoint where app_id=#{appId} and latest=1")
    SavePoint getLatest(@Param("appId") Long appId);

    @Select("select * from t_flink_savepoint where app_id=#{appId}")
    IPage<SavePoint> page(Page<SavePoint> page, @Param("appId") Long appId);

    @Delete("delete from t_flink_savepoint where app_id=#{appId}")
    void removeApp(@Param("appId") Long appId);

    @Delete("delete from t_flink_savepoint where app_id=#{appId} and trigger_time < #{trigger} and type = 1")
    void expire(@Param("appId") Long appId, @Param("trigger") Date triggerTime);

    @Delete("delete from t_flink_savepoint where app_id=#{appId} and type = 1")
    void expireAll(@Param("appId") Long appId);
}
