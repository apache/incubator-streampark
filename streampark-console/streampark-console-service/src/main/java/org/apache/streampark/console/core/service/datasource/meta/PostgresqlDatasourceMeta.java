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

package org.apache.streampark.console.core.service.datasource.meta;

import org.apache.streampark.console.core.entity.Datasource;

import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;

public class PostgresqlDatasourceMeta extends AbstractJdbcDatasourceMeta {
  public PostgresqlDatasourceMeta(Datasource datasource) throws SQLException {
    super(datasource);
  }

  @Override
  public String getJdbcUrl(Datasource datasource) {
    String url =
        String.format(
            "jdbc:postgresql://%s:%s/%s",
            datasource.getHost(), datasource.getPort(), datasource.getDatabase());
    if (StringUtils.isNotBlank(datasource.getParam())) {
      url = url.concat("?").concat(datasource.getParam());
    }
    return url;
  }

  @Override
  public String getJdbcDriverClass() {
    return "org.postgresql.Driver";
  }
}
