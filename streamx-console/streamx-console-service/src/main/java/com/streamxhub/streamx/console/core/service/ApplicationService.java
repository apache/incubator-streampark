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
import com.streamxhub.streamx.console.base.exception.ApplicationException;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.enums.AppExistsState;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author benjobs
 */
public interface ApplicationService extends IService<Application> {

    IPage<Application> page(Application app, RestRequest request);

    boolean create(Application app) throws IOException;

    boolean update(Application app);

    void starting(Application app);

    void start(Application app, boolean auto) throws Exception;

    void restart(Application application) throws Exception;

    String getYarnName(Application app);

    AppExistsState checkExists(Application app);

    String checkSavepointPath(Application app) throws Exception;

    void cancel(Application app) throws Exception;

    void updateTracking(Application application);

    void clean(Application app);

    String readConf(Application app) throws IOException;

    Application getApp(Application app);

    String getMain(Application application);

    boolean mapping(Application app);

    Map<String, Serializable> dashboard();

    void tailMvnDownloading(Long id);

    String upload(MultipartFile file) throws ApplicationException;

    /**
     * 将 latest的设置为Effective的,(此时才真正变成当前生效的)
     */
    void toEffective(Application application);

    void revoke(Application app) throws ApplicationException;

    Boolean delete(Application app);

    boolean checkEnv(Application app) throws ApplicationException;

    void updateLaunch(Application application);

    List<Application> getByProjectId(Long id);

    boolean checkBuildAndUpdate(Application app);

    void forcedStop(Application app);

    Long getCountByTeam(Long teamId);
}
