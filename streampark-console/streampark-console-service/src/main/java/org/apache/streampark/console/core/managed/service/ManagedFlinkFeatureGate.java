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

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.managed.api.ManagedFlinkProviderType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Central feature gate for managed Flink visibility, provider activation, and writes. */
@Component
public class ManagedFlinkFeatureGate {

    private final boolean enabled;

    private final boolean volcengineEnabled;

    public ManagedFlinkFeatureGate(
                                   @Value("${streampark.managed-flink.enabled:false}") boolean enabled,
                                   @Value("${streampark.managed-flink.providers.volcengine.enabled:false}") boolean volcengineEnabled) {
        this.enabled = enabled;
        this.volcengineEnabled = volcengineEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isProviderEnabled(ManagedFlinkProviderType providerType) {
        if (!enabled || providerType == null) {
            return false;
        }
        switch (providerType) {
            case VOLCENGINE:
                return volcengineEnabled;
            default:
                return false;
        }
    }

    public void requireWriteEnabled(ManagedFlinkProviderType providerType) {
        ApiAlertException.throwIfFalse(
            enabled, "Managed Flink is disabled by streampark.managed-flink.enabled.");
        ApiAlertException.throwIfNull(providerType, "Managed Flink provider type is required.");
        ApiAlertException.throwIfFalse(
            isProviderEnabled(providerType),
            String.format(
                "Managed Flink provider %s is disabled by its provider feature flag.",
                providerType));
    }
}
