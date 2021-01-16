/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.console.core.task;

import java.io.File;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.streamxhub.streamx.console.base.utils.WebUtil;
import com.streamxhub.streamx.console.core.service.FlameGraphService;

/**
 * @author benjobs
 */
@Slf4j
@Component
public class MetricsTask {

    @Autowired
    private FlameGraphService flameGraphService;

    private final String FLAMEGRAPH_FILE_REGEXP = "\\d+_\\d+\\.json|\\d+_\\d+\\.folded|\\d+_\\d+\\.svg";
    /**
     * hour.
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanFlameGraph() {
        // 1) clean file
        String tempPath = WebUtil.getAppDir("temp");
        File temp = new File(tempPath);
        Arrays.stream(Objects.requireNonNull(temp.listFiles()))
                .filter(x -> x.getName().matches(FLAMEGRAPH_FILE_REGEXP))
                .forEach(File::delete);

        // 2 clean date
        Date start = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        cal.setTime(start);
        cal.add(Calendar.HOUR_OF_DAY, -24);
        Date end = cal.getTime();
        flameGraphService.clean(end);
    }
}
