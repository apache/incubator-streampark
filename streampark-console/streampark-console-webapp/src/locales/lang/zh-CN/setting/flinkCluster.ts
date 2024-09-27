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
export default {
  title: 'Flink 集群',
  detail: '查看集群详情',
  stop: '停止集群',
  start: '开启集群',
  edit: '编辑集群',
  delete: '确定要删除此集群 ?',
  searchByName: '根据 Flink 集群名称搜索',
  form: {
    clusterName: '集群名称',
    address: '集群URL',
    runState: '运行状态',
    deployMode: '部署模式',
    versionId: 'Flink版本',
    addType: '添加类型',
    addExisting: '已有集群',
    addNew: '全新集群',
    yarnQueue: 'Yarn队列',
    yarnSessionClusterId: 'Yarn Session模式集群ID',
    k8sNamespace: 'Kubernetes 命名空间',
    k8sClusterId: 'Kubernetes 集群 ID',
    serviceAccount: 'Kubernetes  服务账号',
    k8sConf: 'Kube 配置文件',
    flinkImage: 'Flink 基础 Docker 镜像',
    k8sRestExposedType: 'Kubernetes Rest exposed-type',
    resolveOrder: '类加载顺序',
    taskSlots: '任务槽数',
    jmOptions: 'JM内存',
    tmOptions: 'TM内存',
    dynamicProperties: '动态参数',
    clusterDescription: '集群描述',
  },
  placeholder: {
    addType: '请选择集群添加类型',
    clusterName: '请输入集群名称',
    deployMode: '请选择部署模式',
    versionId: '请选择Flink版本',
    addressRemoteMode: '请输入Flink 集群JobManager URL访问地址',
    addressNoRemoteMode: '请输入集群地址，例如：http://host:port',
    yarnSessionClusterId: '请输入Yarn Session模式集群ID',
    k8sConf: '示例：~/.kube/config',
    flinkImage: '请输入 Flink 基础 docker 镜像的标签，如：flink:1.13.0-scala_2.11-java8',
    k8sRestExposedType: 'kubernetes.rest-service.exposed.type',
    resolveOrder: 'classloader.resolve-order',
    taskSlots: '每个TaskManager的插槽数',
    totalOptions: '总内存',
    jmOptions: '请选择要设置的jm资源参数',
    tmOptions: '请选择要设置的tm资源参数',
    clusterDescription: '集群描述信息, 如: 生产flink 1.16集群',
  },
  required: {
    address: '必须填写集群地址',
    deployMode: '部署模式必填',
    clusterId: 'Yarn Session Cluster 为必填项',
    versionId: 'Flink 版本必选',
    flinkImage: 'link基础docker镜像是必填的',
    resolveOrder: '类加载顺序必选',
  },
  operateMessage: {
    createFlinkSessionClusterSuccessful: ' 创建成功!',
    createFlinkSessionClusterFailed: 'session集群创建失败, 请检查日志',
    hadoopEnvInitializationFailed: 'Hadoop环境初始化失败，请检查环境设置',
    flinkClusterIsStarting: '当前集群正在启动',
    flinkClusterHasStartedSuccessful: '当前集群已成功启动',
    updateFlinkClusterSuccessful: ' 更新成功!',
  },
  view: {
    clusterId: '集群ID',
  },
};
