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

import org.apache.streampark.console.api.controller.ExternalLinksApi;

import org.springframework.http.ResponseEntity;

public class ExternalLinksApiImpl implements ExternalLinksApi {
  @Override
  public ResponseEntity<Void> createExternalLink() {
    return ExternalLinksApi.super.createExternalLink();
  }

  @Override
  public ResponseEntity<Void> deleteExternalLink(Long linkId) {
    return ExternalLinksApi.super.deleteExternalLink(linkId);
  }

  @Override
  public ResponseEntity<Void> listExternalLink() {
    return ExternalLinksApi.super.listExternalLink();
  }

  @Override
  public ResponseEntity<Void> updateExternalLink(Long linkId) {
    return ExternalLinksApi.super.updateExternalLink(linkId);
  }
}
