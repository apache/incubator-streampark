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

package org.apache.streampark.console.core.service.alert.impl;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.bean.AlertConfigParams;
import org.apache.streampark.console.core.entity.AlertConfig;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.mapper.AlertConfigMapper;
import org.apache.streampark.console.core.service.alert.AlertConfigService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;

import org.apache.commons.collections.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class AlertConfigServiceImpl extends ServiceImpl<AlertConfigMapper, AlertConfig>
    implements
        AlertConfigService {

    @Autowired
    private ApplicationInfoService applicationInfoService;

    @Override
    public IPage<AlertConfigParams> page(Long userId, RestRequest request) {
        // build query conditions
        LambdaQueryWrapper<AlertConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, AlertConfig::getUserId, userId);

        Page<AlertConfig> page = MybatisPager.getPage(request);
        IPage<AlertConfig> resultPage = getBaseMapper().selectPage(page, wrapper);

        Page<AlertConfigParams> result = new Page<>();
        if (CollectionUtils.isNotEmpty(resultPage.getRecords())) {
            result.setRecords(
                resultPage.getRecords().stream().map(AlertConfigParams::of).collect(Collectors.toList()));
        }

        return result;
    }

    @Override
    public boolean exist(AlertConfig alertConfig) {
        AlertConfig confByName = this.baseMapper.selectAlertConfByName(alertConfig);
        return confByName != null;
    }

    @Override
    public boolean removeById(Long id) throws AlertException {
        long count = applicationInfoService.count(
            new LambdaQueryWrapper<Application>().eq(id != null, Application::getAlertId, id));
        if (count > 0) {
            throw new AlertException(
                String.format(
                    "AlertId:%d, this is bound by application. Please clear the configuration first",
                    id));
        }
        return super.removeById(id);
    }
}
