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

package org.apache.streampark.console.core.task;

import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.service.FlameGraphService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

@Slf4j
@Component
public class MetricsTask {

    @Autowired
    private FlameGraphService flameGraphService;

    private static final String FLAME_GRAPH_FILE_REGEXP = "\\d+_\\d+\\.json|\\d+_\\d+\\.folded|\\d+_\\d+\\.svg";

    /**
     * hour.
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanFlameGraph() {
        // 1) clean file
        String tempPath = WebUtils.getAppTempDir().getAbsolutePath();
        File temp = new File(tempPath);
        Arrays.stream(Objects.requireNonNull(temp.listFiles()))
            .filter(x -> x.getName().matches(FLAME_GRAPH_FILE_REGEXP))
            .forEach(File::delete);

        // 2 clean date
        Date start = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTime(start);
        calendar.add(Calendar.HOUR_OF_DAY, -24);
        Date end = calendar.getTime();
        flameGraphService.clean(end);
    }
}
