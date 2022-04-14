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

import com.streamxhub.streamx.console.core.entity.Application;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author benjobs
 */
public interface ApplicationMapper extends BaseMapper<Application> {
    IPage<Application> page(Page<Application> page, @Param("application") Application application);

    Application getApp(@Param("application") Application application);

    void updateTracking(@Param("application") Application application);

    @Select("select * from t_flink_app where project_id=#{projectId}")
    List<Application> getByProjectId(@Param("projectId") Long projectId);

    @Update("update t_flink_app set app_id=#{application.appId},job_id=#{application.jobId},state=14,end_time=null where id=#{application.id}")
    boolean mapping(@Param("application") Application appParam);

    @Update("update t_flink_app set option_state=0")
    void resetOptionState();

    @Select("select k8s_namespace from " +
            "(select k8s_namespace, max(create_time) as ct from t_flink_app " +
            "where k8s_namespace is not null group by k8s_namespace order by ct desc) as ns " +
            "limit #{limitSize}")
    List<String> getRecentK8sNamespace(@Param("limitSize") int limit);

    @Select("select cluster_id from " +
            "(select cluster_id, max(create_time) as ct from t_flink_app " +
            "where cluster_id is not null and execution_mode = #{executionMode} group by cluster_id order by ct desc) as ci " +
            "limit #{limitSize}")
    List<String> getRecentK8sClusterId(@Param("executionMode") int executionMode, @Param("limitSize") int limit);

    @Select("select flink_image from " +
            "(select flink_image, max(create_time) as ct from t_flink_app " +
            "where flink_image is not null and execution_mode = 6 group by flink_image order by ct desc) as fi " +
            "limit #{limitSize}")
    List<String> getRecentFlinkBaseImage(@Param("limitSize") int limit);

    @Select("select k8s_pod_template from " +
            "(select k8s_pod_template, max(create_time) as ct from t_flink_app " +
            "where k8s_pod_template is not null and k8s_pod_template <> '' and execution_mode = 6 " +
            "group by k8s_pod_template order by ct desc) as pt " +
            "limit #{limitSize}")
    List<String> getRecentK8sPodTemplate(@Param("limitSize") int limit);

    @Select("select k8s_jm_pod_template from " +
            "(select k8s_jm_pod_template, max(create_time) as ct from t_flink_app " +
            "where k8s_jm_pod_template is not null and k8s_jm_pod_template <> '' and execution_mode = 6 " +
            "group by k8s_jm_pod_template order by ct desc) as pt " +
            "limit #{limitSize}")
    List<String> getRecentK8sJmPodTemplate(@Param("limitSize") int limit);

    @Select("select k8s_tm_pod_template from " +
            "(select k8s_tm_pod_template, max(create_time) as ct from t_flink_app " +
            "where k8s_tm_pod_template is not null and k8s_tm_pod_template <> '' and execution_mode = 6 " +
            "group by k8s_tm_pod_template order by ct desc) as pt " +
            "limit #{limitSize}")
    List<String> getRecentK8sTmPodTemplate(@Param("limitSize") int limit);

}
