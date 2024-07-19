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

import org.apache.streampark.common.util.HttpClientUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.entity.FlinkGateWay;
import org.apache.streampark.console.core.enums.GatewayTypeEnum;
import org.apache.streampark.console.core.mapper.FlinkGateWayMapper;
import org.apache.streampark.console.core.service.FlinkGateWayService;
import org.apache.streampark.gateway.flink.client.dto.GetApiVersionResponseBody;

import org.apache.hc.client5.http.config.RequestConfig;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_GATEWAY_NAME_EXIST;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class FlinkGateWayServiceImpl extends ServiceImpl<FlinkGateWayMapper, FlinkGateWay>
    implements
        FlinkGateWayService {

    private void preHandleGatewayInfo(FlinkGateWay flinkGateWay) {
        // validate gateway name
        ApiAlertException.throwIfTrue(
            existsByGatewayName(flinkGateWay.getGatewayName()), FLINK_GATEWAY_NAME_EXIST);

        // validate gateway address and set gateway type
        flinkGateWay.setGatewayType(getGatewayVersion(flinkGateWay.getAddress()));
    }

    @Override
    public void create(FlinkGateWay flinkGateWay) {
        preHandleGatewayInfo(flinkGateWay);
        this.save(flinkGateWay);
    }

    @Override
    public void update(FlinkGateWay flinkGateWay) {
        preHandleGatewayInfo(flinkGateWay);
        this.saveOrUpdate(flinkGateWay);
    }

    @Override
    public boolean existsByGatewayName(String name) {
        return getBaseMapper()
            .exists(new LambdaQueryWrapper<FlinkGateWay>().eq(FlinkGateWay::getGatewayName, name));
    }

    @Override
    public GatewayTypeEnum getGatewayVersion(String address) {
        String restUrl = address + "/api_versions";
        try {
            String result = HttpClientUtils.httpGetRequest(
                restUrl,
                RequestConfig.custom().setConnectTimeout(2000, TimeUnit.MILLISECONDS).build());
            if (result != null) {
                String versionStr = JacksonUtils.read(result, GetApiVersionResponseBody.class).getVersions().get(0);
                return "V1".equals(versionStr) ? GatewayTypeEnum.FLINK_V1 : GatewayTypeEnum.FLINK_V2;
            }
        } catch (Exception e) {
            log.error("get gateway version failed", e);
        }
        throw new ApiAlertException("get gateway version failed");
    }
}
