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

import org.apache.streampark.console.core.entity.ExternalLink;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * External Linked Service,External service integrations such as code repositories, metrics
 * monitoring pages, real-time logging
 */
public interface ExternalLinkService extends IService<ExternalLink> {

    /**
     * Create a ExternalLink
     *
     * @param externalLink The ExternalLink to be create.
     */
    void create(ExternalLink externalLink);

    /**
     * remove ExternalLink by link id
     *
     * @param linkId The ID of the object to delete.
     */
    void removeById(Long linkId);

    /**
     * update ExternalLink information
     *
     * @param externalLink The ExternalLink to be update
     */
    void update(ExternalLink externalLink);

    /**
     * Displays the display that is relevant to the query criteriaExternalLink
     *
     * @param appId The ID of the object to render.
     * @return list of ExternalLink
     */
    List<ExternalLink> render(Long appId);
}
