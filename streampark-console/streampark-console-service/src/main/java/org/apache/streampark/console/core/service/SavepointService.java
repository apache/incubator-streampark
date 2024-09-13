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
import org.apache.streampark.console.base.exception.InternalException;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.Savepoint;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.annotation.Nullable;

public interface SavepointService extends IService<Savepoint> {

  void expire(Long appId);

  Savepoint getLatest(Long id);

  void trigger(Long appId, @Nullable String savepointPath) throws Exception;

  Boolean delete(Long id, Application application) throws InternalException;

  IPage<Savepoint> page(Savepoint savepoint, RestRequest request);

  void removeApp(Application application);

  String getSavePointPath(Application app) throws Exception;

  String processPath(String path, String jobName, Long jobId);

  void saveSavePoint(Savepoint savepoint);
}
