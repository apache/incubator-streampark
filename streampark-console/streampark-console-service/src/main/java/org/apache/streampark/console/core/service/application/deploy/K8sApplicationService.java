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

package org.apache.streampark.console.core.service.application.deploy;

import org.apache.streampark.console.core.service.application.ApplicationService;

import java.util.List;

public interface K8sApplicationService extends ApplicationService {

  List<String> getRecentK8sNamespace();

  List<String> getRecentK8sClusterId(Integer executionMode);

  List<String> getRecentFlinkBaseImage();

  List<String> getRecentK8sPodTemplate();

  List<String> getRecentK8sJmPodTemplate();

  List<String> getRecentK8sTmPodTemplate();

  String k8sStartLog(Long id, Integer offset, Integer limit) throws Exception;
}
