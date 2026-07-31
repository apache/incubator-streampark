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

import org.apache.streampark.console.core.entity.ManagedFlinkApplication;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationStatisticsView;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/** Persistence mapper for managed Flink application extensions. */
public interface ManagedFlinkApplicationMapper extends BaseMapper<ManagedFlinkApplication> {

    /** Serializes write-operation admission for a single managed application. */
    @Select("select app_id from t_managed_flink_app where app_id = #{appId} for update")
    Long lockByAppId(@Param("appId") Long appId);

    /** Aggregates managed application state without registering jobs in legacy watchers. */
    @Select({
            "select count(1) as total,",
            "coalesce(sum(case when app.state = 5 then 1 else 0 end), 0) as running,",
            "coalesce(sum(case when managed.sync_state = 'HEALTHY' then 1 else 0 end), 0) as healthy,",
            "coalesce(sum(case when managed.sync_state = 'DEGRADED' then 1 else 0 end), 0) as degraded,",
            "coalesce(sum(case when managed.sync_state = 'NOT_FOUND' then 1 else 0 end), 0) as not_found,",
            "coalesce(sum(case when managed.sync_state = 'DRIFTED' then 1 else 0 end), 0) as drifted,",
            "coalesce(sum(case when managed.sync_state = 'PENDING' or managed.sync_state is null then 1 else 0 end), 0) as pending",
            "from t_flink_app app",
            "inner join t_managed_flink_app managed on managed.app_id = app.id",
            "where app.team_id = #{teamId} and app.deploy_mode = #{managedMode}"
    })
    ManagedFlinkApplicationStatisticsView selectStatistics(
                                                           @Param("teamId") Long teamId,
                                                           @Param("managedMode") Integer managedMode);
}
