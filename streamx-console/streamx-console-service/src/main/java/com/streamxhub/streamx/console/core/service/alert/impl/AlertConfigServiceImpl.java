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

package com.streamxhub.streamx.console.core.service.alert.impl;

import com.streamxhub.streamx.console.base.domain.Constant;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.exception.AlertException;
import com.streamxhub.streamx.console.base.util.SortUtils;
import com.streamxhub.streamx.console.core.dao.AlertConfigMapper;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.alert.AlertConfig;
import com.streamxhub.streamx.console.core.entity.alert.AlertConfigWithParams;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.console.core.service.alert.AlertConfigService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * @author weijinglun
 * @date 2022.01.14
 */
@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class AlertConfigServiceImpl extends ServiceImpl<AlertConfigMapper, AlertConfig> implements AlertConfigService {

    @Autowired
    private ApplicationService applicationService;

    @Override
    public IPage<AlertConfigWithParams> page(AlertConfigWithParams params, RestRequest request) {
        Page<AlertConfig> page = new Page<>();
        SortUtils.handlePageSort(request, page, "create_time", Constant.ORDER_DESC, false);
        // build query conditions
        QueryWrapper<AlertConfig> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(params.getUserId() != null, AlertConfig::getUserId, params.getUserId());

        IPage<AlertConfig> resultPage = getBaseMapper().selectPage(page, wrapper);

        Page<AlertConfigWithParams> result = new Page<>();
        if (CollectionUtils.isNotEmpty(resultPage.getRecords())) {
            result.setRecords(resultPage.getRecords().stream().map(AlertConfigWithParams::of).collect(Collectors.toList()));
        }

        return result;
    }

    @Override
    public boolean exist(AlertConfig alertConfig) {
        AlertConfig confByName = this.baseMapper.getAlertConfByName(alertConfig);
        return confByName != null;
    }

    @Override
    public boolean deleteById(Long id) throws AlertException {
        int count = applicationService.count(new LambdaQueryWrapper<Application>().eq(id != null, Application::getAlertId, id));
        if (count > 0) {
            throw new AlertException(String.format("AlertId:%d, this is bound by application. Please clear the configuration first", id));
        }
        return removeById(id);
    }
}
