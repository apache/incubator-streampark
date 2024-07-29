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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.entity.SparkApplicationLog;
import org.apache.streampark.console.core.mapper.SparkApplicationLogMapper;
import org.apache.streampark.console.core.service.SparkApplicationLogService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SparkApplicationLogServiceImpl extends ServiceImpl<SparkApplicationLogMapper, SparkApplicationLog>
    implements
        SparkApplicationLogService {

    @Override
    public IPage<SparkApplicationLog> getPage(SparkApplicationLog sparkApplicationLog, RestRequest request) {
        request.setSortField("option_time");
        Page<SparkApplicationLog> page = MybatisPager.getPage(request);
        LambdaQueryWrapper<SparkApplicationLog> queryWrapper = new LambdaQueryWrapper<SparkApplicationLog>()
            .eq(SparkApplicationLog::getAppId, sparkApplicationLog.getAppId());
        return this.page(page, queryWrapper);
    }

    @Override
    public void removeByAppId(Long appId) {
        LambdaQueryWrapper<SparkApplicationLog> queryWrapper = new LambdaQueryWrapper<SparkApplicationLog>()
            .eq(SparkApplicationLog::getAppId, appId);
        this.remove(queryWrapper);
    }
}
