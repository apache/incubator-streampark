/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.streampark.console.api.v2.controller;

import org.apache.streampark.console.api.controller.FlinkPodTemplatesApi;

import org.springframework.http.ResponseEntity;

public class FlinkPodTemplatesApiImpl implements FlinkPodTemplatesApi {

  @Override
  public ResponseEntity<Void> extractHostAlias() {
    return FlinkPodTemplatesApi.super.extractHostAlias();
  }

  @Override
  public ResponseEntity<Void> getHostAliasPodTemplate() {
    return FlinkPodTemplatesApi.super.getHostAliasPodTemplate();
  }

  @Override
  public ResponseEntity<Void> getInitPodTemplate() {
    return FlinkPodTemplatesApi.super.getInitPodTemplate();
  }

  @Override
  public ResponseEntity<Void> getSystemHosts() {
    return FlinkPodTemplatesApi.super.getSystemHosts();
  }

  @Override
  public ResponseEntity<Void> previewHostAliasPodTemplate() {
    return FlinkPodTemplatesApi.super.previewHostAliasPodTemplate();
  }
}
