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

package org.apache.streampark.console.core.assembler;

import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.request.common.TeamScopedIdRequest;
import org.apache.streampark.console.core.request.project.ProjectBuildLogRequest;
import org.apache.streampark.console.core.request.project.ProjectCreateRequest;
import org.apache.streampark.console.core.request.project.ProjectExistsRequest;
import org.apache.streampark.console.core.request.project.ProjectGitRequest;
import org.apache.streampark.console.core.request.project.ProjectListQueryRequest;
import org.apache.streampark.console.core.request.project.ProjectModuleRequest;
import org.apache.streampark.console.core.request.project.ProjectUpdateRequest;
import org.apache.streampark.console.core.response.project.ProjectBranchesResponse;
import org.apache.streampark.console.core.response.project.ProjectResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;

import java.util.List;

/** Converts between project entities and API request/response contracts. */
public final class ProjectAssembler {

    private ProjectAssembler() {
    }

    public static Project toEntity(ProjectCreateRequest request) {
        if (request == null) {
            return null;
        }
        Project project = new Project();
        BeanUtils.copyProperties(request, project);
        return project;
    }

    public static Project toEntity(ProjectUpdateRequest request) {
        if (request == null) {
            return null;
        }
        Project project = toEntity((ProjectCreateRequest) request);
        project.setId(request.getId());
        return project;
    }

    public static Project toEntity(ProjectListQueryRequest request) {
        if (request == null) {
            return null;
        }
        Project project = new Project();
        BeanUtils.copyProperties(request, project);
        return project;
    }

    public static Project toEntity(TeamScopedIdRequest request) {
        if (request == null) {
            return null;
        }
        Project project = new Project();
        project.setId(request.getId());
        project.setTeamId(request.getTeamId());
        return project;
    }

    public static Project toEntity(ProjectGitRequest request) {
        if (request == null) {
            return null;
        }
        Project project = toEntity((TeamScopedIdRequest) request);
        BeanUtils.copyProperties(request, project, "id", "teamId");
        return project;
    }

    public static Project toEntity(ProjectModuleRequest request) {
        if (request == null) {
            return null;
        }
        Project project = toEntity((TeamScopedIdRequest) request);
        project.setModule(request.getModule());
        return project;
    }

    public static Project toEntity(ProjectExistsRequest request) {
        if (request == null) {
            return null;
        }
        Project project = toEntity((TeamScopedIdRequest) request);
        project.setName(request.getName());
        return project;
    }

    public static Project toEntity(ProjectBuildLogRequest request) {
        return toEntity((TeamScopedIdRequest) request);
    }

    public static ProjectResponse toResponse(Project project) {
        return DtoAssembler.toDto(project, ProjectResponse.class);
    }

    public static IPage<ProjectResponse> toPageResponse(IPage<Project> page) {
        return DtoAssembler.toPage(page, ProjectAssembler::toResponse);
    }

    public static List<ProjectResponse> toListResponse(List<Project> projects) {
        return DtoAssembler.toList(projects, ProjectAssembler::toResponse);
    }

    public static ProjectBranchesResponse toBranchesResponse(List<String> branches, List<String> tags) {
        ProjectBranchesResponse response = new ProjectBranchesResponse();
        response.setBranches(branches);
        response.setTags(tags);
        return response;
    }
}
