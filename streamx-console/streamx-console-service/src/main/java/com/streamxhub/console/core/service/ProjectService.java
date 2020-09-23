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
import com.streamxhub.console.base.domain.RestResponse;
import com.streamxhub.console.core.entity.Project;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @author benjobs
 */
public interface ProjectService extends IService<Project> {

    RestResponse create(Project project);

    boolean delete(String id);

    IPage<Project> page(Project project, RestRequest restRequest);

    RestResponse build(Long id) throws Exception;

    void tailBuildLog(Long id);

    List<String> modules(Long id);

    List<String> jars(Project project);

    List<Map<String,Object>> listConf(Project project);

    String getAppConfPath(Long id,String module);
}
