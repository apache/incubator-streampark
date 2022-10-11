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
import { FormSchema } from '/@/components/Form/src/types/form';
import { h, onMounted, reactive, ref, unref, toRaw } from 'vue';
import optionData from '../data/option';
import { fetchFlinkCluster } from '/@/api/flink/setting/flinkCluster';
import { FlinkCluster } from '/@/api/flink/setting/types/flinkCluster.type';
import {
  fetchFlinkBaseImages,
  fetchK8sNamespaces,
  fetchSessionClusterIds,
} from '/@/api/flink/app/flinkHistory';
import { k8sRestExposedType, resolveOrder } from '../data';
import { fetchCheckName } from '/@/api/flink/app/app';
import { fetchAlertSetting } from '/@/api/flink/setting/alert';

import { AlertSetting } from '/@/api/flink/setting/types/alert.type';
import {
  renderDynamicOption,
  renderInputDropdown,
  renderInputGroup,
  renderTotalMemory,
  renderOptionsItems,
  getAlertSvgIcon,
  renderIsSetConfig,
} from './useFlinkRender';
import Icon from '/@/components/Icon';
import { Alert } from 'ant-design-vue';
import { useDrawer } from '/@/components/Drawer';
import { fetchSelect } from '/@/api/flink/project';

export const useFlinkSchema = (editModel?: string) => {
  const [registerConfDrawer, { openDrawer: openConfDrawer }] = useDrawer();
  const getExecutionCluster = (executionMode) => {
    if (editModel) {
      return (toRaw(unref(flinkClusters)) || []).filter(
        (o) => o.executionMode === executionMode && o.clusterState === 1,
      );
    } else {
      return (toRaw(unref(flinkClusters)) || []).filter((o) => o.executionMode === executionMode);
    }
  };

  const getFlinkClusterSchemas = (): FormSchema[] => {
    return [
      {
        field: 'versionId',
        label: 'Flink Version',
        component: 'Select',
        rules: [{ required: true, message: 'Flink Version is required' }],
      },
      {
        field: 'flinkClusterId',
        label: 'Flink Cluster',
        component: 'Select',
        componentProps: {
          placeholder: 'Flink Cluster',
          options: (getExecutionCluster(1) || []).map((i) => ({
            label: i.clusterName,
            value: i.id,
          })),
        },
        ifShow: ({ values }) => values.executionMode == 1,
        rules: [{ required: true, message: 'Flink Cluster is required' }],
      },
      {
        field: 'yarnSessionClusterId',
        label: 'Yarn Session ClusterId',
        component: 'Select',
        componentProps: () => {
          const options = getExecutionCluster(3);
          return {
            placeholder: 'Please enter Yarn Session clusterId',
            options: options.map((i) => ({ label: i.clusterName, value: i.clusterId })),
          };
        },
        ifShow: ({ values }) => values.executionMode == 3,
        rules: [{ required: true, message: 'Flink Cluster is required' }],
      },
      {
        field: 'k8sNamespace',
        label: 'Kubernetes Namespace',
        component: 'Input',
        ifShow: ({ values }) => [5, 6].includes(values.executionMode),
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: 'default',
            options: historyRecord.k8sNamespace,
          }),
      },
      {
        field: 'clusterId',
        label: 'Kubernetes ClusterId',
        component: 'Input',
        componentProps: ({ formModel }) => {
          return {
            placeholder: 'Please enter Kubernetes clusterId',
            onChange: (e) => (formModel.jobName = e.target.value),
          };
        },
        ifShow: ({ values }) => values.executionMode == 6,
        rules: [{ required: true, message: 'Kubernetes clusterId is required' }],
      },
      {
        field: 'clusterId',
        label: 'Kubernetes ClusterId',
        component: 'Select',
        ifShow: ({ values }) => values.executionMode == 5,
        componentProps: {
          placeholder: 'Please enter Kubernetes clusterId',
          options: (getExecutionCluster(5) || []).map((i) => ({
            label: i.clusterName,
            value: i.clusterId,
          })),
        },
        rules: [{ required: true, message: 'Kubernetes clusterId is required' }],
      },
      {
        field: 'flinkImage',
        label: 'Flink Base Docker Image',
        component: 'Input',
        ifShow: ({ values }) => values.executionMode == 6,
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder:
              'Please enter the tag of Flink base docker image, such as: flink:1.13.0-scala_2.11-java8',
            options: historyRecord.k8sSessionClusterId,
          }),
        rules: [{ required: true, message: 'Flink Base Docker Image is required' }],
      },
      {
        field: 'k8sRestExposedType',
        label: 'Rest-Service Exposed Type',
        ifShow: ({ values }) => values.executionMode == 6,
        component: 'Select',
        componentProps: {
          placeholder: 'kubernetes.rest-service.exposed.type',
          options: k8sRestExposedType,
        },
      },
    ];
  };

  const getFlinkFormOtherSchemas = (appId?: string): FormSchema[] => [
    {
      field: 'jobName',
      label: 'Application Name',
      component: 'Input',
      componentProps: {
        placeholder: 'Please enter jobName',
      },
      dynamicRules: () => {
        return [
          {
            required: true,
            trigger: 'blur',
            validator: async (_rule, value) => {
              if (value === null || value === undefined || value === '') {
                return Promise.reject('Application Name is required');
              } else {
                const params = { jobName: value };
                if (appId) Object.assign(params, { id: appId });
                const res = await fetchCheckName(params);
                switch (parseInt(res)) {
                  case 0:
                    return Promise.resolve();
                  case 1:
                    return Promise.reject(
                      'application name must be unique. The application name already exists',
                    );
                  case 2:
                    return Promise.reject(
                      'The application name is already running in yarn,cannot be repeated. Please check',
                    );
                  case 3:
                    return Promise.reject(
                      'The application name is already running in k8s,cannot be repeated. Please check',
                    );
                  default:
                    return Promise.reject(
                      'The application name is invalid.characters must be (Chinese|English|"-"|"_"),two consecutive spaces cannot appear.Please check',
                    );
                }
              }
            },
          },
        ];
      },
    },
    {
      field: 'tags',
      label: 'Tags',
      component: 'Input',
      componentProps: {
        placeholder: 'Please enter tags,if more than one, separate them with commas(,)',
      },
    },
    {
      field: 'resolveOrder',
      label: 'Resolve Order',
      component: 'Select',
      componentProps: {
        placeholder: 'classloader.resolve-order',
        options: resolveOrder,
      },
      rules: [{ required: true, message: 'Resolve Order is required', type: 'number' }],
    },
    {
      field: 'parallelism',
      label: 'Parallelism',
      component: 'InputNumber',
      componentProps: {
        placeholder: 'The parallelism with which to run the program',
        min: 1,
        step: 1,
        class: '!w-full',
      },
    },
    {
      field: 'slot',
      label: 'Task Slots',
      component: 'InputNumber',
      componentProps: {
        placeholder: 'Number of slots per TaskManager',
        min: 1,
        step: 1,
        class: '!w-full',
      },
    },
    {
      field: 'restartSize',
      label: 'Fault Restart Size',
      ifShow: ({ values }) =>
        editModel == 'flink' ? true : ![5, 6].includes(values.executionMode),
      component: 'InputNumber',
      componentProps: {
        placeholder: 'restart max size',
        min: 1,
        step: 1,
        class: '!w-full',
      },
    },
    {
      field: 'cpMaxFailureInterval',
      label: 'CheckPoint Failure Options',
      component: 'InputNumber',
      render: ({ model }) => renderInputGroup(model),
      show: ({ values }) => (editModel == 'flink' ? true : ![5, 6].includes(values.executionMode)),
    },
    {
      field: 'alertId',
      label: 'Fault Alert Template',
      component: 'Select',
    },
    ...getConfigSchemas(),
    {
      field: 'totalOptions',
      label: 'Total Memory Options',
      component: 'Select',
      render: (renderCallbackParams) => renderTotalMemory(renderCallbackParams),
    },
    {
      field: 'totalItem',
      label: 'totalItem',
      component: 'Select',
      renderColContent: ({ model }) => renderOptionsItems(model, 'totalOptions', '.memory'),
    },
    {
      field: 'jmOptions',
      label: 'JM Memory Options',
      component: 'Select',
      componentProps: {
        showSearch: true,
        allowClear: true,
        mode: 'multiple',
        maxTagCount: 2,
        placeholder: 'Please select the resource parameters to set',
        fieldNames: { label: 'name', value: 'key', options: 'options' },
        options: optionData.filter((x) => x.group === 'jobmanager-memory'),
      },
    },
    {
      field: 'jmOptionsItem',
      label: 'jmOptionsItem',
      component: 'Select',
      renderColContent: ({ model }) => renderOptionsItems(model, 'jmOptions', 'jobmanager.memory.'),
    },
    {
      field: 'tmOptions',
      label: 'TM Memory Options',
      component: 'Select',
      componentProps: {
        showSearch: true,
        allowClear: true,
        mode: 'multiple',
        maxTagCount: 2,
        placeholder: 'Please select the resource parameters to set',
        fieldNames: { label: 'name', value: 'key', options: 'options' },
        options: optionData.filter((x) => x.group === 'taskmanager-memory'),
      },
    },
    {
      field: 'tmOptionsItem',
      label: 'tmOptionsItem',
      component: 'Select',
      renderColContent: ({ model }) =>
        renderOptionsItems(model, 'tmOptions', 'taskmanager.memory.'),
    },
    {
      field: 'yarnQueue',
      label: 'Yarn Queue',
      component: 'Input',
      componentProps: {
        placeholder: 'Please enter yarn queue',
      },
      ifShow: ({ values }) => values.executionMode == 4,
    },
    {
      field: 'podTemplate',
      label: 'Kubernetes Pod Template',
      component: 'Input',
      slot: 'podTemplate',
      ifShow: ({ values }) => values.executionMode == 6,
    },
    {
      field: 'dynamicOptions',
      label: 'Dynamic Option',
      component: 'Input',
      render: (renderCallbackParams) => renderDynamicOption(renderCallbackParams),
    },
    {
      field: 'args',
      label: 'Program Args',
      component: 'InputTextArea',
      componentProps: {
        rows: 4,
        placeholder: '<arguments>',
      },
      ifShow: ({ values }) => (editModel ? true : values.jobType == 'customcode'),
    },
    {
      field: 'description',
      label: 'Description',
      component: 'InputTextArea',
      componentProps: {
        rows: 4,
        placeholder: 'Please enter description for this application',
      },
    },
  ];

  /* flink type data */
  const getFlinkTypeSchema = (): FormSchema[] => {
    return [
      {
        field: 'jobType',
        label: 'Development Mode',
        component: 'Input',
        render: ({ model }) => {
          if (model.jobType === 1) {
            return h(
              Alert,
              { type: 'info' },
              {
                message: () => [
                  h(Icon, { icon: 'ant-design:code-outlined', style: { color: '#108ee9' } }),
                  h('span', { class: 'pl-5px' }, 'Custom Code'),
                ],
              },
            );
          } else {
            return getAlertSvgIcon('fql', 'Flink SQL');
          }
        },
      },
      {
        field: 'appType',
        label: 'Application Type',
        component: 'Input',
        render: () => getAlertSvgIcon('flink', 'StreamPark Flink'),
      },
    ];
  };
  /**
   *
   * @param appId Edit state passes app data, create does not pass
   * @returns
   */
  const getFlinkSqlSchema = (appId?: string): FormSchema[] => [
    {
      field: 'flinkSql',
      label: 'Flink SQL',
      component: 'Input',
      slot: 'flinkSql',
      ifShow: ({ values }) => {
        if (appId) {
          return values?.jobType == 2;
        } else {
          return values?.jobType == 'sql';
        }
      },
      rules: [{ required: true, message: 'Flink SQL is required' }],
    },
    {
      field: 'dependency',
      label: 'Dependency',
      component: 'Input',
      slot: 'dependency',
      ifShow: ({ values }) => {
        if (appId) {
          return values.jobType == 2;
        } else {
          return values?.jobType == 'sql';
        }
      },
    },
    {
      field: 'isSetConfig',
      label: 'Application Conf',
      component: 'Switch',
      ifShow: ({ values }) => {
        if (appId) {
          return values?.jobType == 2 && ![5, 6].includes(values.executionMode);
        } else {
          return values?.jobType == 'sql' && ![5, 6].includes(values.executionMode);
        }
      },
      render: ({ model, field }) =>
        renderIsSetConfig(model, field, registerConfDrawer, openConfDrawer),
    },
  ];

  const flinkClusters = ref<FlinkCluster[]>([]);
  const alerts = ref<AlertSetting[]>([]);
  const projectList = ref<any[]>([]);
  const historyRecord = reactive<Record<string, Array<string>>>({
    uploadJars: [],
    k8sNamespace: [],
    k8sSessionClusterId: [],
    flinkImage: [],
    podTemplate: [],
    jmPodTemplate: [],
    tmPodTemplate: [],
  });

  /* 
  !The original item is also unassigned
  */
  function getConfigSchemas() {
    return [];
  }

  onMounted(async () => {
    /* Get project data */
    projectList.value = await fetchSelect({});
    /* Get alarm data */
    alerts.value = await fetchAlertSetting();
    const cluster = await fetchFlinkCluster();
    flinkClusters.value = cluster;
    historyRecord.k8sNamespace = await fetchK8sNamespaces();
    historyRecord.k8sSessionClusterId = await fetchSessionClusterIds({ executionMode: 5 });
    historyRecord.flinkImage = await fetchFlinkBaseImages();
  });
  return {
    getFlinkClusterSchemas,
    getFlinkFormOtherSchemas,
    getFlinkTypeSchema,
    getFlinkSqlSchema,
    flinkClusters,
    projectList,
    alerts,
    historyRecord,
  };
};
