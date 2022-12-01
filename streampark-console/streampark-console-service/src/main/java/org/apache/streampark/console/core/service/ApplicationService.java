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

package org.apache.streampark.console.core.service;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.AppExistsState;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface ApplicationService extends IService<Application> {

    IPage<Application> page(Application app, RestRequest request);

    boolean existsByTeamId(Long teamId);

    boolean create(Application app) throws IOException;

    Long copy(Application app) throws IOException;

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

    Map<String, Serializable> dashboard(Long teamId);

    void tailMvnDownloading(Long id);

    String upload(MultipartFile file) throws ApplicationException;

    /**
     * set the latest to Effective, it will really become the current effective
     */
    void toEffective(Application application);

    void revoke(Application app) throws ApplicationException;

    Boolean delete(Application app);

    boolean checkEnv(Application app) throws ApplicationException;

    boolean checkAlter(Application application);

    void updateLaunch(Application application);

    List<Application> getByProjectId(Long id);

    List<Application> getByTeamId(Long teamId);

    boolean checkBuildAndUpdate(Application app);

    void forcedStop(Application app);

    boolean existsRunningJobByClusterId(Long clusterId);

    boolean existsJobByClusterId(Long id);

    List<String> getRecentK8sNamespace();

    List<String> getRecentK8sClusterId(Integer executionMode);

    List<String> getRecentFlinkBaseImage();

    List<String> getRecentK8sPodTemplate();

    List<String> getRecentK8sJmPodTemplate();

    List<String> getRecentK8sTmPodTemplate();

    List<String> historyUploadJars();
}
