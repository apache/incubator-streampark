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
import { FormSchema } from '/@/components/Table';
import { computed, ref, unref, h, Ref, onMounted, reactive } from 'vue';
import { k8sRestExposedType, resolveOrder } from '../data';
import optionData from '../data/option';
import {
  getAlertSvgIcon,
  renderProperties,
  renderInputDropdown,
  renderInputGroup,
  renderIsSetConfig,
  renderOptionsItems,
  renderTotalMemory,
} from './useFlinkRender';

import { fetchCheckName } from '/@/api/flink/app/app';
import { RuleObject } from 'ant-design-vue/lib/form';
import { StoreValue } from 'ant-design-vue/lib/form/interface';
import { useDrawer } from '/@/components/Drawer';
import { Alert } from 'ant-design-vue';
import Icon from '/@/components/Icon';
import { useMessage } from '/@/hooks/web/useMessage';
import { fetchVariableAll } from '/@/api/system/variable';
import {
  fetchFlinkBaseImages,
  fetchK8sNamespaces,
  fetchSessionClusterIds,
} from '/@/api/flink/app/flinkHistory';
import { fetchSelect } from '/@/api/flink/project';
import { fetchAlertSetting } from '/@/api/flink/setting/alert';
import { fetchFlinkCluster } from '/@/api/flink/setting/flinkCluster';
import { fetchFlinkEnv } from '/@/api/flink/setting/flinkEnv';
import { FlinkEnv } from '/@/api/flink/setting/types/flinkEnv.type';
import { AlertSetting } from '/@/api/flink/setting/types/alert.type';
import { FlinkCluster } from '/@/api/flink/setting/types/flinkCluster.type';
import { ExecModeEnum } from '/@/enums/flinkEnum';
import { isK8sExecMode } from '../utils';
export interface HistoryRecord {
  k8sNamespace: Array<string>;
  k8sSessionClusterId: Array<string>;
  flinkImage: Array<string>;
}
export const useCreateAndEditSchema = (
  dependencyRef: Ref | null,
  edit?: { appId: string; mode: 'streampark' | 'flink' },
) => {
  const flinkEnvs = ref<FlinkEnv[]>([]);
  const alerts = ref<AlertSetting[]>([]);
  const flinkClusters = ref<FlinkCluster[]>([]);
  const projectList = ref<Array<any>>([]);
  const historyRecord = reactive<HistoryRecord>({
    k8sNamespace: [],
    k8sSessionClusterId: [],
    flinkImage: [],
  });

  const { createErrorModal } = useMessage();
  let scalaVersion = '';
  const suggestions = ref<Array<{ text: string; description: string; value: string }>>([]);

  const [registerConfDrawer, { openDrawer: openConfDrawer }] = useDrawer();

  /* 
  !The original item is also unassigned
  */
  function getConfigSchemas() {
    return [];
  }

  /* filter cluster */
  const getExecutionCluster = (
    executionMode: number,
    valueKey: string,
  ): Array<{ label: string; value: string }> => {
    return (unref(flinkClusters) || [])
      .filter((o) => {
        // Edit mode has one more filter condition
        if (edit?.mode) {
          return o.executionMode == executionMode && o.clusterState === 1;
        } else {
          return o.executionMode == executionMode;
        }
      })
      .map((i) => ({ label: i.clusterName, value: i[valueKey] }));
  };

  const getFlinkSqlSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'flinkSql',
        label: 'Flink SQL',
        component: 'Input',
        slot: 'flinkSql',
        ifShow: ({ values }) => {
          if (edit?.appId) {
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
          if (edit?.appId) {
            return values.jobType == 2;
          } else {
            return values?.jobType == 'sql';
          }
        },
      },
      { field: 'configOverride', label: '', component: 'Input', show: false },
      {
        field: 'isSetConfig',
        label: 'Application Conf',
        component: 'Switch',
        ifShow: ({ values }) => {
          if (edit?.appId) {
            return values?.jobType == 2 && !isK8sExecMode(values.executionMode);
          } else {
            return values?.jobType == 'sql' && !isK8sExecMode(values.executionMode);
          }
        },
        render({ model, field }) {
          return renderIsSetConfig(model, field, registerConfDrawer, openConfDrawer);
        },
      },
    ];
  });

  function handleFlinkVersion(id: number | string) {
    if (!dependencyRef) return;
    scalaVersion = unref(flinkEnvs)?.find((v) => v.id === id)?.scalaVersion || '';
    checkPomScalaVersion();
  }

  function checkPomScalaVersion() {
    const pom = unref(dependencyRef)?.dependencyRecords;
    if (pom && pom.length > 0) {
      const invalidArtifact: Array<any> = [];
      pom.forEach((v: Recordable) => {
        const artifactId = v.artifactId;
        if (/flink-(.*)_(.*)/.test(artifactId)) {
          const depScalaVersion = artifactId.substring(artifactId.lastIndexOf('_') + 1);
          if (scalaVersion !== depScalaVersion) {
            invalidArtifact.push(artifactId);
          }
        }
      });
      if (invalidArtifact.length > 0) {
        alertInvalidDependency(scalaVersion, invalidArtifact);
      }
    }
  }

  function alertInvalidDependency(scalaVersion: string, invalidArtifact: string[]) {
    let depCode = '';
    invalidArtifact.forEach((dep) => {
      depCode += `<div style="font-size: 1rem;line-height: 1rem;padding-bottom: 0.3rem">${dep}</div>`;
    });
    createErrorModal({
      title: 'Dependencies invalid',
      width: 500,
      content: `
      <div class="text-left;">
       <div style="padding:0.5em;font-size: 1rem">
       current flink scala version: <strong>${scalaVersion}</strong>,some dependencies scala version is invalid,dependencies list:
       </div>
       <div style="color: red;font-size: 1em;padding:0.5em;">
         ${depCode}
       </div>
      </div>`,
    });
  }
  const getFlinkClusterSchemas = computed((): FormSchema[] => {
    return [
      {
        field: 'versionId',
        label: 'Flink Version',
        component: 'Select',
        componentProps: {
          placeholder: 'Flink Version',
          options: unref(flinkEnvs),
          fieldNames: { label: 'flinkName', value: 'id', options: 'options' },
          onChange: (value) => handleFlinkVersion(value),
        },
        rules: [{ required: true, message: 'Flink Version is required' }],
      },
      {
        field: 'flinkClusterId',
        label: 'Flink Cluster',
        component: 'Select',
        componentProps: {
          placeholder: 'Flink Cluster',
          options: getExecutionCluster(1, 'id'),
        },
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.REMOTE,
        rules: [{ required: true, message: 'Flink Cluster is required' }],
      },
      {
        field: 'yarnSessionClusterId',
        label: 'Yarn Session ClusterId',
        component: 'Select',
        componentProps: () => {
          return {
            placeholder: 'Please enter Yarn Session clusterId',
            options: getExecutionCluster(3, 'clusterId'),
          };
        },
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.YARN_SESSION,
        rules: [{ required: true, message: 'Flink Cluster is required' }],
      },
      {
        field: 'k8sNamespace',
        label: 'Kubernetes Namespace',
        component: 'Input',
        ifShow: ({ values }) => isK8sExecMode(values.executionMode),
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: 'default',
            options: unref(historyRecord)?.k8sNamespace || [],
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
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
        rules: [{ required: true, message: 'Kubernetes clusterId is required' }],
      },
      {
        field: 'clusterId',
        label: 'Kubernetes ClusterId',
        component: 'Select',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        componentProps: {
          placeholder: 'Please enter Kubernetes clusterId',
          options: getExecutionCluster(5, 'clusterId'),
        },
        rules: [{ required: true, message: 'Kubernetes clusterId is required' }],
      },
      {
        field: 'flinkImage',
        label: 'Flink Base Docker Image',
        component: 'Input',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder:
              'Please enter the tag of Flink base docker image, such as: flink:1.13.0-scala_2.11-java8',
            options: unref(historyRecord)?.k8sSessionClusterId || [],
          }),
        rules: [{ required: true, message: 'Flink Base Docker Image is required' }],
      },
      {
        field: 'k8sRestExposedType',
        label: 'Rest-Service Exposed Type',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
        component: 'Select',
        componentProps: {
          placeholder: 'kubernetes.rest-service.exposed.type',
          options: k8sRestExposedType,
        },
      },
    ];
  });

  /* Detect job name field */
  async function getJobNameCheck(_rule: RuleObject, value: StoreValue) {
    if (value === null || value === undefined || value === '') {
      return Promise.reject('Application Name is required');
    } else {
      const params = { jobName: value };
      if (edit?.appId) Object.assign(params, { id: edit.appId });
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
  }

  const getFlinkFormOtherSchemas = computed((): FormSchema[] => {
    const commonInputNum = {
      min: 1,
      step: 1,
      class: '!w-full',
    };
    return [
      {
        field: 'jobName',
        label: 'Application Name',
        component: 'Input',
        componentProps: { placeholder: 'Please enter jobName' },
        dynamicRules: () => {
          return [{ required: true, trigger: 'blur', validator: getJobNameCheck }];
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
        componentProps: { placeholder: 'classloader.resolve-order', options: resolveOrder },
        rules: [{ required: true, message: 'Resolve Order is required', type: 'number' }],
      },
      {
        field: 'parallelism',
        label: 'Parallelism',
        component: 'InputNumber',
        componentProps: {
          placeholder: 'The parallelism with which to run the program',
          ...commonInputNum,
        },
      },
      {
        field: 'slot',
        label: 'Task Slots',
        component: 'InputNumber',
        componentProps: { placeholder: 'Number of slots per TaskManager', ...commonInputNum },
      },
      {
        field: 'restartSize',
        label: 'Fault Restart Size',
        ifShow: ({ values }) =>
          edit?.mode == 'flink' ? true : !isK8sExecMode(values.executionMode),
        component: 'InputNumber',
        componentProps: { placeholder: 'restart max size', ...commonInputNum },
      },
      {
        field: 'alertId',
        label: 'Fault Alert Template',
        component: 'Select',
        componentProps: {
          placeholder: 'Alert Template',
          options: unref(alerts),
          fieldNames: { label: 'alertName', value: 'id', options: 'options' },
        },
      },
      {
        field: 'checkPointFailure',
        label: 'CheckPoint Failure Options',
        component: 'InputNumber',
        renderColContent: renderInputGroup,
        show: ({ values }) => (edit?.mode == 'flink' ? true : !isK8sExecMode(values.executionMode)),
      },
      ...getConfigSchemas(),
      {
        field: 'totalOptions',
        label: 'Total Memory Options',
        component: 'Select',
        render: renderTotalMemory,
      },
      {
        field: 'totalItem',
        label: 'totalItem',
        component: 'Select',
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'totalOptions', field, '.memory', true),
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
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'jmOptions', field, 'jobmanager.memory.'),
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
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'tmOptions', field, 'taskmanager.memory.'),
      },
      {
        field: 'yarnQueue',
        label: 'Yarn Queue',
        component: 'Input',
        componentProps: { placeholder: 'Please enter yarn queue' },
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.YARN_APPLICATION,
      },
      {
        field: 'podTemplate',
        label: 'Kubernetes Pod Template',
        component: 'Input',
        slot: 'podTemplate',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
      },
      {
        field: 'properties',
        label: 'Properties',
        component: 'Input',
        render: (renderCallbackParams) => renderProperties(renderCallbackParams),
      },
      {
        field: 'args',
        label: 'Program Args',
        component: 'InputTextArea',
        defaultValue: '',
        slot: 'args',
        ifShow: ({ values }) => (edit?.mode ? true : values.jobType == 'customcode'),
      },
      {
        field: 'description',
        label: 'Description',
        component: 'InputTextArea',
        componentProps: { rows: 4, placeholder: 'Please enter description for this application' },
      },
    ];
  });

  const getFlinkTypeSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'jobType',
        label: 'Development Mode',
        component: 'Input',
        render: ({ model }) => {
          if (model.jobType == 1) {
            return h(
              Alert,
              { type: 'info' },
              {
                message: () => [
                  h(Icon, {
                    icon: 'ant-design:code-outlined',
                    style: { color: '#108ee9' },
                  }),
                  h('span', { class: 'pl-8px' }, 'Custom Code'),
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
  });

  onMounted(async () => {
    /* Get project data */
    fetchSelect({}).then((res) => {
      projectList.value = res;
    });

    /* Get alert data */
    fetchAlertSetting().then((res) => {
      alerts.value = res;
    });

    //get flinkEnv
    fetchFlinkEnv().then((res) => {
      flinkEnvs.value = res;
    });
    //get flinkCluster
    fetchFlinkCluster().then((res) => {
      flinkClusters.value = res;
    });
    fetchK8sNamespaces().then((res) => {
      historyRecord.k8sNamespace = res;
    });
    fetchSessionClusterIds({ executionMode: ExecModeEnum.KUBERNETES_SESSION }).then((res) => {
      historyRecord.k8sSessionClusterId = res;
    });
    fetchFlinkBaseImages().then((res) => {
      historyRecord.flinkImage = res;
    });

    fetchVariableAll().then((res) => {
      suggestions.value = res.map((v) => {
        return {
          text: v.variableCode,
          description: v.description,
          value: v.variableValue,
        };
      });
    });
  });
  return {
    projectList,
    alerts,
    flinkEnvs,
    flinkClusters,
    historyRecord,
    suggestions,
    getFlinkSqlSchema,
    getFlinkClusterSchemas,
    getFlinkFormOtherSchemas,
    getFlinkTypeSchema,
    openConfDrawer,
  };
};
