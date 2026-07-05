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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class K8sPodTemplates {
    private String podTemplate = "";
    private String jmPodTemplate = "";
    private String tmPodTemplate = "";

    public boolean nonEmpty() {
        return isNotBlank(podTemplate) || isNotBlank(jmPodTemplate) || isNotBlank(tmPodTemplate);
    }

    public boolean isEmpty() {
        return !nonEmpty();
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
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
        return safeTrim(podTemplate).equals(safeTrim(that.podTemplate))
            && safeTrim(jmPodTemplate).equals(safeTrim(that.jmPodTemplate))
            && safeTrim(tmPodTemplate).equals(safeTrim(that.tmPodTemplate));
    }

    private static String safeTrim(String content) {
        if (content == null) {
            return "";
        }
        return content.trim();
    }

    public static K8sPodTemplates empty() {
        return new K8sPodTemplates();
    }

    public static K8sPodTemplates of(String podTemplate, String jmPodTemplate, String tmPodTemplate) {
        return new K8sPodTemplates(safeGet(podTemplate), safeGet(jmPodTemplate), safeGet(tmPodTemplate));
    }

    private static String safeGet(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        return content;
    }
}
