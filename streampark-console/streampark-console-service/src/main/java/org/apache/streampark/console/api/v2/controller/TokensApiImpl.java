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

import org.apache.streampark.console.api.controller.TokensApi;

import org.springframework.http.ResponseEntity;

public class TokensApiImpl implements TokensApi {

  @Override
  public ResponseEntity<Void> createToken() {
    return TokensApi.super.createToken();
  }

  @Override
  public ResponseEntity<Void> deleteToken(Long tokenId) {
    return TokensApi.super.deleteToken(tokenId);
  }

  @Override
  public ResponseEntity<Void> listToken() {
    return TokensApi.super.listToken();
  }

  @Override
  public ResponseEntity<Void> updateToken(Long tokenId) {
    return TokensApi.super.updateToken(tokenId);
  }

  @Override
  public ResponseEntity<Void> verifyCurrentTokenStatus() {
    return TokensApi.super.verifyCurrentTokenStatus();
  }
}
