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

package org.apache.streampark.console.core.service.datasource.impl;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.domain.RestResponse;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.entity.Datasource;
import org.apache.streampark.console.core.mapper.DatasourceMapper;
import org.apache.streampark.console.core.service.datasource.DatasourceService;
import org.apache.streampark.console.core.service.datasource.config.DatasourceColumnInfo;
import org.apache.streampark.console.core.service.datasource.config.DatasourceMetaHelper;
import org.apache.streampark.console.core.service.datasource.config.DatasourceType;
import org.apache.streampark.console.core.service.datasource.meta.DatasourceMeta;

import org.apache.commons.collections.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class DatasourceServiceImpl extends ServiceImpl<DatasourceMapper, Datasource>
    implements DatasourceService {

  private static final Logger logger = LoggerFactory.getLogger(DatasourceServiceImpl.class);

  private static final Cache<String, DatasourceMeta> datasourceMetaCache =
      Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

  @Override
  public RestResponse create(Datasource datasource) {
    LambdaQueryWrapper<Datasource> queryWrapper =
        new LambdaQueryWrapper<Datasource>()
            .eq(Datasource::getDatasourceName, datasource.getDatasourceName());
    long count = count(queryWrapper);
    RestResponse response = RestResponse.success();

    ApiAlertException.throwIfTrue(
        count > 0, "Datasource name already exists, add Datasource failed");

    datasource.setCreateTime(new Date());
    datasource.setModifyTime(new Date());
    datasource.setState(
        testConnection(datasource)
            ? DatasourceType.SUCCESS.getState()
            : DatasourceType.FAILED.getState());
    boolean status = save(datasource);

    if (status) {
      return response.message("Add Datasource successfully").data(true);
    } else {
      return response.message("Add Datasource failed").data(false);
    }
  }

  @Override
  public boolean exist(Datasource datasource) {
    Datasource datasourceByName = this.baseMapper.getDatasourceByName(datasource);
    return datasourceByName != null;
  }

  @Override
  public boolean update(Datasource datasourceParam) {
    Datasource datasource = getById(datasourceParam.getId());
    Utils.notNull(datasource);

    datasource.setDatasourceName(datasourceParam.getDatasourceName());
    datasource.setDatasourceType(datasourceParam.getDatasourceType());
    datasource.setHost(datasourceParam.getHost());
    datasource.setPort(datasourceParam.getPort());
    datasource.setParam(datasourceParam.getParam());
    datasource.setUsername(datasourceParam.getUsername());
    datasource.setPassword(datasourceParam.getPassword());
    datasource.setState(datasourceParam.getState());
    datasource.setModifyTime(new Date());

    baseMapper.updateById(datasource);
    return true;
  }

  @Override
  public boolean delete(Long id) {
    Datasource datasource = getById(id);
    Utils.notNull(datasource);
    boolean result = removeById(id);

    if (result) {
      datasourceMetaCache.invalidate(datasource.getDatasourceName());
    }
    return result;
  }

  @Override
  public Boolean testConnection(Datasource datasource) {
    return getDatasourceMeta(datasource).testConnection();
  }

  @Override
  public List<String> getTables(Long id) {
    Datasource datasource = getById(id);
    return getDatasourceMeta(datasource).getTables(datasource.getDatabase());
  }

  @Override
  public List<String> getAllSupportType() {
    return DatasourceMetaHelper.getAllSupportType();
  }

  @Override
  public List<DatasourceColumnInfo> getColumns(Long id, String tableName) {
    Datasource datasource = getById(id);
    List<String> tables = getTables(id);
    if (CollectionUtils.isEmpty(tables) || !tables.contains(tableName)) {
      logger.error("Table does not exist: {}", tableName);
      return Collections.emptyList();
    }
    return getDatasourceMeta(datasource).getColumns(datasource.getDatabase(), tableName);
  }

  @Override
  public List<String> getDatabases(Long id) {
    Datasource datasource = getById(id);
    return getDatasourceMeta(datasource).getDatabases();
  }

  @Override
  public String getFlinkDdl(Long id, String tableName) {
    Datasource datasource = getById(id);
    List<String> tables = getTables(id);
    if (CollectionUtils.isEmpty(tables) || !tables.contains(tableName)) {
      logger.error("Table does not exist: {}", tableName);
      return "";
    }
    return getDatasourceMeta(datasource).getFlinkDdl(datasource, tableName);
  }

  private DatasourceMeta getDatasourceMeta(Datasource datasource) {
    if (datasourceMetaCache.getIfPresent(datasource.getDatasourceName()) == null) {
      DatasourceMeta datasourceMeta = DatasourceMetaHelper.getDatasourceMeta(datasource);
      datasourceMetaCache.put(datasource.getDatasourceName(), datasourceMeta);
    }
    return datasourceMetaCache.getIfPresent(datasource.getDatasourceName());
  }
}
