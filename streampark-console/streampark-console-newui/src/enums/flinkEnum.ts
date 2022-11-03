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
export enum BuildEnum {
  NOT_BUDIL = -1,
  NEED_REBUILD = -2,
  BUILDING = 0,
  SUCCESSFUL = 1,
  FAILED = 2,
}
/* ExecutionMode  */
export enum ExecutionModeEnum {
  /* remote (standalone) */
  REMOTE = 1,
  /* yarn per-job (deprecated, please use yarn-application mode) */
  YARN_PER_JOB = 2,
  /* yarn session */
  YARN_SESSION = 3,
  /* yarn application */
  YARN_APPLICATION = 4,
  /* kubernetes session */
  KUBERNETES_SESSION = 5,
  /* kubernetes application */
  KUBERNETES_APPLICATION = 6,
}

export const executionMap = {
  REMOTE: ExecutionModeEnum.REMOTE,
  YARN_PER_JOB: ExecutionModeEnum.YARN_PER_JOB,
  YARN_SESSION: ExecutionModeEnum.YARN_SESSION,
  YARN_APPLICATION: ExecutionModeEnum.YARN_APPLICATION,
  KUBERNETES_SESSION: ExecutionModeEnum.KUBERNETES_SESSION,
  KUBERNETES_APPLICATION: ExecutionModeEnum.KUBERNETES_APPLICATION,
};
