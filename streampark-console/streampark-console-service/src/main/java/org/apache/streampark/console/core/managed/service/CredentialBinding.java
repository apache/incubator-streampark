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

package org.apache.streampark.console.core.managed.service;

import lombok.Value;

import java.util.Objects;

/** Immutable identity bound to credential ciphertext through AES-GCM AAD. */
@Value
public class CredentialBinding {

    String providerType;

    Long accountId;

    CredentialField field;

    public CredentialBinding(
                             String providerType, Long accountId, CredentialField field) {
        if (providerType == null || providerType.trim().isEmpty()) {
            throw new IllegalArgumentException("providerType must not be blank");
        }
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("accountId must be greater than zero");
        }
        this.providerType = providerType.trim();
        this.accountId = accountId;
        this.field = Objects.requireNonNull(field, "field must not be null");
    }
}
