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
import org.apache.streampark.console.core.bean.FlinkCatalogParams;
import org.apache.streampark.console.core.entity.FlinkCatalog;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/** This interface is used to managed catalog */
public interface FlinkCatalogService extends IService<FlinkCatalog> {

    /**
     * Create Catalog
     *
     * @param catalog The {@link FlinkCatalogParams} object containing the search criteria.
     */
    boolean create(FlinkCatalogParams catalog, Long userId);

    /**
     * Remove Catalog
     *
     * @param id The {@link FlinkCatalogParams} object containing the search criteria.
     */
    boolean remove(Long id);

    /**
     * Retrieves a page of applications based on the provided parameters. Params: catalog – The
     * Catalog object to be used for filtering the results. request – The REST request object
     * containing additional parameters or headers. Returns: A page of Catalog objects based on the
     * provided parameters.
     */
    IPage<FlinkCatalogParams> page(FlinkCatalogParams catalog, RestRequest request);

    FlinkCatalog getCatalog(Long catalogId);

    FlinkCatalog getCatalog(String catalogName);
    /**
     * update Catalog
     *
     * @param catalog The {@link FlinkCatalogParams} object containing the search criteria.
     */
    boolean update(FlinkCatalogParams catalog, Long userId);
}
