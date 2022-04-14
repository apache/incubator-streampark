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

package com.streamxhub.streamx.console.core.service;

import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.ApplicationConfig;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author benjobs
 */
public interface ApplicationConfigService extends IService<ApplicationConfig> {

    void create(Application application, Boolean latest);

    void update(Application application, Boolean latest);

    void setLatestOrEffective(Boolean latest, Long configId, Long appId);

    void toEffective(Long appId, Long configId);

    ApplicationConfig getLatest(Long appId);

    /**
     * 获取application的生效的活跃的Config
     *
     * @param application
     * @return
     */
    ApplicationConfig getEffective(Long appId);

    ApplicationConfig get(Long id);

    IPage<ApplicationConfig> page(ApplicationConfig config, RestRequest request);

    List<ApplicationConfig> history(Application application);

    String readTemplate();

    void removeApp(Long appId);
}
