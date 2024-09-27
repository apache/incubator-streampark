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

import org.apache.streampark.console.core.entity.FlinkEffective;
import org.apache.streampark.console.core.enums.EffectiveTypeEnum;
import org.apache.streampark.console.core.mapper.FlinkEffectiveMapper;
import org.apache.streampark.console.core.service.FlinkEffectiveService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkEffectiveServiceImpl extends ServiceImpl<FlinkEffectiveMapper, FlinkEffective>
    implements
        FlinkEffectiveService {

    @Override
    public void remove(Long appId, EffectiveTypeEnum effectiveTypeEnum) {
        LambdaQueryWrapper<FlinkEffective> queryWrapper = new LambdaQueryWrapper<FlinkEffective>()
            .eq(FlinkEffective::getAppId, appId)
            .eq(FlinkEffective::getTargetType, effectiveTypeEnum.getType());
        baseMapper.delete(queryWrapper);
    }

    @Override
    public FlinkEffective get(Long appId, EffectiveTypeEnum effectiveTypeEnum) {
        LambdaQueryWrapper<FlinkEffective> queryWrapper = new LambdaQueryWrapper<FlinkEffective>()
            .eq(FlinkEffective::getAppId, appId)
            .eq(FlinkEffective::getTargetType, effectiveTypeEnum.getType());
        return this.getOne(queryWrapper);
    }

    @Override
    public void saveOrUpdate(Long appId, EffectiveTypeEnum type, Long id) {
        LambdaQueryWrapper<FlinkEffective> queryWrapper = new LambdaQueryWrapper<FlinkEffective>()
            .eq(FlinkEffective::getAppId, appId)
            .eq(FlinkEffective::getTargetType, type.getType());
        long count = count(queryWrapper);
        if (count == 0) {
            FlinkEffective effective = new FlinkEffective();
            effective.setAppId(appId);
            effective.setTargetType(type.getType());
            effective.setTargetId(id);
            effective.setCreateTime(new Date());
            save(effective);
        } else {
            update(
                new LambdaUpdateWrapper<FlinkEffective>()
                    .eq(FlinkEffective::getAppId, appId)
                    .eq(FlinkEffective::getTargetType, type.getType())
                    .set(FlinkEffective::getTargetId, id));
        }
    }

    @Override
    public void removeByAppId(Long appId) {
        LambdaQueryWrapper<FlinkEffective> queryWrapper =
            new LambdaQueryWrapper<FlinkEffective>().eq(FlinkEffective::getAppId, appId);
        this.remove(queryWrapper);
    }
}
