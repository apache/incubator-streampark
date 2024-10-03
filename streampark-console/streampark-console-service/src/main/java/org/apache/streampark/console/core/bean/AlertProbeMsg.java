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

package org.apache.streampark.console.core.bean;

import org.apache.streampark.console.core.enums.FlinkAppStateEnum;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
public class AlertProbeMsg {

    private Set<Long> alertId;

    private String user;

    private Integer probeJobs = 0;

    private Integer failedJobs = 0;

    private Integer lostJobs = 0;

    private Integer cancelledJobs = 0;

    public void compute(FlinkAppStateEnum state) {
        this.probeJobs++;
        switch (state) {
            case LOST:
                this.lostJobs++;
                break;
            case FAILED:
                failedJobs++;
                break;
            case CANCELED:
                cancelledJobs++;
                break;
            default:
                break;
        }
    }
}
