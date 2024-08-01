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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.entity.Catalog;
import org.apache.streampark.console.core.mapper.CatalogMapper;
import org.apache.streampark.console.core.service.CatalogService;
import org.apache.streampark.console.core.util.ServiceHelper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** catalog manage */
@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class CatalogServiceImpl extends ServiceImpl<CatalogMapper, Catalog>
    implements
        CatalogService {

    @Override
    public boolean create(Catalog catalog, Long userId) {
        AlertException.throwIfNull(
            catalog.getTeamId(), "The teamId can't be null. Create catalog failed.");
        AlertException.throwIfFalse(
            validateCatalogName(catalog.getCatalogName()),
            "Catalog Name only lowercase letters, numbers, and -,.. Symbol composition, cannot end with a symbol.");
        AlertException.throwIfFalse(
            JacksonUtils.isValidJson(catalog.getConfiguration()), "Catalog configure must json.");
        AlertException.throwIfTrue(
            existsByCatalogName(catalog.getCatalogName()), "Catalog name  already exists.");
        Date date = new Date();
        catalog.setCreateTime(date);
        catalog.setUpdateTime(date);
        return this.save(catalog);
    }

    @Override
    public boolean remove(Long id) {
        Catalog catalog = getById(id);
        ApiAlertException.throwIfNull(catalog, "Catalog not exist, please check.");
        return this.removeById(id);
    }

    @Override
    public IPage<Catalog> page(Catalog catalog, RestRequest request) {
        AlertException.throwIfNull(
            catalog.getTeamId(), "The teamId can't be null. List catalog failed.");

        Page<Catalog> page = MybatisPager.getPage(request);
        this.baseMapper.selectPage(page, catalog);
        return page;
    }

    @Override
    public boolean update(Catalog catalogParam, long userId) {
        AlertException.throwIfNull(
            catalogParam.getTeamId(), "The teamId can't be null. List catalog failed.");
        Catalog catalog = getById(catalogParam.getId());
        AlertException.throwIfFalse(
            catalogParam.getCatalogName().equalsIgnoreCase(catalog.getCatalogName()),
            "The catalog name cannot be modified.");
        log.debug(
            "Catalog {} has modify from {} to {}",
            catalog.getCatalogName(),
            catalog.getConfiguration(),
            catalogParam.getConfiguration());
        catalog.setUserId(ServiceHelper.getUserId());
        catalog.setConfiguration(catalogParam.getConfiguration());
        catalog.setUserId(catalogParam.getUserId());
        catalog.setUpdateTime(new Date());
        return this.updateById(catalog);
    }

    public Boolean existsByCatalogName(String catalogName) {
        return this.baseMapper.existsByCatalogName(catalogName);
    }

    /** validate catalog name */
    private boolean validateCatalogName(String catalogName) {
        String regex = "^[a-z0-9]+([\\-\\.][a-z0-9]+)*$";
        return catalogName.matches(regex);
    }
}
