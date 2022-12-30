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
import org.apache.streampark.console.core.entity.ApplicationBackUp;
import org.apache.streampark.console.core.entity.FlinkSql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ApplicationBackUpService extends IService<ApplicationBackUp> {

  Boolean delete(Long id) throws InternalException;

  void backup(Application application, FlinkSql flinkSql);

  IPage<ApplicationBackUp> page(ApplicationBackUp backUp, RestRequest request);

  void rollback(ApplicationBackUp backUp);

  void revoke(Application application);

  void removeApp(Application application);

  void rollbackFlinkSql(Application application, FlinkSql sql);

  boolean isFlinkSqlBacked(Long appId, Long sqlId);
}
