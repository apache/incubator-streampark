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
import { executionModes, k8sRestExposedType, resolveOrder } from '../data';
import optionData from '../data/option';
import {
  getAlertSvgIcon,
  renderDynamicProperties,
  renderInputDropdown,
  renderInputGroup,
  renderIsSetConfig,
  renderOptionsItems,
  renderTotalMemory,
  renderYarnQueue,
} from './useFlinkRender';

import { fetchCheckName } from '/@/api/flink/app/app';
import { RuleObject } from 'ant-design-vue/lib/form';
import { StoreValue } from 'ant-design-vue/lib/form/interface';
import { useDrawer } from '/@/components/Drawer';
import { Alert } from 'ant-design-vue';
import Icon from '/@/components/Icon';
import { useMessage } from '/@/hooks/web/useMessage';
import { fetchVariableAll } from '/@/api/flink/variable';
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
import { ClusterStateEnum, ExecModeEnum, JobTypeEnum } from '/@/enums/flinkEnum';
import { isK8sExecMode } from '../utils';
import { useI18n } from '/@/hooks/web/useI18n';
import { fetchCheckHadoop } from '/@/api/flink/setting';
const { t } = useI18n();
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
          return o.executionMode == executionMode && o.clusterState === ClusterStateEnum.STARTED;
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
            return values?.jobType == JobTypeEnum.SQL;
          } else {
            return values?.jobType == 'sql';
          }
        },
        rules: [{ required: true, message: t('flink.app.addAppTips.flinkSqlIsRequiredMessage') }],
      },
      {
        field: 'dependency',
        label: t('flink.app.dependency'),
        component: 'Input',
        slot: 'dependency',
        ifShow: ({ values }) => {
          if (edit?.appId) {
            return values.jobType == JobTypeEnum.SQL;
          } else {
            return values?.jobType == 'sql';
          }
        },
      },
      { field: 'configOverride', label: '', component: 'Input', show: false },
      {
        field: 'isSetConfig',
        label: t('flink.app.appConf'),
        component: 'Switch',
        ifShow: ({ values }) => {
          if (edit?.appId) {
            return values?.jobType == JobTypeEnum.SQL && !isK8sExecMode(values.executionMode);
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
        label: t('flink.app.flinkVersion'),
        component: 'Select',
        componentProps: {
          placeholder: t('flink.app.flinkVersion'),
          options: unref(flinkEnvs),
          fieldNames: { label: 'flinkName', value: 'id', options: 'options' },
          onChange: (value) => handleFlinkVersion(value),
        },
        rules: [
          { required: true, message: t('flink.app.addAppTips.flinkVersionIsRequiredMessage') },
        ],
      },
      {
        field: 'flinkClusterId',
        label: t('flink.app.flinkCluster'),
        component: 'Select',
        componentProps: {
          placeholder: t('flink.app.flinkCluster'),
          options: getExecutionCluster(ExecModeEnum.REMOTE, 'id'),
        },
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.REMOTE,
        rules: [
          { required: true, message: t('flink.app.addAppTips.flinkClusterIsRequiredMessage') },
        ],
      },
      {
        field: 'yarnSessionClusterId',
        label: t('flink.app.flinkCluster'),
        component: 'Select',
        componentProps: {
          placeholder: t('flink.app.flinkCluster'),
          options: getExecutionCluster(ExecModeEnum.YARN_SESSION, 'id'),
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
            options: unref(historyRecord)?.k8sNamespace || [],
          }),
      },
      {
        field: 'clusterId',
        label: t('flink.app.kubernetesClusterId'),
        component: 'Input',
        componentProps: ({ formModel }) => {
          return {
            placeholder: t('flink.app.addAppTips.kubernetesClusterIdRequire'),
            onChange: (e: ChangeEvent) => (formModel.jobName = e.target.value),
          };
        },
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_APPLICATION,
        rules: [
          {
            required: true,
            message: t('flink.app.addAppTips.kubernetesClusterIdRequire'),
            pattern: /^(?=.{1,45}$)[a-z]([-a-z0-9]*[a-z0-9])$/,
          },
        ],
      },
      {
        field: 'clusterId',
        label: t('flink.app.kubernetesClusterId'),
        component: 'Select',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        componentProps: {
          placeholder: t('flink.app.addAppTips.kubernetesClusterIdPlaceholder'),
          options: getExecutionCluster(ExecModeEnum.KUBERNETES_SESSION, 'clusterId'),
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
            options: unref(historyRecord)?.k8sSessionClusterId || [],
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
  });

  /* Detect job name field */
  async function getJobNameCheck(_rule: RuleObject, value: StoreValue) {
    if (value === null || value === undefined || value === '') {
      return Promise.reject(t('flink.app.addAppTips.appNameIsRequiredMessage'));
    } else {
      const params = { jobName: value };
      if (edit?.appId) Object.assign(params, { id: edit.appId });
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
  }

  const getFlinkFormOtherSchemas = computed((): FormSchema[] => {
    const commonInputNum = {
      min: 0,
      step: 1,
      class: '!w-full',
    };
    return [
      {
        field: 'jobName',
        label: t('flink.app.appName'),
        component: 'Input',
        componentProps: { placeholder: t('flink.app.addAppTips.appNamePlaceholder') },
        dynamicRules: () => {
          return [{ required: true, trigger: 'blur', validator: getJobNameCheck }];
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
        componentProps: { placeholder: 'classloader.resolve-order', options: resolveOrder },
        rules: [{ required: true, message: 'Resolve Order is required', type: 'number' }],
      },
      {
        field: 'parallelism',
        label: t('flink.app.parallelism'),
        component: 'InputNumber',
        componentProps: {
          placeholder: t('flink.app.addAppTips.parallelismPlaceholder'),
          ...commonInputNum,
        },
      },
      {
        field: 'slot',
        label: t('flink.app.dashboard.taskSlots'),
        component: 'InputNumber',
        componentProps: {
          placeholder: t('flink.app.addAppTips.slotsOfPerTaskManagerPlaceholder'),
          ...commonInputNum,
        },
      },
      {
        field: 'restartSize',
        label: t('flink.app.restartSize'),
        ifShow: ({ values }) =>
          edit?.mode == 'flink' ? true : !isK8sExecMode(values.executionMode),
        component: 'InputNumber',
        componentProps: {
          placeholder: t('flink.app.addAppTips.restartSizePlaceholder'),
          ...commonInputNum,
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
        show: ({ values }) => (edit?.mode == 'flink' ? true : !isK8sExecMode(values.executionMode)),
      },
      ...getConfigSchemas(),
      {
        field: 'totalOptions',
        label: t('flink.app.totalMemoryOptions'),
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
        field: 'jmOptionsItem',
        label: 'jmOptionsItem',
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
        ifShow: ({ values }) =>
          values.executionMode == ExecModeEnum.YARN_APPLICATION ||
          values.executionMode == ExecModeEnum.YARN_PER_JOB,
        render: (renderCallbackParams) => renderYarnQueue(renderCallbackParams),
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
        ifShow: ({ values }) => (edit?.mode ? true : values.jobType == 'customcode'),
      },
      {
        field: 'description',
        label: t('common.description'),
        component: 'InputTextArea',
        componentProps: { rows: 4, placeholder: t('flink.app.addAppTips.descriptionPlaceholder') },
      },
    ];
  });

  const getFlinkTypeSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'jobType',
        label: t('flink.app.developmentMode'),
        component: 'Input',
        render: ({ model }) => {
          if (model.jobType == JobTypeEnum.JAR) {
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
        label: t('flink.app.appType'),
        component: 'Input',
        render: () => getAlertSvgIcon('flink', 'StreamPark Flink'),
      },
    ];
  });
  const getExecutionModeSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'executionMode',
        label: t('flink.app.executionMode'),
        component: 'Select',
        itemProps: {
          autoLink: false, //Resolve multiple trigger validators with null value ·
        },
        componentProps: {
          placeholder: t('flink.app.addAppTips.executionModePlaceholder'),
          options: executionModes,
        },
        rules: [
          {
            required: true,
            validator: async (_rule, value) => {
              if (value === null || value === undefined || value === '') {
                return Promise.reject(t('flink.app.addAppTips.executionModeIsRequiredMessage'));
              } else {
                if (
                  [
                    ExecModeEnum.YARN_PER_JOB,
                    ExecModeEnum.YARN_SESSION,
                    ExecModeEnum.YARN_APPLICATION,
                  ].includes(value)
                ) {
                  const res = await fetchCheckHadoop();
                  if (res) {
                    return Promise.resolve();
                  } else {
                    return Promise.reject(t('flink.app.addAppTips.hadoopEnvInitMessage'));
                  }
                }
                return Promise.resolve();
              }
            },
          },
        ],
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
    getExecutionModeSchema,
    openConfDrawer,
  };
};
