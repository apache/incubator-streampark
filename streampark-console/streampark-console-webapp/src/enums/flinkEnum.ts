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

export enum BuildStateEnum {
  /** has changed, need rebuild */
  NEED_REBUILD = -2,
  /** has cancelled, not build */
  NOT_BUDIL = -1,
  /** building */
  BUILDING = 0,
  /** build successful */
  SUCCESSFUL = 1,
  /** build failed  */
  FAILED = 2,
}
/* ExecutionMode  */
export enum ExecModeEnum {
  /** remote (standalone) */
  REMOTE = 1,
  /** yarn per-job (deprecated, please use yarn-application mode) */
  YARN_PER_JOB = 2,
  /** yarn session */
  YARN_SESSION = 3,
  /** yarn application */
  YARN_APPLICATION = 4,
  /** kubernetes session */
  KUBERNETES_SESSION = 5,
  /** kubernetes application */
  KUBERNETES_APPLICATION = 6,
}

export const executionMap = {
  /**1. remote (standalone) */
  REMOTE: ExecModeEnum.REMOTE,
  /**2. yarn per-job (deprecated, please use yarn-application mode) */
  YARN_PER_JOB: ExecModeEnum.YARN_PER_JOB,
  /**3. yarn session */
  YARN_SESSION: ExecModeEnum.YARN_SESSION,
  /**4. yarn application */
  YARN_APPLICATION: ExecModeEnum.YARN_APPLICATION,
  /**5. kubernetes session */
  KUBERNETES_SESSION: ExecModeEnum.KUBERNETES_SESSION,
  /**6. kubernetes application */
  KUBERNETES_APPLICATION: ExecModeEnum.KUBERNETES_APPLICATION,
};

export enum LaunchStateEnum {
  /** launch failed */
  FAILED = -1,
  /** launch done */
  DONE = 0,
  /** need relaunch after modify task */
  NEED_LAUNCH = 1,
  /** launching */
  LAUNCHING = 2,
  /** launch complete, need restart */
  NEED_RESTART = 3,
  /**  need rollback */
  NEED_ROLLBACK = 4,
  /**
   * project has changed, need to check the jar whether to be re-selected
   */
  NEED_CHECK = 5,
  /**
   * revoked
   */
  REVOKED = 10,
}

export enum OptionStateEnum {
  /**  Application which is currently action: none. */
  NONE = 0,
  /** Application which is currently action: deploying. */
  LAUNCHING = 1,
  /** Application which is currently action: cancelling. */
  CANCELLING = 2,
  /** Application which is currently action: starting. */
  STARTING = 3,
  /** Application which is currently action: savepointing. */
  SAVEPOINTING = 4,
}
