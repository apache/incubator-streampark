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

import org.apache.commons.lang3.StringUtils;

public class K8sPodTemplates {

  private String podTemplate = "";

  private String jmPodTemplate = "";

  private String tmPodTemplate = "";

  public K8sPodTemplates(String podTemplate, String jmPodTemplate, String tmPodTemplate) {
    this.podTemplate = podTemplate;
    this.jmPodTemplate = jmPodTemplate;
    this.tmPodTemplate = tmPodTemplate;
  }

  public String getPodTemplate() {
    return podTemplate;
  }

  public void setPodTemplate(String podTemplate) {
    this.podTemplate = podTemplate;
  }

  public String getJmPodTemplate() {
    return jmPodTemplate;
  }

  public void setJmPodTemplate(String jmPodTemplate) {
    this.jmPodTemplate = jmPodTemplate;
  }

  public String getTmPodTemplate() {
    return tmPodTemplate;
  }

  public void setTmPodTemplate(String tmPodTemplate) {
    this.tmPodTemplate = tmPodTemplate;
  }

  public boolean nonEmpty() {
    return StringUtils.isNotBlank(podTemplate)
        || StringUtils.isNotBlank(jmPodTemplate)
        || StringUtils.isNotBlank(tmPodTemplate);
  }
}
