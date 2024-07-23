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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class FlinkStateChangeEvent {

    private Long id;
    private String jobId;
    private FlinkAppStateEnum appState;
    private OptionStateEnum optionState;
    private String jobManagerUrl;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        FlinkStateChangeEvent that = (FlinkStateChangeEvent) object;
        return Objects.equals(id, that.id)
            && Objects.equals(jobId, that.jobId)
            && Objects.equals(appState, that.appState)
            && Objects.equals(optionState, that.optionState)
            && Objects.equals(jobManagerUrl, that.jobManagerUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, jobId, appState, optionState, jobManagerUrl);
    }

}
