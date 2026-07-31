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

package org.apache.streampark.console.core.managed.api;

import lombok.Builder;
import lombok.Value;

/**
 * Non-secret provider request context.
 *
 * <p>Credentials are resolved by the provider client factory from the cloud account and credential
 * version. Plaintext credentials must never be added to this value object.
 */
@Value
@Builder
public class ProviderContext {

    Long cloudAccountId;

    Long credentialVersion;

    String region;

    String endpoint;

    String projectId;
}
