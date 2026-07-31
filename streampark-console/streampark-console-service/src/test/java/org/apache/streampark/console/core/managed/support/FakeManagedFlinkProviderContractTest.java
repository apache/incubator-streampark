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

package org.apache.streampark.console.core.managed.support;

import org.apache.streampark.console.core.managed.api.ManagedFlinkProvider;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderMetadataContract;
import org.apache.streampark.console.core.managed.api.ProviderContext;

import java.time.Clock;
import java.time.Duration;

class FakeManagedFlinkProviderContractTest extends ManagedFlinkProviderMetadataContract {

    private final FakeManagedFlinkProvider provider =
        new FakeManagedFlinkProvider(Clock.systemUTC(), Duration.ofMinutes(5));

    @Override
    protected ManagedFlinkProvider provider() {
        return provider;
    }

    @Override
    protected ProviderContext validContext() {
        return ProviderContext.builder()
            .cloudAccountId(FakeManagedFlinkProvider.VALID_ACCOUNT_ID)
            .credentialVersion(1L)
            .region("cn-beijing")
            .build();
    }

    @Override
    protected ProviderContext invalidCredentialContext() {
        return ProviderContext.builder()
            .cloudAccountId(FakeManagedFlinkProvider.INVALID_ACCOUNT_ID)
            .credentialVersion(1L)
            .region("cn-beijing")
            .build();
    }
}
