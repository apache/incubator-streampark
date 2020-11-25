/**
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
package com.streamxhub.console.core.service;

import com.streamxhub.console.base.domain.RestRequest;
import com.streamxhub.console.core.entity.Application;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.streamxhub.console.core.entity.Project;
import com.streamxhub.console.core.enums.AppExistsState;

import java.io.IOException;
import java.util.List;

/**
 * @author benjobs
 */
public interface ApplicationService extends IService<Application> {

    IPage<Application> page(Application app, RestRequest request);

    boolean create(Application app) throws IOException;

    boolean update(Application app);

    boolean start(Application app) throws Exception;

    String getYarnName(Application app);

    AppExistsState checkExists(Application app);

    void deploy(Application app) throws Exception;

    void updateDeploy(Application app);

    void updateState(Application app);

    void cancel(Application app);

    void updateTracking(Application application);

    void clean(Application app);

    String readConf(Application app) throws IOException;

    Application getApp(Application app);

    String getMain(Application application);

    boolean mapping(Application app);

}
