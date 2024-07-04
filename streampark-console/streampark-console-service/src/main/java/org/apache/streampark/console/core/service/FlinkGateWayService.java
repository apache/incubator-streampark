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

import org.apache.streampark.console.core.entity.FlinkGateWay;
import org.apache.streampark.console.core.enums.GatewayTypeEnum;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;

/** Connecting to Flink Gateway service */
public interface FlinkGateWayService extends IService<FlinkGateWay> {

    /**
     * Create flink gateway
     *
     * @param flinkGateWay FlinkGateWay
     */
    void create(FlinkGateWay flinkGateWay);

    /**
     * Update flink gateway
     *
     * @param flinkGateWay FlinkGateWay
     */
    void update(FlinkGateWay flinkGateWay);

    /**
     * Check gateway exists by gateway name
     *
     * @param name gateway name
     * @return Whether it exists
     */
    boolean existsByGatewayName(String name);

    /**
     * Get gateway version
     *
     * @param address
     * @return gateway type
     * @throws JsonProcessingException
     */
    GatewayTypeEnum getGatewayVersion(String address) throws JsonProcessingException;
}
