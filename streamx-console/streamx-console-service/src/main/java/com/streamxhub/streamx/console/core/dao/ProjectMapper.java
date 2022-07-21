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

import com.streamxhub.streamx.console.core.entity.Project;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @author benjobs
 */
public interface ProjectMapper extends BaseMapper<Project> {

    IPage<Project> findProject(Page<Project> page, @Param("project") Project project);

    @Update("update t_flink_project set BUILD_STATE=2 where id=#{project.id}")
    void failureBuild(@Param("project") Project project);

    @Update("update t_flink_project set LAST_BUILD = now(),BUILD_STATE=1 where id=#{project.id}")
    void successBuild(@Param("project") Project project);

    @Update("update t_flink_project set BUILD_STATE=0 where id=#{project.id}")
    void startBuild(@Param("project") Project project);

    Long getCountByTeam(@Param("teamId") Long teamId);
}
