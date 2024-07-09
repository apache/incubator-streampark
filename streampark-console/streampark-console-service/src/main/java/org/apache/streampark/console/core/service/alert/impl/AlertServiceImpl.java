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

package org.apache.streampark.console.core.service.alert.impl;

import org.apache.streampark.console.base.exception.AlertException;
import org.apache.streampark.console.base.util.SpringContextUtils;
import org.apache.streampark.console.core.bean.AlertConfigParams;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.entity.AlertConfig;
import org.apache.streampark.console.core.enums.AlertTypeEnum;
import org.apache.streampark.console.core.service.alert.AlertConfigService;
import org.apache.streampark.console.core.service.alert.AlertService;

import org.apache.flink.api.java.tuple.Tuple2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertConfigService alertConfigService;

    @Override
    public boolean alert(Long alertConfigId, AlertTemplate alertTemplate) {

        if (alertConfigId == null) {
            log.warn("alertConfigId is null");
            return false;
        }
        AlertConfig alertConfig = alertConfigService.getById(alertConfigId);
        try {
            AlertConfigParams params = AlertConfigParams.of(alertConfig);
            List<AlertTypeEnum> alertTypeEnums = AlertTypeEnum.decode(params.getAlertType());
            if (CollectionUtils.isEmpty(alertTypeEnums)) {
                return true;
            }
            // No use thread pool, ensure that the alarm can be sent successfully
            Tuple2<Boolean, AlertException> reduce = triggerAlert(alertTemplate, alertTypeEnums, params);
            if (reduce.f1 != null) {
                throw reduce.f1;
            }

            return reduce.f0;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    @Nonnull
    private Tuple2<Boolean, AlertException> triggerAlert(
                                                         AlertTemplate alertTemplate,
                                                         List<AlertTypeEnum> alertTypeEnums, AlertConfigParams params) {
        return alertTypeEnums.stream()
            .map(
                alertTypeEnum -> {
                    try {
                        boolean alertRes = SpringContextUtils.getBean(alertTypeEnum.getClazz())
                            .doAlert(params, alertTemplate);
                        return new Tuple2<Boolean, AlertException>(alertRes, null);
                    } catch (AlertException e) {
                        return new Tuple2<>(false, e);
                    }
                })
            .reduce(
                new Tuple2<>(true, null),
                (tp1, tp2) -> {
                    boolean alertResult = tp1.f0 & tp2.f0;
                    if (tp1.f1 == null && tp2.f1 == null) {
                        return new Tuple2<>(tp1.f0 & tp2.f0, null);
                    }
                    if (tp1.f1 != null && tp2.f1 != null) {
                        // merge multiple exception, and keep the details of the first exception
                        AlertException alertException = new AlertException(
                            tp1.f1.getMessage() + "\n" + tp2.f1.getMessage(),
                            tp1.f1);
                        return new Tuple2<>(alertResult, alertException);
                    }
                    return new Tuple2<>(alertResult, tp1.f1 == null ? tp2.f1 : tp1.f1);
                });
    }
}
