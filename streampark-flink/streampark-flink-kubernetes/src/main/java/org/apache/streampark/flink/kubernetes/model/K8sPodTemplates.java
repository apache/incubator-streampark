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

package org.apache.streampark.flink.kubernetes.model;

import org.apache.streampark.common.util.Utils;

import lombok.AllArgsConstructor;
import lombok.Builder;

/** Pod template for flink k8s cluster */
@Builder
@AllArgsConstructor
public class K8sPodTemplates {

    @Builder.Default
    private final String podTemplate = "";
    @Builder.Default
    private final String jmPodTemplate = "";
    @Builder.Default
    private final String tmPodTemplate = "";

    public String podTemplate() {
        return podTemplate;
    }

    public String jmPodTemplate() {
        return jmPodTemplate;
    }

    public String tmPodTemplate() {
        return tmPodTemplate;
    }

    public boolean nonEmpty() {
        return isNotBlank(podTemplate) || isNotBlank(jmPodTemplate) || isNotBlank(tmPodTemplate);
    }

    public boolean isEmpty() {
        return !nonEmpty();
    }

    public static K8sPodTemplates empty() {
        return K8sPodTemplates.builder().build();
    }

    public static K8sPodTemplates of(String podTemplate, String jmPodTemplate, String tmPodTemplate) {
        return K8sPodTemplates.builder()
            .podTemplate(safeGet(podTemplate))
            .jmPodTemplate(safeGet(jmPodTemplate))
            .tmPodTemplate(safeGet(tmPodTemplate))
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
        return Utils.hashCode(podTemplate, jmPodTemplate, tmPodTemplate);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof K8sPodTemplates)) {
            return false;
        }
        K8sPodTemplates that = (K8sPodTemplates) obj;
        return trimSafe(podTemplate).equals(trimSafe(that.podTemplate))
            && trimSafe(jmPodTemplate).equals(trimSafe(that.jmPodTemplate))
            && trimSafe(tmPodTemplate).equals(trimSafe(that.tmPodTemplate));
    }
}
