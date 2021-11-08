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
import com.streamxhub.streamx.console.core.entity.FlinkEnv;

import java.io.IOException;

/**
 * @author benjobs
 */
public interface FlinkEnvService extends IService<FlinkEnv> {

    /**
     * check exists
     *
     * @param version
     * @return
     */
    boolean exists(FlinkEnv version);

    /**
     * create new
     *
     * @param version
     * @throws IOException
     */
    boolean create(FlinkEnv version);

    /**
     * update
     *
     * @param version
     * @throws IOException
     */
    void update(FlinkEnv version) throws IOException;

    /**
     * 根据appId获取flinkVersion
     *
     * @param appId
     * @return
     */
    FlinkEnv getByAppId(Long appId);

    /**
     * 设置某个flink版本为默认版本
     *
     * @param id
     */
    void setDefault(Long id);

    /**
     * 获取默认的flink版本
     *
     * @return
     */
    FlinkEnv getDefault();

    /**
     * 根据id获取 如果获取不到则使用默认的flink版本
     *
     * @return
     */
    FlinkEnv getByIdOrDefault(Long id);

    /**
     * 同步配置文件
     *
     * @param id
     */
    void syncConf(Long id) throws IOException;
}
