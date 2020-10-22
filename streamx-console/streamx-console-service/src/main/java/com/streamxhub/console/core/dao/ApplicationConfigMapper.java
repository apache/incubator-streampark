/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.console.core.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.streamxhub.console.core.entity.Application;
import com.streamxhub.console.core.entity.ApplicationConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author benjobs
 */
public interface ApplicationConfigMapper extends BaseMapper<ApplicationConfig> {

    @Select("select max(`version`) as lastVersion from t_flink_config where app_id=#{appId}")
    Integer getLastVersion(@Param("appId") Long appId);

    @Update("update t_flink_config set actived=0 where app_id=#{appId}")
    void standby(@Param("appId") Long id);

    @Select("select * from t_flink_config where app_id=#{appId} and actived=1")
    ApplicationConfig getActived(@Param("appId")Long id);

    @Update("update t_flink_config set actived=1 where id=#{id}")
    void active(@Param("id")Long id);

    @Select("select * from t_flink_config where app_id=#{app.appId}")
    IPage<ApplicationConfig> page(Page<ApplicationConfig> page, Application app);
}
