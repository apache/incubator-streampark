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

import org.apache.streampark.console.core.entity.CloudAccount;
import org.apache.streampark.console.core.managed.model.CloudAccountView;

/** Maps persistence entities to secret-free managed Flink API models. */
final class CloudAccountModelMapper {

    private CloudAccountModelMapper() {
    }

    static CloudAccountView toView(CloudAccount account) {
        return CloudAccountView.builder()
            .id(account.getId())
            .accountName(account.getAccountName())
            .providerType(account.getProviderType())
            .region(account.getRegion())
            .endpoint(account.getEndpoint())
            .accessKeyMask(account.getAccessKeyMask())
            .connectivityState(account.getConnectivityState())
            .lastCheckTime(account.getLastCheckTime())
            .lastErrorCode(account.getLastErrorCode())
            .lastErrorMessage(account.getLastErrorMessage())
            .status(account.getStatus())
            .description(account.getDescription())
            .createUserId(account.getCreateUserId())
            .createTime(account.getCreateTime())
            .modifyTime(account.getModifyTime())
            .version(account.getVersion())
            .build();
    }
}
