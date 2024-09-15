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
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkEnv;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;
import java.util.Properties;

public interface FlinkEnvService extends IService<FlinkEnv> {

  /**
   * check exists
   *
   * @param version
   * @return
   */
  Integer check(FlinkEnv version);

  /**
   * create new
   *
   * @param version
   * @throws IOException
   */
  boolean create(FlinkEnv version) throws Exception;

  /**
   * delete flink env
   *
   * @param id
   */
  void delete(Long id);

  /**
   * update
   *
   * @param version
   * @throws IOException
   */
  void update(FlinkEnv version) throws IOException;

  /**
   * get flink version by appid
   *
   * @param appId
   * @return
   */
  FlinkEnv getByAppId(Long appId);

  /**
   * set a flink version as the default
   *
   * @param id
   */
  void setDefault(Long id);

  /**
   * get default version
   *
   * @return
   */
  FlinkEnv getDefault();

  /**
   * get flink version, if null, get default version
   *
   * @return
   */
  FlinkEnv getByIdOrDefault(Long id);

  /**
   * sycn conf file
   *
   * @param id
   */
  void syncConf(Long id) throws IOException;

  void validity(Long id);

  IPage<FlinkEnv> findPage(FlinkEnv flinkEnv, RestRequest restRequest);

  Properties getFlinkConfig(FlinkEnv flinkEnv, Application application);
}
