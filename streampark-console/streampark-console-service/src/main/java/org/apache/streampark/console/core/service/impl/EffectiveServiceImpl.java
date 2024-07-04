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

import org.apache.streampark.console.core.entity.Effective;
import org.apache.streampark.console.core.enums.EffectiveTypeEnum;
import org.apache.streampark.console.core.mapper.EffectiveMapper;
import org.apache.streampark.console.core.service.EffectiveService;

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
public class EffectiveServiceImpl extends ServiceImpl<EffectiveMapper, Effective>
        implements
            EffectiveService {

    @Override
    public void remove(Long appId, EffectiveTypeEnum effectiveTypeEnum) {
        LambdaQueryWrapper<Effective> queryWrapper =
                new LambdaQueryWrapper<Effective>()
                        .eq(Effective::getAppId, appId)
                        .eq(Effective::getTargetType, effectiveTypeEnum.getType());
        baseMapper.delete(queryWrapper);
    }

    @Override
    public Effective get(Long appId, EffectiveTypeEnum effectiveTypeEnum) {
        LambdaQueryWrapper<Effective> queryWrapper =
                new LambdaQueryWrapper<Effective>()
                        .eq(Effective::getAppId, appId)
                        .eq(Effective::getTargetType, effectiveTypeEnum.getType());
        return this.getOne(queryWrapper);
    }

    @Override
    public void saveOrUpdate(Long appId, EffectiveTypeEnum type, Long id) {
        LambdaQueryWrapper<Effective> queryWrapper =
                new LambdaQueryWrapper<Effective>()
                        .eq(Effective::getAppId, appId)
                        .eq(Effective::getTargetType, type.getType());
        long count = count(queryWrapper);
        if (count == 0) {
            Effective effective = new Effective();
            effective.setAppId(appId);
            effective.setTargetType(type.getType());
            effective.setTargetId(id);
            effective.setCreateTime(new Date());
            save(effective);
        } else {
            update(
                    new LambdaUpdateWrapper<Effective>()
                            .eq(Effective::getAppId, appId)
                            .eq(Effective::getTargetType, type.getType())
                            .set(Effective::getTargetId, id));
        }
    }

    @Override
    public void removeByAppId(Long appId) {
        LambdaQueryWrapper<Effective> queryWrapper =
                new LambdaQueryWrapper<Effective>().eq(Effective::getAppId, appId);
        this.remove(queryWrapper);
    }
}
