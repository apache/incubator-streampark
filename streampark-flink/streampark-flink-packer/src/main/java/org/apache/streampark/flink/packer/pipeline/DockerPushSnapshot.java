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

package org.apache.streampark.flink.packer.pipeline;

import org.apache.streampark.common.util.Utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Data
@Accessors(fluent = true)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerPushSnapshot {

    private List<DockerLayerProgress> detail;
    private String error;
    private long emitTime;
    private double percent;

    public List<DockerLayerProgress> detailAsJava() {
        return detail == null ? Collections.emptyList() : detail;
    }

    public static DockerPushSnapshot of(List<DockerLayerProgress> detail, String error, long emitTime) {
        long current = detail.stream().mapToLong(DockerLayerProgress::current).sum();
        long total = detail.stream().mapToLong(DockerLayerProgress::total).sum();
        return new DockerPushSnapshot(detail, error, emitTime, Utils.calPercent(current, total));
    }
}
