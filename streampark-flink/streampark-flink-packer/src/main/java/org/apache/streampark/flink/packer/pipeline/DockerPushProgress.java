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

import org.apache.commons.lang3.StringUtils;

import com.github.dockerjava.api.model.PushResponseItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class DockerPushProgress {

    private final Map<String, DockerLayerProgress> layers = new LinkedHashMap<>();
    private String error = "";
    private long lastTime;

    public DockerPushProgress(long lastTime) {
        this.lastTime = lastTime;
    }

    public void update(PushResponseItem pushRsp) {
        if (pushRsp == null
            || StringUtils.isBlank(pushRsp.getId())
            || StringUtils.isBlank(pushRsp.getStatus())) {
            return;
        }
        if (pushRsp.getStatus().contains("complete")) {
            layers.put(
                pushRsp.getId(),
                new DockerLayerProgress(pushRsp.getId(), pushRsp.getStatus(), 1, 1));
            lastTime = System.currentTimeMillis();
        } else {
            long cur =
                pushRsp.getProgressDetail() == null || pushRsp.getProgressDetail().getCurrent() == null
                    ? 0L
                    : pushRsp.getProgressDetail().getCurrent();
            long total =
                pushRsp.getProgressDetail() == null || pushRsp.getProgressDetail().getTotal() == null
                    ? 0L
                    : pushRsp.getProgressDetail().getTotal();
            layers.put(
                pushRsp.getId(),
                new DockerLayerProgress(pushRsp.getId(), pushRsp.getStatus(), cur, total));
            error =
                pushRsp.getErrorDetail() == null ? "" : pushRsp.getErrorDetail().getMessage();
            lastTime = System.currentTimeMillis();
        }
    }

    public DockerPushSnapshot snapshot() {
        return DockerPushSnapshot.of(new ArrayList<>(layers.values()), error, lastTime);
    }

    public static DockerPushProgress empty() {
        return new DockerPushProgress(System.currentTimeMillis());
    }
}
