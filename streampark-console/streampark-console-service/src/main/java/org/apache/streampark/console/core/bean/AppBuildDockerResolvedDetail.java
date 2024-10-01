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

import org.apache.streampark.flink.packer.pipeline.DockerBuildSnapshot;
import org.apache.streampark.flink.packer.pipeline.DockerPullSnapshot;
import org.apache.streampark.flink.packer.pipeline.DockerPushSnapshot;
import org.apache.streampark.flink.packer.pipeline.DockerResolvedSnapshot;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/** View for DockerResolveProgress */
@Getter
@Setter
@Accessors(chain = true)
public class AppBuildDockerResolvedDetail {

    @Nullable
    private ImagePull pull;
    @Nullable
    private ImageBuild build;
    @Nullable
    private ImagePush push;

    public static AppBuildDockerResolvedDetail of(@Nullable DockerResolvedSnapshot snapshot) {
        AppBuildDockerResolvedDetail detail = new AppBuildDockerResolvedDetail();
        if (snapshot == null) {
            return detail;
        }
        return detail
            .setPull(ImagePull.of(snapshot.pull()))
            .setBuild(ImageBuild.of(snapshot.build()))
            .setPush(ImagePush.of(snapshot.push()));
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class ImagePull {

        private List<ImageLayer> layers;
        private Date st;

        public static ImagePull of(DockerPullSnapshot snapshot) {
            if (snapshot == null) {
                return null;
            }
            return new ImagePull()
                .setSt(new Date(snapshot.emitTime()))
                .setLayers(
                    snapshot.detailAsJava().stream()
                        .map(
                            e -> new ImageLayer()
                                .setLayerId(e.layerId())
                                .setStatus(e.status())
                                .setCurrentMb(e.currentMb())
                                .setTotalMb(e.totalMb())
                                .setPercent(e.percent()))
                        .sorted(Comparator.comparing(ImageLayer::getStatus)
                            .reversed())
                        .collect(Collectors.toList()));
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class ImageBuild {

        private List<String> steps;
        private Date st;

        public static ImageBuild of(DockerBuildSnapshot snapshot) {
            if (snapshot == null) {
                return null;
            }
            return new ImageBuild()
                .setSt(new Date(snapshot.emitTime()))
                .setSteps(
                    snapshot.detailAsJava().stream()
                        .filter(e -> e.trim().startsWith("Step")
                            || e.trim().startsWith("step"))
                        .collect(Collectors.toList()));
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class ImagePush {

        private List<ImageLayer> layers;
        private Date st;

        public static ImagePush of(DockerPushSnapshot snapshot) {
            if (snapshot == null) {
                return null;
            }
            return new ImagePush()
                .setSt(new Date(snapshot.emitTime()))
                .setLayers(
                    snapshot.detailAsJava().stream()
                        .map(
                            e -> new ImageLayer()
                                .setLayerId(e.layerId())
                                .setStatus(e.status())
                                .setCurrentMb(e.currentMb())
                                .setTotalMb(e.totalMb())
                                .setPercent(e.percent()))
                        .sorted(Comparator.comparing(ImageLayer::getStatus)
                            .reversed())
                        .collect(Collectors.toList()));
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class ImageLayer {

        private String layerId;
        private String status;
        private Double currentMb;
        private Double totalMb;
        private Double percent;
    }
}
