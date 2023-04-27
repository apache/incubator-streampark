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
import org.apache.streampark.console.core.entity.Dependency;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DependencyService extends IService<Dependency> {

  /**
   * list dependency
   *
   * @param dependency dependency
   * @param restRequest queryRequest
   * @return IPage
   */
  IPage<Dependency> page(Dependency dependency, RestRequest restRequest);

  /**
   * add dependency
   *
   * @param dependency dependency
   */
  void addDependency(Dependency dependency);

  /**
   * @param teamId team id
   * @param name dependency name
   * @return the found dependency
   */
  Dependency findByDependencyName(Long teamId, String name);

  /**
   * update dependency
   *
   * @param dependency the updated dependency
   */
  void updateDependency(Dependency dependency);

  /**
   * delete dependency
   *
   * @param dependency
   */
  void deleteDependency(Dependency dependency);

  /**
   * Get dependency through team id.
   *
   * @param teamId
   * @return team dependencies
   */
  List<Dependency> findByTeamId(Long teamId);
}
