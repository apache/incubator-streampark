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

package org.apache.streampark.flink.catalog.factory;

import org.apache.streampark.flink.catalog.MysqlCatalog;
import org.apache.streampark.flink.catalog.utils.Constants;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.factories.CatalogFactory;
import org.apache.flink.table.factories.FactoryUtil;

import java.util.HashSet;
import java.util.Set;

/** Factory for {@link MysqlCatalog}. */
public class MysqlCatalogFactory implements CatalogFactory {

  @Override
  public String factoryIdentifier() {
    return Constants.IDENTIFIER;
  }

  @Override
  public Set<ConfigOption<?>> requiredOptions() {
    final Set<ConfigOption<?>> options = new HashSet<>();
    options.add(MysqlCatalogFactoryOptions.JDBC_URL);
    options.add(MysqlCatalogFactoryOptions.USERNAME);
    options.add(MysqlCatalogFactoryOptions.PASSWORD);
    return options;
  }

  @Override
  public Set<ConfigOption<?>> optionalOptions() {
    return new HashSet<>();
  }

  @Override
  public Catalog createCatalog(Context context) {
    final FactoryUtil.CatalogFactoryHelper helper =
        FactoryUtil.createCatalogFactoryHelper(this, context);
    helper.validate();
    return new MysqlCatalog(
        context.getName(),
        helper.getOptions().get(MysqlCatalogFactoryOptions.JDBC_URL),
        helper.getOptions().get(MysqlCatalogFactoryOptions.USERNAME),
        helper.getOptions().get(MysqlCatalogFactoryOptions.PASSWORD));
  }
}
