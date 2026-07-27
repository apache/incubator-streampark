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

import java.util.List;

/** snapshot for pushing docker image progress. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerPushSnapshot {

    private final List<DockerLayerProgress> detail;
    private final String error;
    private final long emitTime;
    private final double percent;

    public DockerPushSnapshot(
                              List<DockerLayerProgress> detail,
                              String error,
                              long emitTime,
                              double percent) {
        this.detail = detail;
        this.error = error;
        this.emitTime = emitTime;
        this.percent = percent;
    }

    public List<DockerLayerProgress> detail() {
        return detail;
    }

    public String error() {
        return error;
    }

    public long emitTime() {
        return emitTime;
    }

    public double percent() {
        return percent;
    }

    public List<DockerLayerProgress> detailAsJava() {
        return detail;
    }

    public static DockerPushSnapshot of(
                                        List<DockerLayerProgress> detail,
                                        String error,
                                        long emitTime) {
        long currentSum = detail.stream().mapToLong(DockerLayerProgress::current).sum();
        long totalSum = detail.stream().mapToLong(DockerLayerProgress::total).sum();
        return new DockerPushSnapshot(
            detail, error, emitTime, Utils.calPercent(currentSum, totalSum));
    }
}
