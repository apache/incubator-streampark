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

package org.apache.streampark.console.core.service.application;

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.AppExistsState;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The ApplicationService interface provides methods to manage applications information.
 * It extends the IService interface with the Application entity.
 */
public interface ApplicationService extends IService<Application> {

    IPage<Application> page(Application app, RestRequest request);

    boolean create(Application app) throws IOException;

    Long copy(Application app) throws IOException;

    boolean update(Application app);

    Boolean delete(Application app);

    Application getApp(Application app);

    void updateRelease(Application application);

    List<Application> getByProjectId(Long id);

    void changeOwnership(Long userId, Long targetUserId);

    List<Application> getByTeamId(Long teamId);

    List<Application> getByTeamIdAndExecutionModes(
        Long teamId, Collection<ExecutionMode> executionModes);
}
