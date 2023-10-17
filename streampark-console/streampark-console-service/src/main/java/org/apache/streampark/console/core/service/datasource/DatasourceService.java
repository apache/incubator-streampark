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

package org.apache.streampark.console.core.service.datasource;

import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.core.entity.Datasource;
import org.apache.streampark.console.core.service.datasource.config.DatasourceColumnInfo;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DatasourceService extends IService<Datasource> {

  RestResponse create(Datasource datasource);

  boolean update(Datasource datasourceParam);

  boolean delete(Long id);

  boolean exist(Datasource datasource);

  Boolean testConnection(Datasource datasource);

  List<String> getDatabases(Long id);

  List<String> getTables(Long id);

  List<String> getAllSupportType();

  String getFlinkDdl(Long id, String tableName);

  List<DatasourceColumnInfo> getColumns(Long id, String tableName);
}
