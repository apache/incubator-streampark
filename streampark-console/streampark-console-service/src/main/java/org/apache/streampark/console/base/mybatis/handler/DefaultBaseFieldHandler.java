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

package org.apache.streampark.console.base.mybatis.handler;

import org.apache.streampark.console.base.mybatis.entity.BaseEntity;

import org.apache.ibatis.reflection.MetaObject;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

/** Automatically generate common basic field values when adding and modifying data. */
@Component
public class DefaultBaseFieldHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        baseEntity(metaObject).ifPresent(entity -> {
            entity.setCreateTime(new Date());
            entity.setModifyTime(new Date());
        });

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        baseEntity(metaObject).ifPresent(entity -> entity.setModifyTime(new Date()));
    }

    private Optional<BaseEntity> baseEntity(MetaObject metaObject) {
        return Optional.ofNullable(metaObject)
            .map(MetaObject::getOriginalObject)
            .filter(BaseEntity.class::isInstance)
            .map(BaseEntity.class::cast);
    }

}
