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
import org.apache.streampark.console.core.entity.Resource;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ResourceService extends IService<Resource> {

  /**
   * list resource
   *
   * @param resource resource
   * @param restRequest queryRequest
   * @return IPage
   */
  IPage<Resource> page(Resource resource, RestRequest restRequest);

  /**
   * add resource
   *
   * @param resource resource
   */
  void addResource(Resource resource);

  /**
   * @param teamId team id
   * @param name resource name
   * @return the found resource
   */
  Resource findByResourceName(Long teamId, String name);

  /**
   * update resource
   *
   * @param resource the updated resource
   */
  void updateResource(Resource resource);

  /**
   * delete resource
   *
   * @param resource
   */
  void deleteResource(Resource resource);

  /**
   * Get resource through team id.
   *
   * @param teamId
   * @return team resources
   */
  List<Resource> findByTeamId(Long teamId);
}
