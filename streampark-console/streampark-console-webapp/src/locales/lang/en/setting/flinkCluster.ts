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
  title: 'Flink Cluster',
  detail: 'View Cluster Detail',
  stop: 'Stop Cluster',
  start: 'Start Cluster',
  edit: 'Edit Cluster',
  delete: 'Are you sure delete this cluster ?',
  searchByName: 'Search by cluster name',
  form: {
    clusterName: 'Cluster Name',
    address: 'Cluster URL',
    runState: 'Run State',
    internal: 'internal cluster',
    deployMode: 'Deploy Mode',
    versionId: 'Flink Version',
    addExisting: 'existing cluster',
    addNew: 'new cluster',
    yarnQueue: 'Yarn Queue',
    yarnSessionClusterId: 'Yarn Session Cluster',
    k8sNamespace: 'Kubernetes Namespace',
    k8sClusterId: 'Kubernetes ClusterId',
    serviceAccount: 'Service Account',
    k8sConf: 'Kube Conf File',
    flinkImage: 'Flink Base Docker Image',
    k8sRestExposedType: 'Rest-Service Exposed Type',
    resolveOrder: 'Resolve Order',
    taskSlots: 'Task Slots',
    totalOptions: 'Total Memory Options',
    jmOptions: 'JM Memory Options',
    tmOptions: 'TM Memory Options',
    dynamicProperties: 'Dynamic Properties',
    clusterDescription: 'Description',
  },
  placeholder: {
    addType: 'Please select cluster Add Type',
    clusterName: 'Please enter cluster name',
    deployMode: 'Please enter deploy mode',
    versionId: 'Please select flink version',
    addressRemoteMode: 'Please enter jobManager URL',
    addressNoRemoteMode: 'Please enter cluster address,  e.g: http://host:port',
    yarnSessionClusterId: 'Please enter Yarn Session cluster',
    k8sConf: '~/.kube/config',
    flinkImage:
      'Please enter the tag of Flink base docker image, such as: flink:1.13.0-scala_2.11-java8',
    k8sRestExposedType: 'kubernetes.rest-service.exposed.type',
    resolveOrder: 'classloader.resolve-order',
    taskSlots: 'Number of slots per TaskManager',
    jmOptions: 'Please select the resource parameters to set',
    tmOptions: 'Please select the resource parameters to set',
    clusterDescription: 'Please enter description for this application',
  },
  required: {
    address: 'Cluster address is required',
    deployMode: 'Deploy Mode is required',
    clusterId: 'Yarn Session Cluster is required',
    versionId: 'Flink Version is required',
    flinkImage: 'Flink Base Docker Image is required',
    resolveOrder: 'Resolve Order is required',
  },
  operateMessage: {
    createFlinkSessionClusterSuccessful: ' create successful!',
    createFlinkSessionClusterFailed: 'create cluster failed, please check log',
    hadoopEnvInitializationFailed:
      'Hadoop environment initialization failed, please check the environment settings',
    flinkClusterIsStarting: 'The current cluster is starting',
    flinkClusterHasStartedSuccessful: 'The current cluster is started',
    updateFlinkClusterSuccessful: ' update successful!',
  },
  view: {
    clusterId: 'ClusterId',
  },
};
