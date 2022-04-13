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

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.console.core.dao.EffectiveMapper;
import com.streamxhub.streamx.console.core.entity.Effective;
import com.streamxhub.streamx.console.core.enums.EffectiveType;
import com.streamxhub.streamx.console.core.service.EffectiveService;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class EffectiveServiceImpl extends ServiceImpl<EffectiveMapper, Effective>
    implements EffectiveService {

    @Override
    public void delete(Long appId, EffectiveType effectiveType) {
        Wrapper<Effective> queryWrapper = new QueryWrapper<Effective>()
            .lambda()
            .eq(Effective::getAppId, appId)
            .eq(Effective::getTargetType, effectiveType.getType());
        baseMapper.delete(queryWrapper);
    }

    @Override
    public Effective get(Long appId, EffectiveType effectiveType) {
        return baseMapper.get(appId, effectiveType.getType());
    }

    @Override
    public void saveOrUpdate(Long appId, EffectiveType type, Long id) {
        Wrapper<Effective> queryWrapper = new QueryWrapper<Effective>()
            .lambda()
            .eq(Effective::getAppId, appId)
            .eq(Effective::getTargetType, type.getType());
        int count = count(queryWrapper);
        if (count == 0) {
            Effective effective = new Effective();
            effective.setAppId(appId);
            effective.setTargetType(type.getType());
            effective.setTargetId(id);
            effective.setCreateTime(new Date());
            save(effective);
        } else {
            update(new UpdateWrapper<Effective>()
                .lambda()
                .eq(Effective::getAppId, appId)
                .eq(Effective::getTargetType, type.getType())
                .set(Effective::getTargetId, id)
            );
        }
    }

    @Override
    public void removeApp(Long appId) {
        baseMapper.removeApp(appId);
    }
}
