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

package org.apache.streampark.spark.kubernetes.model;

import org.apache.streampark.common.util.Utils;

import lombok.AllArgsConstructor;
import lombok.Builder;

/** Pod template for Spark k8s cluster */
@Builder
@AllArgsConstructor
public class SparkKubernetesPodTemplates {

    @Builder.Default
    private final String driverPodTemplate = "";
    @Builder.Default
    private final String executorPodTemplate = "";

    public String driverPodTemplate() {
        return driverPodTemplate;
    }

    public String executorPodTemplate() {
        return executorPodTemplate;
    }

    public boolean nonEmpty() {
        return isNotBlank(driverPodTemplate) || isNotBlank(executorPodTemplate);
    }

    public boolean isEmpty() {
        return !nonEmpty();
    }

    public static SparkKubernetesPodTemplates empty() {
        return SparkKubernetesPodTemplates.builder().build();
    }

    public static SparkKubernetesPodTemplates of(String driverPodTemplate, String executorPodTemplate) {
        return SparkKubernetesPodTemplates.builder()
            .driverPodTemplate(safeGet(driverPodTemplate))
            .executorPodTemplate(safeGet(executorPodTemplate))
            .build();
    }

    private static String safeGet(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        return content;
    }

    private static boolean isNotBlank(String content) {
        return content != null && !content.trim().isEmpty();
    }

    private static String trimSafe(String content) {
        if (content == null) {
            return "";
        }
        return content.trim();
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(driverPodTemplate, executorPodTemplate);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SparkKubernetesPodTemplates)) {
            return false;
        }
        SparkKubernetesPodTemplates that = (SparkKubernetesPodTemplates) obj;
        return trimSafe(driverPodTemplate).equals(trimSafe(that.driverPodTemplate))
            && trimSafe(executorPodTemplate).equals(trimSafe(that.executorPodTemplate));
    }
}
