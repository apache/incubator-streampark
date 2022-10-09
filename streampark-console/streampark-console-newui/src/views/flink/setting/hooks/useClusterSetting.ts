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
import { executionModes, k8sRestExposedType, resolveOrder } from '../../app/data';
import {
  renderDynamicOption,
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
import { handleFormValue, handleYarnQueue } from '../../app/utils';
import { useMessage } from '/@/hooks/web/useMessage';

export const useClusterSetting = () => {
  const { createMessage } = useMessage();

  const submitLoading = ref(false);
  const flinkEnvs = ref<any[]>([]);
  const historyRecord = reactive<{
    k8sNamespace: string[];
    k8sSessionClusterId: string[];
    serviceAccount: string[];
    kubeConfFile: string[];
    flinkImage: string[];
  }>({
    k8sNamespace: [],
    k8sSessionClusterId: [],
    serviceAccount: [],
    kubeConfFile: [],
    flinkImage: [],
  });

  const changeLoading = (loading: boolean) => {
    submitLoading.value = loading;
  };
  const getLoading = computed(() => submitLoading.value);

  /* 校验 */
  async function handleCheckExecMode(_rule: RuleObject, value: StoreValue) {
    if (value === null || value === undefined || value === '') {
      return Promise.reject('Execution Mode is required');
    } else {
      if (value === 2 || value === 3 || value === 4) {
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
          options: executionModes,
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
        field: 'yarnQueue',
        label: 'Yarn Queue',
        component: 'Input',
        componentProps: {
          placeholder: 'Please enter yarn queue',
        },
        ifShow: ({ values }) => values.executionMode == 3,
      },
      {
        field: 'address',
        label: 'Address',
        component: 'Input',
        componentProps: ({ formModel }) => {
          return {
            placeholder:
              formModel.executionMode == 1
                ? "Please enter cluster address, multiple addresses use ',' split e.g: http://host:port,http://host1:port2"
                : 'Please enter cluster address,  e.g: http://host:port',
          };
        },
        dynamicRules: ({ model }) => {
          return [{ required: model.executionMode == 1 }];
        },
      },
      {
        field: 'clusterId',
        label: 'Yarn Session ClusterId',
        component: 'Input',
        componentProps: {
          placeholder: 'Please enter Yarn Session clusterId',
        },
        ifShow: ({ values }) => values.executionMode == 3,
      },
      {
        field: 'k8sNamespace',
        label: 'Kubernetes Namespace',
        ifShow: ({ values }) => values.executionMode == 5,
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
        ifShow: ({ values }) => values.executionMode == 5,
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
        ifShow: ({ values }) => values.executionMode == 5,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: 'default',
            options: historyRecord.serviceAccount,
          }),
      },
      {
        field: 'kubeConfFile',
        label: 'Kube Conf File',
        ifShow: ({ values }) => values.executionMode == 5,
        component: 'Input',
        render: ({ model, field }) =>
          renderInputDropdown(model, field, {
            placeholder: '~/.kube/config',
            options: historyRecord.kubeConfFile,
          }),
      },
      {
        field: 'flinkImage',
        label: 'Flink Base Docker Image',
        ifShow: ({ values }) => values.executionMode == 5,
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
        ifShow: ({ values }) => values.executionMode == 5,
        component: 'Select',
        componentProps: {
          placeholder: 'kubernetes.rest-service.exposed.type',
          options: k8sRestExposedType,
        },
      },
      {
        field: 'resolveOrder',
        label: 'Resolve Order',
        ifShow: ({ values }) => [3, 5].includes(values.executionMode),
        component: 'Select',
        componentProps: { placeholder: 'classloader.resolve-order', options: resolveOrder },
        rules: [{ required: true, message: 'Resolve Order is required', type: 'number' }],
      },
      {
        field: 'slot',
        label: 'Task Slots',
        ifShow: ({ values }) => [3, 5].includes(values.executionMode),
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
        ifShow: ({ values }) => [3, 5].includes(values.executionMode),
        component: 'Select',
        render: (renderCallbackParams) => renderTotalMemory(renderCallbackParams),
      },
      {
        field: 'totalItem',
        label: 'totalItem',
        ifShow: ({ values }) => [3, 5].includes(values.executionMode),
        component: 'Select',
        renderColContent: ({ model }) => renderOptionsItems(model, 'totalOptions', '.memory'),
      },
      {
        field: 'jmOptions',
        label: 'JM Memory Options',
        ifShow: ({ values }) => [3, 5].includes(values.executionMode),
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
        ifShow: ({ values }) => [3, 5].includes(values.executionMode),
        component: 'Select',
        renderColContent: ({ model }) =>
          renderOptionsItems(model, 'jmOptions', 'jobmanager.memory.'),
      },
      {
        field: 'tmOptions',
        label: 'TM Memory Options',
        ifShow: ({ values }) => [3, 5].includes(values.executionMode),
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
        ifShow: ({ values }) => [3, 5].includes(values.executionMode),
        component: 'Select',
        renderColContent: ({ model }) =>
          renderOptionsItems(model, 'tmOptions', 'taskmanager.memory.'),
      },
      {
        field: 'dynamicOptions',
        label: 'Dynamic Option',
        ifShow: ({ values }) => [3, 5].includes(values.executionMode),
        component: 'Input',
        render: (renderCallbackParams) => renderDynamicOption(renderCallbackParams),
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
      clusterId: values.clusterId || null,
      clusterName: values.clusterName,
      executionMode: values.executionMode,
      versionId: values.versionId,
      description: values.description,
    };
    switch (values.executionMode) {
      case 1:
        Object.assign(params, {
          address: values.address,
        });
        return params;
      case 3:
        Object.assign(params, {
          options: JSON.stringify(options),
          yarnQueue: handleYarnQueue(values),
          dynamicOptions: values.dynamicOptions,
          resolveOrder: values.resolveOrder,
          address: values.address,
          flameGraph: values.flameGraph,
        });
        return params;
      case 5:
        Object.assign(params, {
          options: JSON.stringify(options),
          dynamicOptions: values.dynamicOptions,
          resolveOrder: values.resolveOrder,
          k8sRestExposedType: values.k8sRestExposedType,
          k8sNamespace: values.k8sNamespace || null,
          serviceAccount: values.serviceAccount,
          k8sConf: values.kubeConfFile,
          flinkImage: values.flinkImage || null,
          address: values.address,
          flameGraph: values.flameGraph,
        });
        return params;
      default:
        createMessage.error('error executionMode.');
        return {};
    }
  }
  onMounted(async () => {
    flinkEnvs.value = await fetchFlinkEnv();
    historyRecord.k8sNamespace = await fetchK8sNamespaces();
    historyRecord.k8sSessionClusterId = await fetchSessionClusterIds({ executionMode: 5 });
    historyRecord.flinkImage = await fetchFlinkBaseImages();
  });
  return { getClusterSchema, handleSubmitParams, changeLoading, getLoading };
};
