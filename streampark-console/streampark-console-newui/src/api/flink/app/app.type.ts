// 仪表盘
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
// 列表数据
export interface AppListResponse {
  total: string;
  records: AppListRecord[];
}
export interface AppListRecord {
  id: string;
  jobType: number;
  projectId?: any;
  tags?: any;
  userId: string;
  teamId: string;
  jobName: string;
  appId?: string;
  jobId?: string;
  versionId: string;
  clusterId?: string;
  flinkImage?: string;
  k8sNamespace: string;
  state: number;
  launch: number;
  build: boolean;
  restartSize: number;
  restartCount?: number;
  optionState: number;
  alertId?: any;
  args?: string;
  module?: any;
  options: string;
  hotParams?: string;
  resolveOrder: number;
  executionMode: number;
  dynamicOptions?: string;
  appType: number;
  flameGraph: boolean;
  tracking: number;
  jar?: string;
  jarCheckSum?: string;
  mainClass?: string;
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
  createTime: string;
  optionTime?: string;
  modifyTime: string;
  k8sRestExposedType?: any;
  k8sPodTemplate?: any;
  k8sJmPodTemplate?: any;
  k8sTmPodTemplate?: any;
  ingressTemplate?: any;
  defaultModeIngress?: any;
  resourceFrom?: number;
  k8sHadoopIntegration: boolean;
  overview?: any;
  dependency?: any;
  sqlId?: any;
  flinkSql?: any;
  stateArray?: any;
  jobTypeArray?: any;
  backUp: boolean;
  restart: boolean;
  userName: string;
  nickName: string;
  config?: any;
  configId?: any;
  flinkVersion: string;
  confPath?: any;
  format?: any;
  savePoint?: any;
  savePointed: boolean;
  drain: boolean;
  allowNonRestored: boolean;
  socketId?: any;
  projectName?: any;
  createTimeFrom?: any;
  createTimeTo?: any;
  backUpDescription?: any;
  yarnQueue?: any;
  yarnSessionClusterId?: any;
  teamIdList?: any;
  teamName: string;
  flinkRestUrl?: any;
  buildStatus: number;
  appControl: AppControl;
  fsOperator: any;
  workspace: any;
  k8sPodTemplates: {
    empty: boolean;
  };
  streamXJob: boolean;
}

interface AppControl {
  allowStart: boolean;
  allowStop: boolean;
  allowBuild: boolean;
}
/* cancel params */
export interface CancelParam {
  id: string;
  savePointed: boolean;
  drain: boolean;
  savePoint: string;
}
