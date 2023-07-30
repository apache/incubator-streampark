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

import org.apache.streampark.console.core.entity.FlinkEnv;

import com.baomidou.mybatisplus.extension.service.IService;

import java.io.IOException;

public interface FlinkEnvService extends IService<FlinkEnv> {

  /**
   * check exists
   *
   * @param version version
   * @return two places will be checked: <br>
   *     1) name repeated <br>
   *     2) flink-dist <br>
   *     -1) invalid path <br>
   *     0) ok <br>
   */
  Integer check(FlinkEnv version);

  /**
   * create new
   *
   * @param version version
   * @throws IOException IOException
   */
  boolean create(FlinkEnv version) throws Exception;

  /**
   * delete flink env
   *
   * @param id id
   */
  void delete(Long id);

  /**
   * update
   *
   * @param version version
   * @throws IOException IOException
   */
  void update(FlinkEnv version) throws IOException;

  /**
   * get flink version by appid
   *
   * @param appId appId
   * @return FlinkEnv
   */
  FlinkEnv getByAppId(Long appId);

  /**
   * set a flink version as the default
   *
   * @param id id
   */
  void setDefault(Long id);

  /**
   * get default version
   *
   * @return FlinkEnv
   */
  FlinkEnv getDefault();

  /**
   * get flink version, if null, get default version
   *
   * @return FlinkEnv
   */
  FlinkEnv getByIdOrDefault(Long id);

  /**
   * sycn conf file
   *
   * @param id id
   */
  void syncConf(Long id) throws IOException;

  void validity(Long id);
}
