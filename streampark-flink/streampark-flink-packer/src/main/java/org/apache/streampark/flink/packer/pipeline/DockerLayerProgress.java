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

/** push/pull progress of per docker layer. */
public class DockerLayerProgress {

    private final String layerId;
    private final String status;
    private final long current;
    private final long total;

    public DockerLayerProgress(String layerId, String status, long current, long total) {
        this.layerId = layerId;
        this.status = status;
        this.current = current;
        this.total = total;
    }

    public String layerId() {
        return layerId;
    }

    public String status() {
        return status;
    }

    public long current() {
        return current;
    }

    public long total() {
        return total;
    }

    public double percent() {
        return Utils.calPercent(current, total);
    }

    public double currentMb() {
        if (current == 0) {
            return 0;
        }
        return Double.parseDouble(String.format("%.2f", current / (1024.0 * 1024.0)));
    }

    public double totalMb() {
        if (total == 0) {
            return 0;
        }
        return Double.parseDouble(String.format("%.2f", total / (1024.0 * 1024.0)));
    }
}
