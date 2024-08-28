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
// dashboard
export interface DashboardResponse {
  totalTM: number;
  task: Task;
  availableSlot: number;
  totalSlot: number;
  runningJob: number;
  tmMemory: number;
  jmMemory: number;
}

interface Task {
  total: number;
  created: number;
  scheduled: number;
  deploying: number;
  running: number;
  finished: number;
  canceling: number;
  canceled: number;
  failed: number;
  reconciling: number;
}
// The list of data
export interface AppListResponse {
  total: string;
  records: SparkApplication[];
}
export interface SparkApplication {
  id?: string;
  teamId?: string;
  jobType?: number;
  appType?: number;
  versionId?: number;
  appName?: string;
  executionMode?: number;
  resourceFrom?: number;
  module?: any;
  mainClass?: string;
  jar?: string;
  jarCheckSum?: string;
  appProperties?: string;
  appArgs?: string;
  appId?: string;
  yarnQueue?: any;

  projectId?: any;
  tags?: any;
  userId?: string;
  jobName?: string;
  jobId?: string;
  clusterId?: string;
  flinkImage?: string;
  k8sNamespace?: string;
  state?: number;
  release?: number;
  build?: boolean;
  restartSize?: number;
  restartCount?: number;
  optionState?: number;
  alertId?: any;
  args?: string;
  options?: string;
  hotParams?: string;
  resolveOrder?: number;
  dynamicProperties?: string;
  tracking?: number;

  startTime?: string;
  endTime?: string;
  duration?: string;
  cpMaxFailureInterval?: any;
  cpFailureRateInterval?: any;
  cpFailureAction?: any;
  totalTM?: any;
  totalSlot?: any;
  availableSlot?: any;
  jmMemory?: number;
  tmMemory?: number;
  totalTask?: number;
  flinkClusterId?: any;
  description?: string;
  createTime?: string;
  optionTime?: string;
  modifyTime?: string;
  k8sRestExposedType?: any;
  k8sPodTemplate?: any;
  k8sJmPodTemplate?: any;
  k8sTmPodTemplate?: any;
  ingressTemplate?: any;
  defaultModeIngress?: any;
  k8sHadoopIntegration?: boolean;
  overview?: any;
  teamResource?: any;
  dependency?: any;
  sqlId?: any;
  flinkSql?: any;
  stateArray?: any;
  jobTypeArray?: any;
  backUp?: boolean;
  restart?: boolean;
  userName?: string;
  nickName?: string;
  config?: any;
  configId?: any;
  flinkVersion?: string;
  confPath?: any;
  format?: any;
  savepointPath?: any;
  restoreOrTriggerSavepoint?: boolean;
  drain?: boolean;
  nativeFormat?: boolean;
  allowNonRestored?: boolean;
  socketId?: any;
  projectName?: any;
  createTimeFrom?: any;
  createTimeTo?: any;
  backUpDescription?: any;
  teamIdList?: any;
  teamName?: string;
  flinkRestUrl?: any;
  buildStatus?: number;
  appControl?: AppControl;
  fsOperator?: any;
  workspace?: any;
  k8sPodTemplates?: {
    empty?: boolean;
  };
  streamParkJob?: boolean;
  hadoopUser?: string;
}

interface AppControl {
  allowStart: boolean;
  allowStop: boolean;
  allowBuild: boolean;
}

// create Params
export interface CreateParams {
  jobType: number;
  executionMode: number;
  versionId: string;
  flinkSql: string;
  appType: number;
  config?: any;
  format?: any;
  jobName: string;
  tags: string;
  args?: any;
  dependency: string;
  options: string;
  cpMaxFailureInterval: number;
  cpFailureRateInterval: number;
  cpFailureAction: number;
  dynamicProperties: string;
  resolveOrder: number;
  restartSize: number;
  alertId: string;
  description: string;
  k8sNamespace?: any;
  clusterId: string;
  flinkClusterId: string;
  flinkImage?: any;
}
