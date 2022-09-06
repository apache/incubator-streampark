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

import org.apache.streampark.console.core.entity.ApplicationConfig;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ApplicationConfigMapper extends BaseMapper<ApplicationConfig> {

    @Select("select max(`version`) as lastVersion from t_flink_config where app_id=#{appId}")
    Integer getLastVersion(@Param("appId") Long appId);

    @Select("select * from t_flink_config where app_id=#{appId}")
    IPage<ApplicationConfig> page(Page<ApplicationConfig> page, @Param("appId") Long appId);

    @Select("select s.* from t_flink_config s inner join t_flink_effective e on s.id = e.target_id where e.app_id=#{appId} and e.target_type=1")
    ApplicationConfig getEffective(@Param("appId") Long appId);

    @Select("select * from t_flink_config where app_id=#{appId} and latest=true")
    ApplicationConfig getLatest(@Param("appId") Long appId);

}
