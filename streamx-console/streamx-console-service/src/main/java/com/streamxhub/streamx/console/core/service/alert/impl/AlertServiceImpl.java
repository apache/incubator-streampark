/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.service.alert.impl;

import com.streamxhub.streamx.console.base.exception.AlertException;
import com.streamxhub.streamx.console.base.util.SpringContextUtils;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.alert.AlertConfig;
import com.streamxhub.streamx.console.core.entity.alert.AlertConfigWithParams;
import com.streamxhub.streamx.console.core.entity.alert.AlertTemplate;
import com.streamxhub.streamx.console.core.enums.AlertType;
import com.streamxhub.streamx.console.core.enums.CheckPointStatus;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
import com.streamxhub.streamx.console.core.service.alert.AlertConfigService;
import com.streamxhub.streamx.console.core.service.alert.AlertNotifyService;
import com.streamxhub.streamx.console.core.service.alert.AlertService;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.tuple.Tuple2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author weijinglun
 * @date 2022.01.14
 */
@Slf4j
@Service
public class AlertServiceImpl implements AlertService {
    @Autowired
    private AlertConfigService alertConfigService;

    @Override
    public void alert(Application application, CheckPointStatus checkPointStatus) {
        AlertTemplate alertTemplate = AlertTemplate.of(application, checkPointStatus);
        alert(application, alertTemplate);
    }

    @Override
    public void alert(Application application, FlinkAppState appState) {
        AlertTemplate alertTemplate = AlertTemplate.of(application, appState);
        alert(application, alertTemplate);
    }

    private void alert(Application application, AlertTemplate alertTemplate) {
        Integer alertId = application.getAlertId();
        if (alertId == null) {
            return;
        }
        AlertConfig alertConfig = alertConfigService.getById(alertId);
        try {
            alert(AlertConfigWithParams.of(alertConfig), alertTemplate);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public boolean alert(AlertConfigWithParams params, AlertTemplate alertTemplate) throws AlertException {
        List<AlertType> alertTypes = AlertType.decode(params.getAlertType());
        if (CollectionUtils.isEmpty(alertTypes)) {
            return true;
        }
        // 没有使用线程池,保证报警能够成功发送
        Tuple2<Boolean, AlertException> reduce = alertTypes.stream().map(alertType -> {
            try {
                // 处理每种报警类型的异常,并收集异常
                boolean alertRes = SpringContextUtils
                    .getBean(alertType.getServiceType(), AlertNotifyService.class)
                    .doAlert(params, alertTemplate);
                return new Tuple2<Boolean, AlertException>(alertRes, null);
            } catch (AlertException e) {
                return new Tuple2<>(false, e);
            }
        }).reduce(new Tuple2<>(true, null), (tp1, tp2) -> {
            boolean alertResult = tp1.f0 & tp2.f0;
            if (tp1.f1 == null && tp2.f1 == null) {
                return new Tuple2<>(tp1.f0 & tp2.f0, null);
            }
            if (tp1.f1 != null && tp2.f1 != null) {
                // 合并多个异常的 message 信息,只保留第一个异常的详细内容
                AlertException alertException = new AlertException(tp1.f1.getMessage() + "\n" + tp2.f1.getMessage(), tp1.f1);
                return new Tuple2<>(alertResult, alertException);
            }
            return new Tuple2<>(alertResult, tp1.f1 == null ? tp2.f1 : tp1.f1);
        });
        if (reduce.f1 != null) {
            throw reduce.f1;
        }

        return reduce.f0;
    }
}
