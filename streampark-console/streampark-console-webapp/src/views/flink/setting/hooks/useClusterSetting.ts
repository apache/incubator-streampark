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
import { ClusterAddTypeEnum } from '/@/enums/appEnum';
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
      return Promise.reject('Execution Mode is required');
    } else {
      if (value === ExecModeEnum.YARN_SESSION) {
        try {
          const res = await fetchCheckHadoop();
          if (res) {
            return Promise.resolve();
          } else {
            return Promise.reject(
              'Hadoop environment initialization failed, please check the environment settings',
            );
          }
        } catch (error) {
          return Promise.reject(
            'Hadoop environment initialization failed, please check the environment settings',
          );
        }
      } else {
        return Promise.resolve();
      }
    }
  }

  function isAddExistYarnSession(value: Recordable) {
    return (
      value.executionMode == ExecModeEnum.YARN_SESSION &&
      value.addType == ClusterAddTypeEnum.ADD_EXISTING
    );
  }

  // session mode
  function isShowInSessionMode(value: Recordable): boolean {
    if (value.executionMode == ExecModeEnum.YARN_SESSION) {
      return value.addType == ClusterAddTypeEnum.ADD_NEW;
    }
    return value.executionMode == ExecModeEnum.KUBERNETES_SESSION;
  }

  const getClusterSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'clusterName',
        label: 'Cluster Name',
        component: 'Input',
        componentProps: {
          placeholder: 'Please enter cluster name',
        },
        required: true,
      },
      {
        field: 'executionMode',
        label: 'Execution Mode',
        component: 'Select',
        componentProps: {
          placeholder: 'Please enter cluster name',
          options: [
            { label: 'remote (standalone)', value: ExecModeEnum.REMOTE },
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
        label: 'Flink Version',
        component: 'Select',
        componentProps: {
          placeholder: 'Flink Version',
          options: unref(flinkEnvs),
          fieldNames: { label: 'flinkName', value: 'id', options: 'options' },
        },
        rules: [{ required: true, message: 'Flink Version is required' }],
      },
      {
        field: 'addType',
        label: t('flink.setting.cluster.form.addType'),
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.YARN_SESSION,
        component: 'Select',
        defaultValue: ClusterAddTypeEnum.ADD_EXISTING,
        componentProps: {
          placeholder: t('flink.setting.cluster.placeholder.addType'),
          allowClear: false,
          options: [
            {
              label: t('flink.setting.cluster.form.addExisting'),
              value: ClusterAddTypeEnum.ADD_EXISTING,
            },
            {
              label: t('flink.setting.cluster.form.addNew'),
              value: ClusterAddTypeEnum.ADD_NEW,
            },
          ],
        },
      },
      {
        field: 'yarnQueue',
        label: 'Yarn Queue',
        component: 'Input',
        componentProps: {
          placeholder: 'Please enter yarn queue',
        },
        ifShow: ({ values }) =>
          values.executionMode == ExecModeEnum.YARN_SESSION &&
          values.addType == ClusterAddTypeEnum.ADD_NEW,
      },
      {
        field: 'address',
        label: 'Address',
        component: 'Input',
        componentProps: ({ formModel }) => {
          return {
            placeholder:
              formModel.executionMode == ExecModeEnum.REMOTE
                ? "Please enter cluster address, multiple addresses use ',' split e.g: http://host:port,http://host1:port2"
                : 'Please enter cluster address,  e.g: http://host:port',
          };
        },
        ifShow: ({ values }) =>
          isAddExistYarnSession(values) || values.executionMode == ExecModeEnum.REMOTE,
        rules: [{ required: true, message: t('flink.setting.cluster.required.address') }],
      },
      {
        field: 'clusterId',
        label: 'Yarn Session Cluster',
        component: 'Input',
        componentProps: {
          placeholder: 'Please enter Yarn Session cluster',
        },
        ifShow: ({ values }) => isAddExistYarnSession(values),
        rules: [{ required: true, message: t('flink.setting.cluster.required.clusterId') }],
      },
      {
        field: 'k8sNamespace',
        label: 'Kubernetes Namespace',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: 'default',
            options: historyRecord.k8sNamespace,
          }),
      },
      {
        field: 'clusterId',
        label: 'Kubernetes ClusterId',
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
        field: 'serviceAccount',
        label: 'Service Account',
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
        label: 'Kube Conf File',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: '~/.kube/config',
            options: historyRecord.k8sConf,
          }),
      },
      {
        field: 'flinkImage',
        label: 'Flink Base Docker Image',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder:
              'Please enter the tag of Flink base docker image, such as: flink:1.13.0-scala_2.11-java8',
            options: historyRecord.flinkImage,
          }),
        rules: [{ required: true, message: 'Flink Base Docker Image is required' }],
      },
      {
        field: 'k8sRestExposedType',
        label: 'Rest-Service Exposed Type',
        ifShow: ({ values }) => values.executionMode == ExecModeEnum.KUBERNETES_SESSION,
        component: 'Select',
        componentProps: {
          placeholder: 'kubernetes.rest-service.exposed.type',
          options: k8sRestExposedType,
        },
      },
      {
        field: 'resolveOrder',
        label: 'Resolve Order',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        componentProps: { placeholder: 'classloader.resolve-order', options: resolveOrder },
        rules: [{ message: 'Resolve Order is required', type: 'number' }],
      },
      {
        field: 'slot',
        label: 'Task Slots',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'InputNumber',
        componentProps: {
          placeholder: 'Number of slots per TaskManager',
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
        label: 'JM Memory Options',
        ifShow: ({ values }) => isShowInSessionMode(values),
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
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'jmOptions', field, 'jobmanager.memory.'),
      },
      {
        field: 'tmOptions',
        label: 'TM Memory Options',
        ifShow: ({ values }) => isShowInSessionMode(values),
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
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Select',
        renderColContent: ({ model, field }) =>
          renderOptionsItems(model, 'tmOptions', field, 'taskmanager.memory.'),
      },
      {
        field: 'dynamicProperties',
        label: 'Dynamic Properties',
        ifShow: ({ values }) => isShowInSessionMode(values),
        component: 'Input',
        render: (renderCallbackParams) => renderDynamicProperties(renderCallbackParams),
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
        if (values.addType === ClusterAddTypeEnum.ADD_EXISTING) {
          Object.assign(params, {
            clusterId: values.clusterId,
            address: values.address,
          });
        } else {
          Object.assign(params, {
            options: JSON.stringify(options),
            yarnQueue: values.yarnQueue || 'default',
            dynamicProperties: values.dynamicProperties,
            resolveOrder: values.resolveOrder,
          });
        }
        return params;
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
