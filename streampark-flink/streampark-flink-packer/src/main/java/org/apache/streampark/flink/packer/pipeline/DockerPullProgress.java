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

import com.github.dockerjava.api.model.PullResponseItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class DockerPullProgress {

    private final Map<String, DockerLayerProgress> layers = new LinkedHashMap<>();
    private String error = "";
    private long lastTime;

    public DockerPullProgress(long lastTime) {
        this.lastTime = lastTime;
    }

    public void update(PullResponseItem pullRsp) {
        if (pullRsp == null
            || StringUtils.isBlank(pullRsp.getId())
            || StringUtils.isBlank(pullRsp.getStatus())) {
            return;
        }
        if (pullRsp.getStatus().contains("complete")) {
            layers.put(
                pullRsp.getId(),
                new DockerLayerProgress(pullRsp.getId(), pullRsp.getStatus(), 1, 1));
            lastTime = System.currentTimeMillis();
        } else {
            long cur =
                pullRsp.getProgressDetail() == null || pullRsp.getProgressDetail().getCurrent() == null
                    ? 0
                    : pullRsp.getProgressDetail().getCurrent();
            long total =
                pullRsp.getProgressDetail() == null || pullRsp.getProgressDetail().getTotal() == null
                    ? 0
                    : pullRsp.getProgressDetail().getTotal();
            layers.put(
                pullRsp.getId(),
                new DockerLayerProgress(pullRsp.getId(), pullRsp.getStatus(), cur, total));
            error =
                pullRsp.getErrorDetail() == null ? "" : pullRsp.getErrorDetail().getMessage();
            lastTime = System.currentTimeMillis();
        }
    }

    public DockerPullSnapshot snapshot() {
        return DockerPullSnapshot.of(new ArrayList<>(layers.values()), error, lastTime);
    }

    public static DockerPullProgress empty() {
        return new DockerPullProgress(System.currentTimeMillis());
    }
}
