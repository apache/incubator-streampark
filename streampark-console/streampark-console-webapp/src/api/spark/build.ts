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
import type { Result } from '/#/axios';
import { defHttp } from '/@/utils/http/axios';

const apiPrefix = `/spark/pipe`;

/**
 * Release application building pipeline.
 *
 * @param appId application id
 * @param forceBuild forced start pipeline or not
 * @return Whether the pipeline was successfully started
 */
export function fetchBuildSparkApp(data: { appId: string; forceBuild: boolean }) {
  return defHttp.post<Result<boolean>>(
    { url: `${apiPrefix}/build`, data },
    { isTransformResponse: false },
  );
}

/**
 * Get application building pipeline progress detail.
 *
 * @param appId application id
 * @return "pipeline" -> pipeline details, "docker" -> docker resolved snapshot
 */

export function fetchBuildProgressDetail(appId: number) {
  return defHttp.post<{
    pipeline: any;
    docker: any;
  }>({ url: `${apiPrefix}/detail`, data: { appId } });
}
