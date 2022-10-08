export interface FlinkCluster {
  id: string;
  address: string;
  clusterId: string;
  clusterName: string;
  executionMode: number;
  versionId: string;
  k8sNamespace: string;
  serviceAccount?: any;
  description: string;
  userId: string;
  flinkImage?: any;
  options: string;
  yarnQueue: string;
  k8sHadoopIntegration: boolean;
  dynamicOptions: string;
  k8sRestExposedType: number;
  flameGraph: boolean;
  k8sConf?: any;
  resolveOrder: number;
  exception?: any;
  clusterState: number;
  createTime: string;
  executionModeEnum: string;
  clusterStateEnum: string;
}
