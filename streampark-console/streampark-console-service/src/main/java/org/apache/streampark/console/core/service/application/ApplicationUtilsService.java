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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.AppExistsState;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This interface defines the methods that can be used for various utility operations related to an application.
 */
public interface ApplicationUtilsService extends IService<Application> {
    boolean existsByTeamId(Long teamId);

    boolean existsByUserId(Long userId);

    boolean existsRunningJobByClusterId(Long clusterId);

    boolean existsJobByClusterId(Long clusterId);

    boolean existsJobByFlinkEnvId(Long flinkEnvId);

    Integer countJobsByClusterId(Long clusterId);

    Integer countAffectedJobsByClusterId(Long clusterId, String dbType);

    String getYarnName(Application app);

    AppExistsState checkExists(Application app);

    void persistMetrics(Application application);

    String readConf(Application app) throws IOException;

    String getMain(Application application);

    Map<String, Serializable> dashboard(Long teamId);

    String k8sStartLog(Long id, Integer offset, Integer limit) throws Exception;

    List<String> getRecentK8sNamespace();

    List<String> getRecentK8sClusterId(Integer executionMode);

    List<String> getRecentFlinkBaseImage();

    List<String> getRecentK8sPodTemplate();

    List<String> getRecentK8sJmPodTemplate();

    List<String> getRecentK8sTmPodTemplate();

    List<String> historyUploadJars();
}
