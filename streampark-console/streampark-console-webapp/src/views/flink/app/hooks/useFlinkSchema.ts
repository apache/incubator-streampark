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
import { ExecModeEnum, ClusterStateEnum } from '/@/enums/flinkEnum';

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
  renderDynamicProperties,
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
import { isK8sExecMode } from '../utils';
import { useI18n } from '/@/hooks/web/useI18n';
const { t } = useI18n();
export const useFlinkSchema = (editModel?: string) => {
  const [registerConfDrawer, { openDrawer: openConfDrawer }] = useDrawer();
  const getExecutionCluster = (executionMode) => {
    if (editModel) {
      return (toRaw(unref(flinkClusters)) || []).filter(
        (o) => o.executionMode == executionMode && o.clusterState === ClusterStateEnum.STARTED,
      );
    } else {
      return (toRaw(unref(flinkClusters)) || []).filter((o) => o.executionMode == executionMode);
    }
  };

  const getFlinkClusterSchemas = (): FormSchema[] => {
    return [
      {
        field: 'versionId',
        label: t('flink.app.flinkVersion'),
        component: 'Select',
        componentProps: {
          placeholder: t('flink.app.flinkVersion'),
        },
        rules: [{ required: true, message: 'Flink Version is required' }],
      },
      {
        field: 'flinkClusterId',
        label: t('flink.app.flinkCluster'),
        component: 'Select',
        componentProps: () => {
          const options = getExecutionCluster(ExecModeEnum.REMOTE);
          return {
            placeholder: t('flink.app.flinkCluster'),
            options: options.map((i) => ({ label: i.clusterName, value: i.id })),
          };
        },
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.REMOTE,
        rules: [
          { required: true, message: t('flink.app.addAppTips.flinkClusterIsRequiredMessage') },
        ],
      },
      {
        field: 'flinkClusterId',
        label: t('flink.app.flinkCluster'),
        component: 'Select',
        componentProps: () => {
          const options = getExecutionCluster(ExecModeEnum.YARN_SESSION);
          return {
            placeholder: t('flink.app.flinkCluster'),
            options: options.map((i) => ({ label: i.clusterName, value: i.id })),
          };
        },
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.YARN_SESSION,
        rules: [
          { required: true, message: t('flink.app.addAppTips.flinkClusterIsRequiredMessage') },
        ],
      },
      {
        field: 'k8sNamespace',
        label: t('flink.app.kubernetesNamespace'),
        component: 'Input',
        ifShow: ({ values }) => isK8sExecMode(values.executionMode),
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: t('flink.app.addAppTips.kubernetesNamespacePlaceholder'),
            options: historyRecord.k8sNamespace,
          }),
      },
      {
        field: 'clusterId',
        label: t('flink.app.kubernetesClusterId'),
        component: 'Input',
        componentProps: ({ formModel }) => {
          return {
            placeholder: t('flink.app.addAppTips.kubernetesClusterIdPlaceholder'),
            onChange: (e: ChangeEvent) => (formModel.jobName = e.target.value),
          };
        },
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
        rules: [
          { required: true, message: t('flink.app.addAppTips.kubernetesClusterIdPlaceholder') },
        ],
      },
      {
        field: 'clusterId',
        label: t('flink.app.kubernetesClusterId'),
        component: 'Select',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        componentProps: {
          placeholder: t('flink.app.addAppTips.kubernetesClusterIdPlaceholder'),
          options: (getExecutionCluster(ExecModeEnum.KUBERNETES_SESSION) || []).map((i) => ({
            label: i.clusterName,
            value: i.clusterId,
          })),
        },
        rules: [
          {
            required: true,
            message: t('flink.app.addAppTips.kubernetesClusterIdIsRequiredMessage'),
          },
        ],
      },
      {
        field: 'flinkImage',
        label: t('flink.app.flinkBaseDockerImage'),
        component: 'Input',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: t('flink.app.addAppTips.flinkImagePlaceholder'),
            options: historyRecord.k8sSessionClusterId,
          }),
        rules: [{ required: true, message: t('flink.app.addAppTips.flinkImageIsRequiredMessage') }],
      },
      {
        field: 'k8sRestExposedType',
        label: t('flink.app.restServiceExposedType'),
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
        component: 'Select',
        componentProps: {
          placeholder: t('flink.app.addAppTips.k8sRestExposedTypePlaceholder'),
          options: k8sRestExposedType,
        },
      },
    ];
  };

  const getFlinkFormOtherSchemas = (appId?: string): FormSchema[] => [
    {
      field: 'jobName',
      label: t('flink.app.appName'),
      component: 'Input',
      componentProps: {
        placeholder: t('flink.app.addAppTips.appNamePlaceholder'),
      },
      dynamicRules: () => {
        return [
          {
            required: true,
            trigger: 'blur',
            validator: async (_rule, value) => {
              if (value === null || value === undefined || value === '') {
                return Promise.reject(t('flink.app.addAppTips.appNameIsRequiredMessage'));
              } else {
                const params = { jobName: value };
                if (appId) Object.assign(params, { id: appId });
                const res = await fetchCheckName(params);
                switch (parseInt(res)) {
                  case 0:
                    return Promise.resolve();
                  case 1:
                    return Promise.reject(t('flink.app.addAppTips.appNameNotUniqueMessage'));
                  case 2:
                    return Promise.reject(t('flink.app.addAppTips.appNameExistsInYarnMessage'));
                  case 3:
                    return Promise.reject(t('flink.app.addAppTips.appNameExistsInK8sMessage'));
                  default:
                    return Promise.reject(t('flink.app.addAppTips.appNameNotValid'));
                }
              }
            },
          },
        ];
      },
    },
    {
      field: 'tags',
      label: t('flink.app.tags'),
      component: 'Input',
      componentProps: {
        placeholder: t('flink.app.addAppTips.tagsPlaceholder'),
      },
    },
    {
      field: 'resolveOrder',
      label: t('flink.app.resolveOrder'),
      component: 'Select',
      componentProps: {
        placeholder: 'classloader.resolve-order',
        options: resolveOrder,
      },
      rules: [{ required: true, message: 'Resolve Order is required', type: 'number' }],
    },
    {
      field: 'parallelism',
      label: t('flink.app.parallelism'),
      component: 'InputNumber',
      componentProps: {
        placeholder: t('flink.app.addAppTips.parallelismPlaceholder'),
        min: 1,
        step: 1,
        class: '!w-full',
      },
    },
    {
      field: 'slot',
      label: t('flink.app.dashboard.taskSlots'),
      component: 'InputNumber',
      componentProps: {
        placeholder: t('flink.app.addAppTips.slotsOfPerTaskManagerPlaceholder'),
        min: 1,
        step: 1,
        class: '!w-full',
      },
    },
    {
      field: 'restartSize',
      label: t('flink.app.restartSize'),
      ifShow: ({ values }) => (editModel == 'flink' ? true : !isK8sExecMode(values.executionMode)),
      component: 'InputNumber',
      componentProps: {
        placeholder: t('flink.app.addAppTips.restartSizePlaceholder'),
        min: 1,
        step: 1,
        class: '!w-full',
      },
    },
    {
      field: 'alertId',
      label: t('flink.app.faultAlertTemplate'),
      component: 'Select',
      componentProps: {
        placeholder: t('flink.app.addAppTips.alertTemplatePlaceholder'),
        options: unref(alerts),
        fieldNames: { label: 'alertName', value: 'id', options: 'options' },
      },
    },
    {
      field: 'checkPointFailure',
      label: t('flink.app.checkPointFailureOptions'),
      component: 'InputNumber',
      renderColContent: renderInputGroup,
      show: ({ values }) => (editModel == 'flink' ? true : !isK8sExecMode(values.executionMode)),
    },
    ...getConfigSchemas(),
    {
      field: 'totalOptions',
      label: t('flink.app.totalMemoryOptions'),
      component: 'Select',
      render: (renderCallbackParams) => renderTotalMemory(renderCallbackParams),
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
      label: t('flink.app.jmMemoryOptions'),
      component: 'Select',
      componentProps: {
        showSearch: true,
        allowClear: true,
        mode: 'multiple',
        maxTagCount: 2,
        placeholder: t('flink.app.addAppTips.totalMemoryOptionsPlaceholder'),
        fieldNames: { label: 'name', value: 'key', options: 'options' },
        options: optionData.filter((x) => x.group === 'jobmanager-memory'),
      },
    },
    {
      field: 'jmMemoryItem',
      label: 'jmMemoryItem',
      component: 'Select',
      renderColContent: ({ model, field }) =>
        renderOptionsItems(model, 'jmOptions', field, 'jobmanager.memory.'),
    },
    {
      field: 'tmOptions',
      label: t('flink.app.tmMemoryOptions'),
      component: 'Select',
      componentProps: {
        showSearch: true,
        allowClear: true,
        mode: 'multiple',
        maxTagCount: 2,
        placeholder: t('flink.app.addAppTips.tmPlaceholder'),
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
      label: t('flink.app.yarnQueue'),
      component: 'Input',
      componentProps: { placeholder: t('flink.app.addAppTips.yarnQueuePlaceholder') },
      ifShow: ({ values }) => values.executionMode == ExecModeEnum.YARN_APPLICATION,
    },
    {
      field: 'podTemplate',
      label: t('flink.app.podTemplate'),
      component: 'Input',
      slot: 'podTemplate',
      ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
    },
    {
      field: 'dynamicProperties',
      label: t('flink.app.dynamicProperties'),
      component: 'Input',
      render: (renderCallbackParams) => renderDynamicProperties(renderCallbackParams),
    },
    {
      field: 'args',
      label: t('flink.app.programArgs'),
      component: 'InputTextArea',
      defaultValue: '',
      slot: 'args',
      ifShow: ({ values }) => (editModel ? true : values.jobType == 'customcode'),
    },
    {
      field: 'description',
      label: t('common.description'),
      component: 'InputTextArea',
      componentProps: {
        rows: 4,
        placeholder: t('flink.app.addAppTips.descriptionPlaceholder'),
      },
    },
  ];

  /* flink type data */
  const getFlinkTypeSchema = (): FormSchema[] => {
    return [
      {
        field: 'jobType',
        label: t('flink.app.developmentMode'),
        component: 'Input',
        render: ({ model }) => {
          if (model.jobType == 1) {
            return h(
              Alert,
              { type: 'info' },
              {
                message: () => [
                  h(Icon, { icon: 'ant-design:code-outlined', style: { color: '#108ee9' } }),
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
        label: t('flink.app.appType'),
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
      rules: [{ required: true, message: t('flink.app.addAppTips.flinkSqlIsRequiredMessage') }],
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
    { field: 'configOverride', label: '', component: 'Input', show: false },
    {
      field: 'isSetConfig',
      label: 'Application Conf',
      component: 'Switch',
      ifShow: ({ values }) => {
        if (appId) {
          return values?.jobType == 2 && !isK8sExecMode(values.executionMode);
        } else {
          return values?.jobType == 'sql' && !isK8sExecMode(values.executionMode);
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
    fetchSelect({}).then((res) => {
      projectList.value = res;
    });

    /* Get alert data */
    fetchAlertSetting().then((res) => {
      alerts.value = res;
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
