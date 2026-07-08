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

package org.apache.streampark.flink.util;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.util.TimeUtils;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** Flink utility methods. */
public final class FlinkUtils {

    private FlinkUtils() {
    }

    public static <R> ListState<R> getUnionListState(
                                                     FunctionInitializationContext context,
                                                     String descriptorName,
                                                     TypeInformation<R> typeInformation) {
        try {
            return context.getOperatorStateStore()
                .getUnionListState(
                    new ListStateDescriptor<>(
                        descriptorName, typeInformation.getTypeClass()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFlinkDistJar(String flinkHome) {
        String[] jars =
            new File(flinkHome + "/lib")
                .list((dir, name) -> name.matches("flink-dist.*\\.jar"));
        if (jars == null || jars.length == 0) {
            throw new IllegalArgumentException(
                "[StreamPark] can no found flink-dist jar in " + flinkHome + "/lib");
        }
        if (jars.length == 1) {
            return flinkHome + "/lib/" + jars[0];
        }
        throw new IllegalArgumentException(
            "[StreamPark] found multiple flink-dist jar in "
                + flinkHome
                + "/lib,["
                + Arrays.stream(jars).collect(Collectors.joining(","))
                + "]");
    }

    public static boolean isCheckpointEnabled(Map<String, String> map) {
        Duration checkpointInterval =
            TimeUtils.parseDuration(
                map.getOrDefault(
                    CheckpointingOptions.CHECKPOINTING_INTERVAL.key(), "0ms"));
        return checkpointInterval.toMillis() > 0;
    }
}
