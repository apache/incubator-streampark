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

import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.SubmitResponse;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Application info operation service */
public interface OpApplicationInfoService extends IService<Application> {

  boolean create(Application app) throws IOException;

  Long copy(Application app) throws IOException;

  boolean update(Application app);

  void starting(Application app);

  void persistMetrics(Application application);

  void toEffective(Application application);

  void updateRelease(Application application);

  void forcedStop(Application app);

  void clean(Application app);

  List<String> historyUploadJars();

  Boolean delete(Application app);

  String upload(MultipartFile file) throws Exception;

  boolean mapping(Application app);

  void revoke(Application app) throws ApplicationException;

  void updateToStopped(Application app);

  Map<Long, CompletableFuture<SubmitResponse>> getStartFutureMap();

  Map<Long, CompletableFuture<CancelResponse>> getCancelFutureMap();

  boolean checkBuildAndUpdate(Application app);

  void changeOwnership(Long userId, Long targetUserId);
}
