import { ExecModeEnum } from '/@/enums/flinkEnum';
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
import { RuleObject } from 'ant-design-vue/lib/form';
import { StoreValue } from 'ant-design-vue/lib/form/interface';
import { computed, onMounted, reactive, ref, unref } from 'vue';
import { k8sRestExposedType, resolveOrder } from '../../app/data';
import {
  renderDynamicProperties,
  renderInputDropdown,
  renderOptionsItems,
  renderTotalMemory,
} from '../../app/hooks/useFlinkRender';
import { fetchCheckHadoop } from '/@/api/flink/setting';
import { fetchFlinkEnv } from '/@/api/flink/setting/flinkEnv';
import { FormSchema } from '/@/components/Table';
import optionData from '../../app/data/option';
import {
  fetchFlinkBaseImages,
  fetchK8sNamespaces,
  fetchSessionClusterIds,
} from '/@/api/flink/app/flinkHistory';
import { handleFormValue } from '../../app/utils';
import { useMessage } from '/@/hooks/web/useMessage';
import { useI18n } from '/@/hooks/web/useI18n';

export const useClusterSetting = () => {
  const { createMessage } = useMessage();
  const { t } = useI18n();

  const submitLoading = ref(false);
  const flinkEnvs = ref<any[]>([]);
  const historyRecord = reactive<{
    k8sNamespace: string[];
    k8sSessionClusterId: string[];
    serviceAccount: string[];
    k8sConf: string[];
    flinkImage: string[];
  }>({
    k8sNamespace: [],
    k8sSessionClusterId: [],
    serviceAccount: [],
    k8sConf: [],
    flinkImage: [],
  });

  const changeLoading = (loading: boolean) => {
    submitLoading.value = loading;
  };
  const getLoading = computed(() => submitLoading.value);

  /* check */
  async function handleCheckExecMode(_rule: RuleObject, value: StoreValue) {
    if (value === null || value === undefined || value === '') {
      return Promise.reject(t('flink.setting.cluster.required.executionMode'));
    } else {
      if (value === ExecModeEnum.YARN_SESSION) {
        try {
          const res = await fetchCheckHadoop();
          if (res) {
            return Promise.resolve();
          } else {
            return Promise.reject(
              t('flink.setting.cluster.operateMessage.hadoopEnvInitializationFailed'),
            );
          }
        } catch (error) {
          return Promise.reject(
            t('flink.setting.cluster.operateMessage.hadoopEnvInitializationFailed'),
          );
        }
      } else {
        return Promise.resolve();
      }
    }
  }

  // session mode
  function isShowInSessionMode(value: Recordable): boolean {
    return (
      value.executionMode == ExecModeEnum.YARN_SESSION ||
      value.executionMode == ExecModeEnum.KUBERNETES_SESSION
    );
  }

  const getClusterSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'clusterName',
        label: t('flink.setting.cluster.form.clusterName'),
        component: 'Input',
        componentProps: {
          placeholder: t('flink.setting.cluster.placeholder.clusterName'),
        },
        required: true,
      },
      {
        field: 'executionMode',
        label: t('flink.setting.cluster.form.executionMode'),
        component: 'Select',
        componentProps: {
          placeholder: t('flink.setting.cluster.placeholder.executionMode'),
          options: [
            {
              label: 'remote',
              value: ExecModeEnum.REMOTE,
            },
            { label: 'yarn session', value: ExecModeEnum.YARN_SESSION },
            { label: 'kubernetes session', value: ExecModeEnum.KUBERNETES_SESSION },
          ],
        },
        dynamicRules: () => {
          return [{ required: true, validator: handleCheckExecMode }];
        },
      },
      {
        field: 'versionId',
        label: t('flink.setting.cluster.form.versionId'),
        component: 'Select',
        componentProps: {
          placeholder: t('flink.setting.cluster.placeholder.versionId'),
          options: unref(flinkEnvs),
          fieldNames: { label: 'flinkName', value: 'id', options: 'options' },
        },
        rules: [{ required: true, message: t('flink.setting.cluster.required.versionId') }],
      },
      {
        field: 'address',
        label: 'JobManager URL',
        component: 'Input',
        componentProps: {
          placeholder: t('flink.setting.cluster.placeholder.addressRemoteMode'),
        },
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.REMOTE,
        rules: [{ required: true, message: t('flink.setting.cluster.required.address') }],
      },
      {
        field: 'yarnQueue',
        label: t('flink.setting.cluster.form.yarnQueue'),
        component: 'Input',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.YARN_SESSION,
        componentProps: {
          placeholder: t('flink.setting.cluster.placeholder.yarnQueue'),
        },
      },
      {
        field: 'clusterId',
        label: t('flink.setting.cluster.form.k8sClusterId'),
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        component: 'Input',
        defaultValue: unref(flinkEnvs).filter((v) => v.isDefault)[0],
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: 'default',
            options: historyRecord.k8sSessionClusterId,
          }),
      },
      {
        field: 'k8sNamespace',
        label: t('flink.setting.cluster.form.k8sNamespace'),
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: 'default',
            options: historyRecord.k8sNamespace,
          }),
      },
      {
        field: 'serviceAccount',
        label: t('flink.setting.cluster.form.serviceAccount'),
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: 'default',
            options: historyRecord.serviceAccount,
          }),
      },
      {
        field: 'k8sConf',
        label: t('flink.setting.cluster.form.k8sConf'),
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: t('flink.setting.cluster.placeholder.k8sConf'),
            options: historyRecord.k8sConf,
          }),
      },
      {
        field: 'flinkImage',
        label: t('flink.setting.cluster.form.flinkImage'),
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: t('flink.setting.cluster.placeholder.flinkImage'),
            options: historyRecord.flinkImage,
          }),
        rules: [{ required: true, message: t('flink.setting.cluster.required.flinkImage') }],
      },
      {
        field: 'k8sRestExposedType',
        label: t('flink.setting.cluster.form.k8sRestExposedType'),
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        component: 'Select',
        componentProps: {
          placeholder: t('flink.setting.cluster.placeholder.k8sRestExposedType'),
          options: k8sRestExposedType,
        },
      },
      {
        field: 'resolveOrder',
        label: t('flink.setting.cluster.form.resolveOrder'),
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        componentProps: {
          placeholder: t('flink.setting.cluster.placeholder.resolveOrder'),
          options: resolveOrder,
        },
        rules: [{ message: t('flink.setting.cluster.placeholder.resolveOrder'), type: 'number' }],
      },
      {
        field: 'slot',
        label: t('flink.setting.cluster.form.taskSlots'),
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'InputNumber',
        componentProps: {
          placeholder: t('flink.setting.cluster.placeholder.taskSlots'),
          min: 1,
          step: 1,
          class: '!w-full',
        },
      },
      {
        field: 'totalOptions',
        label: 'Total Memory Options',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        render: (renderCallbackParams) => renderTotalMemory(renderCallbackParams),
      },
      {
        field: 'totalItem',
        label: 'totalItem',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'totalOptions', field, '.memory', true),
      },
      {
        field: 'jmOptions',
        label: t('flink.setting.cluster.form.jmOptions'),
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        componentProps: {
          showSearch: true,
          allowClear: true,
          mode: 'multiple',
          maxTagCount: 2,
          placeholder: t('flink.setting.cluster.placeholder.jmOptions'),
          fieldNames: { label: 'name', value: 'key', options: 'options' },
          options: optionData.filter((x) => x.group === 'jobmanager-memory'),
        },
      },
      {
        field: 'jmOptionsItem',
        label: 'jmOptionsItem',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'jmOptions', field, 'jobmanager.memory.'),
      },
      {
        field: 'tmOptions',
        label: t('flink.setting.cluster.form.tmOptions'),
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        componentProps: {
          showSearch: true,
          allowClear: true,
          mode: 'multiple',
          maxTagCount: 2,
          placeholder: t('flink.setting.cluster.placeholder.tmOptions'),
          fieldNames: { label: 'name', value: 'key', options: 'options' },
          options: optionData.filter((x) => x.group === 'taskmanager-memory'),
        },
      },
      {
        field: 'tmOptionsItem',
        label: 'tmOptionsItem',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'tmOptions', field, 'taskmanager.memory.'),
      },
      {
        field: 'dynamicProperties',
        label: t('flink.setting.cluster.form.dynamicProperties'),
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Input',
        render: (renderCallbackParams) => renderDynamicProperties(renderCallbackParams),
      },
      {
        field: 'description',
        label: t('flink.setting.cluster.form.clusterDescription'),
        component: 'InputTextArea',
        componentProps: {
          rows: 4,
          placeholder: t('flink.setting.cluster.placeholder.clusterDescription'),
        },
      },
    ];
  });
  function handleSubmitParams(values: Recordable) {
    const options = handleFormValue(values);
    const params = {
      clusterName: values.clusterName,
      executionMode: values.executionMode,
      versionId: values.versionId,
      description: values.description,
    };

    switch (values.executionMode) {
      case ExecModeEnum.REMOTE:
        Object.assign(params, {
          address: values.address,
        });
        return params;
      case ExecModeEnum.YARN_SESSION:
        Object.assign(params, {
          options: JSON.stringify(options),
          yarnQueue: values.yarnQueue || 'default',
          dynamicProperties: values.dynamicProperties,
          resolveOrder: values.resolveOrder,
        });
      case ExecModeEnum.KUBERNETES_SESSION:
        Object.assign(params, {
          clusterId: values.clusterId,
          options: JSON.stringify(options),
          dynamicProperties: values.dynamicProperties,
          resolveOrder: values.resolveOrder,
          k8sRestExposedType: values.k8sRestExposedType,
          k8sNamespace: values.k8sNamespace || null,
          serviceAccount: values.serviceAccount,
          k8sConf: values.k8sConf,
          flinkImage: values.flinkImage || null,
          address: values.address,
        });
        return params;
      default:
        createMessage.error('error executionMode.');
        return {};
    }
  }
  onMounted(() => {
    fetchFlinkEnv().then((res) => {
      flinkEnvs.value = res;
    });
    fetchK8sNamespaces().then((res) => {
      historyRecord.k8sNamespace = res;
    });
    fetchSessionClusterIds({
      executionMode: ExecModeEnum.KUBERNETES_SESSION,
    }).then((res) => {
      historyRecord.k8sSessionClusterId = res;
    });
    fetchFlinkBaseImages().then((res) => {
      historyRecord.flinkImage = res;
    });
  });
  return { getClusterSchema, handleSubmitParams, changeLoading, getLoading };
};
