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
import org.apache.streampark.console.core.bean.FlinkCatalogParams;
import org.apache.streampark.console.core.entity.FlinkCatalog;
import org.apache.streampark.console.core.mapper.CatalogMapper;
import org.apache.streampark.console.core.service.CatalogService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** catalog manage */
@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class CatalogServiceImpl extends ServiceImpl<CatalogMapper, FlinkCatalog>
    implements
        CatalogService {

    @Override
    public boolean create(FlinkCatalogParams catalog, Long userId) {
        AlertException.throwIfNull(
            catalog.getTeamId(), "The teamId can't be null. Create catalog failed.");
        AlertException.throwIfFalse(
            validateCatalogName(catalog.getCatalogName()),
            "Catalog Name only lowercase letters, numbers, and -,.. Symbol composition, cannot end with a symbol.");
        AlertException.throwIfTrue(
            existsByCatalogName(catalog.getCatalogName()), "Catalog name  already exists.");
        FlinkCatalog flinkCatalog = FlinkCatalog.of(catalog);
        Date date = new Date();
        flinkCatalog.setCreateTime(date);
        flinkCatalog.setUpdateTime(date);
        return this.save(flinkCatalog);
    }

    @Override
    public boolean remove(Long id) {
        FlinkCatalog catalog = getById(id);
        ApiAlertException.throwIfNull(catalog, "Catalog not exist, please check.");
        return this.removeById(id);
    }

    @Override
    public IPage<FlinkCatalogParams> page(FlinkCatalogParams catalog, RestRequest request) {
        AlertException.throwIfNull(
            catalog.getTeamId(), "The teamId can't be null. List catalog failed.");

        Page<FlinkCatalog> page = MybatisPager.getPage(request);
        this.baseMapper.selectPage(page, FlinkCatalog.of(catalog));
        Page<FlinkCatalogParams> paramsPage = new Page<>();
        BeanUtils.copyProperties(page, paramsPage, "records");
        List<FlinkCatalogParams> paramList = new ArrayList<>();
        page.getRecords()
            .forEach(
                record -> {
                    paramList.add(FlinkCatalogParams.of(record));
                });
        paramsPage.setRecords(paramList);
        return paramsPage;
    }

    @Override
    public boolean update(FlinkCatalogParams catalogParam, long userId) {
        AlertException.throwIfNull(
            catalogParam.getTeamId(), "The teamId can't be null. List catalog failed.");
        FlinkCatalog catalog = getById(catalogParam.getId());
        FlinkCatalog flinkCatalog = FlinkCatalog.of(catalogParam);
        AlertException.throwIfFalse(
            catalogParam.getCatalogName().equalsIgnoreCase(catalog.getCatalogName()),
            "The catalog name cannot be modified.");
        log.debug(
            "Catalog {} has modify from {} to {}",
            catalog.getCatalogName(),
            catalog.getConfiguration(),
            flinkCatalog.getConfiguration());
        catalog.setConfiguration(flinkCatalog.getConfiguration());
        catalog.setUpdateTime(new Date());
        catalog.setUserId(userId);
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
