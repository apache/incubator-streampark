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
import org.apache.streampark.console.core.managed.api.CloudProject;
import org.apache.streampark.console.core.managed.api.ManagedDraftDirectory;
import org.apache.streampark.console.core.managed.api.ManagedFlinkCapability;
import org.apache.streampark.console.core.managed.api.ManagedResourcePool;
import org.apache.streampark.console.core.managed.model.ManagedFlinkDraftDirectoryRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkMetadataRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkResourcePoolRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Default Team-authorized provider metadata facade. */
@Service
@RequiredArgsConstructor
public class ManagedFlinkMetadataServiceImpl implements ManagedFlinkMetadataService {

    private final ManagedFlinkProviderContextService contextService;

    @Override
    public ManagedFlinkCapability capability(ManagedFlinkMetadataRequest request) {
        ManagedFlinkProviderSession session =
            contextService.resolve(request.getTeamId(), request.getCloudAccountId(), null);
        return session.getProvider().getCapability(session.getContext());
    }

    @Override
    public List<CloudProject> projects(ManagedFlinkMetadataRequest request) {
        ManagedFlinkProviderSession session =
            contextService.resolve(request.getTeamId(), request.getCloudAccountId(), null);
        ManagedFlinkCapability capability =
            session.getProvider().getCapability(session.getContext());
        ApiAlertException.throwIfFalse(
            capability.isSupportsProjectList(),
            "The managed Flink provider does not support project discovery.");
        return session.getProvider().listProjects(session.getContext(), request.getKeyword());
    }

    @Override
    public List<ManagedResourcePool> resourcePools(
                                                   ManagedFlinkResourcePoolRequest request) {
        ManagedFlinkProviderSession session =
            contextService.resolve(
                request.getTeamId(), request.getCloudAccountId(), request.getProjectId());
        ManagedFlinkCapability capability =
            session.getProvider().getCapability(session.getContext());
        ApiAlertException.throwIfFalse(
            capability.isSupportsResourcePoolList(),
            "The managed Flink provider does not support resource pool discovery.");
        return session
            .getProvider()
            .listResourcePools(
                session.getContext(), request.getProjectId(), request.getKeyword());
    }

    @Override
    public List<ManagedDraftDirectory> draftDirectories(
                                                        ManagedFlinkDraftDirectoryRequest request) {
        ManagedFlinkProviderSession session =
            contextService.resolve(
                request.getTeamId(), request.getCloudAccountId(), request.getProjectId());
        try {
            return session
                .getProvider()
                .listDraftDirectories(
                    session.getContext(), request.getProjectId(), request.getKeyword());
        } catch (UnsupportedOperationException exception) {
            throw new ApiAlertException(
                "The managed Flink provider does not support draft directory discovery.");
        }
    }
}
