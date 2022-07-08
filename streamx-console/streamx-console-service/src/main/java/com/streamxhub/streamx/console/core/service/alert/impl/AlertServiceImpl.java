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

    @Override
    public void alert(Application application, AlertTemplate alertTemplate) {
        Integer alertId = application.getAlertId();
        if (alertId == null) {
            return;
        }
        AlertConfig alertConfig = alertConfigService.getById(alertId);
        alert(AlertConfigWithParams.of(alertConfig), alertTemplate);
    }

    @Override
    public boolean alert(AlertConfigWithParams params, AlertTemplate alertTemplate) {
        List<AlertType> alertTypes = AlertType.decode(params.getAlertType());
        if (CollectionUtils.isEmpty(alertTypes)) {
            return true;
        }

        boolean result = true;
        for (AlertType alertType : alertTypes) {
            result &= SpringContextUtils.getBean(alertType.getServiceType(), AlertNotifyService.class)
                    .doAlert(params, alertTemplate);
        }
        return result;
    }
}
