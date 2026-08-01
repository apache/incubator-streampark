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

package org.apache.streampark.console.core.service.alert;

import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.core.bean.AlertConfigWithParams;
import org.apache.streampark.console.core.entity.AlertConfig;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlertConfigService extends IService<AlertConfig> {
  IPage<AlertConfigWithParams> page(AlertConfigWithParams params, RestRequest request);

  boolean exist(AlertConfig alertConfig);

  boolean deleteById(Long id) throws AlertException;

  /**
   * Create a new alert config. Ownership (userId) is always forced to the currently logged-in user,
   * regardless of what the client submitted in the request body.
   *
   * @param alertConfig the alert config to create.
   * @return {@code true} if saved successfully.
   */
  boolean createAlertConfig(AlertConfig alertConfig);

  /**
   * Update an existing alert config, after verifying the current user is the owner (creator) of the
   * target record, or the platform administrator.
   *
   * @param alertConfig the alert config to update.
   * @return {@code true} if updated successfully.
   */
  boolean updateAlertConfig(AlertConfig alertConfig);

  /**
   * Get a single alert config, after verifying the current user is the owner (creator) of the
   * target record, or the platform administrator.
   *
   * @param id alert config id.
   * @return the alert config, or {@code null} if it doesn't exist.
   */
  AlertConfig getAlertConfig(Long id);

  /**
   * List every alert config owned by the currently logged-in user, or every alert config in the
   * system if the current user is the platform administrator.
   *
   * @return the list of alert configs.
   */
  List<AlertConfig> listByCurrentUser();
}
