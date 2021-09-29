/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.console.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.streamx.console.core.entity.Effective;
import com.streamxhub.streamx.console.core.enums.EffectiveType;

/**
 * @author benjobs
 */
public interface EffectiveService extends IService<Effective> {
    /**
     * @param appId
     * @param config
     */
    void delete(Long appId, EffectiveType config);

    Effective get(Long appId, EffectiveType config);

    void saveOrUpdate(Long appId, EffectiveType type, Long id);

    void removeApp(Long appId);
}
