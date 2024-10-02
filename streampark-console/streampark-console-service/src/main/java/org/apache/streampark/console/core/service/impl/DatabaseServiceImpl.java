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

import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.core.bean.DatabaseParam;
import org.apache.streampark.console.core.entity.Database;
import org.apache.streampark.console.core.entity.FlinkCatalog;
import org.apache.streampark.console.core.mapper.DatabaseMapper;
import org.apache.streampark.console.core.service.DatabaseService;
import org.apache.streampark.console.core.service.FlinkCatalogBase;
import org.apache.streampark.console.core.service.FlinkCatalogService;

import org.apache.flink.table.catalog.CatalogDatabase;
import org.apache.flink.table.catalog.CatalogDatabaseImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.streampark.console.core.util.CatalogServiceUtils.getOptions;

@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class DatabaseServiceImpl extends ServiceImpl<DatabaseMapper, Database>
    implements
        DatabaseService {

    @Autowired
    private FlinkCatalogService catalogService;
    @Autowired
    private FlinkCatalogBase flinkCatalogBase;

    @Override
    public boolean databaseExists(DatabaseParam databaseParam) {
        AlertException.throwIfNull(databaseParam.getName(), "Database name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(databaseParam.getCatalogId());
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit in database.");
        return flinkCatalogBase.databaseExists(
            flinkCatalog.getCatalogName(),
            getOptions(flinkCatalog.getConfiguration()),
            databaseParam.getName());
    }

    @Override
    public boolean createDatabase(DatabaseParam databaseParam) {
        AlertException.throwIfNull(databaseParam.getName(), "Database name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(databaseParam.getCatalogId());
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit in database.");
        Map<String, String> dbMap = new ConcurrentHashMap<>();
        dbMap.put("cascade", String.valueOf(databaseParam.isCascade()));
        CatalogDatabase catalogDatabase =
            new CatalogDatabaseImpl(dbMap, databaseParam.getDescription());
        return flinkCatalogBase.createDatabase(
            flinkCatalog.getCatalogName(),
            getOptions(flinkCatalog.getConfiguration()),
            databaseParam.getName(),
            catalogDatabase,
            databaseParam.isIgnoreIfExits());
    }

    @Override
    public List<DatabaseParam> listDatabases(Long catalogId) {
        FlinkCatalog flinkCatalog = catalogService.getCatalog(catalogId);
        AlertException.throwIfNull(
            flinkCatalog, "The catalog can't be null. get catalog from database failed.");
        List<String> databases =
            flinkCatalogBase.listDatabases(
                flinkCatalog.getCatalogName(), getOptions(flinkCatalog.getConfiguration()));
        if (databases == null || databases.isEmpty()) {
            return Collections.emptyList();
        }
        List<DatabaseParam> databaseList = new ArrayList<>();
        databases.forEach(
            dbName -> {
                DatabaseParam dbParam = new DatabaseParam();
                dbParam.setCatalogId(catalogId);
                dbParam.setCatalogName(flinkCatalog.getCatalogName());
                dbParam.setName(dbName);
                databaseList.add(dbParam);
            });
        return databaseList;
    }

    @Override
    public boolean dropDatabase(DatabaseParam databaseParam) {
        AlertException.throwIfNull(databaseParam.getName(), "Database name can not be null.");
        FlinkCatalog flinkCatalog = catalogService.getCatalog(databaseParam.getCatalogId());
        AlertException.throwIfNull(flinkCatalog, "Catalog is not exit in database.");
        flinkCatalogBase.dropDatabase(
            flinkCatalog.getCatalogName(),
            getOptions(flinkCatalog.getConfiguration()),
            databaseParam.getName(),
            databaseParam.isCascade(),
            databaseParam.isIgnoreIfExits());
        return true;
    }
}
