/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { request } from '../../http'

export function fetchProjectList(data?: Recordable) {
    return request.Post<Recordable[]>('/project/list', data ?? {})
}

export function fetchProjectExists(data: Recordable) {
    return request.Post<boolean>('/project/exists', data)
}

export function fetchProjectGitCheck(data: Recordable) {
    return request.Post<Recordable>('/project/git_check', data)
}

export function fetchProjectBranches(data: Recordable) {
    return request.Post<string[]>('/project/branches', data)
}

export function fetchProjectCreate(data: Recordable) {
    return request.Post<boolean>('/project/create', data)
}

export function fetchProjectDetail(data: Recordable) {
    return request.Post<Recordable>('/project/get', data)
}

export function fetchProjectUpdate(data: Recordable) {
    return request.Post<boolean>('/project/update', data)
}

export function fetchProjectBuild(data: Recordable) {
    return request.Post<boolean>('/project/build', data)
}

export function fetchProjectBuildLog(data: Recordable) {
    return request.Post<string>('/project/build_log', data)
}

export function fetchProjectDelete(data: Recordable) {
    return request.Post<boolean>('/project/delete', data)
}

export function fetchProjectModules(data: Recordable) {
    return request.Post<string[]>('/project/modules', data)
}

export function fetchProjectJars(data: Recordable) {
    return request.Post<string[]>('/project/jars', data)
}

export function fetchProjectListConf(data: Recordable) {
    return request.Post<Recordable[]>('/project/list_conf', data)
}

export function fetchProjectSelect(data: Recordable) {
    return request.Post<Recordable[]>('/project/select', data)
}
