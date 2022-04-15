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

import com.streamxhub.streamx.console.core.entity.ApplicationBackUp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author benjobs
 */
public interface ApplicationBackUpMapper extends BaseMapper<ApplicationBackUp> {

    @Select("SELECT * from t_app_backup where app_id=#{appId}")
    IPage<ApplicationBackUp> page(Page<ApplicationBackUp> page, @Param("appId") Long appId);

    @Select("SELECT * from t_app_backup where app_id=#{appId} order by create_time desc limit 1")
    ApplicationBackUp getLastBackup(@Param("appId") Long appId);

    @Delete("delete from t_app_backup where app_id=#{appId}")
    void removeApp(@Param("appId") Long appId);

    @Select("SELECT * from t_app_backup where app_id=#{appId} and sql_id=#{sqlId}")
    ApplicationBackUp getFlinkSqlBackup(@Param("appId") Long appId, @Param("sqlId") Long sqlId);

}
