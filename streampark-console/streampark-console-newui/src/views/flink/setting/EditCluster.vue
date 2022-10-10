<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<script lang="ts">
  import { defineComponent } from 'vue';
  export default defineComponent({
    name: 'EditCluster',
  });
</script>
<script setup lang="ts" name="EditCluster">
  import { PageWrapper } from '/@/components/Page';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    fetchCheckCluster,
    fetchGetCluster,
    fetchUpdateCluster,
  } from '/@/api/flink/setting/flinkCluster';

  import { useClusterSetting } from './hooks/useClusterSetting';
  import { nextTick, onMounted, reactive } from 'vue';
  import { useRoute } from 'vue-router';
  import { useEdit } from '../app/hooks/useEdit';
  import { useI18n } from '/@/hooks/web/useI18n';

  const { close } = useTabs();
  const route = useRoute();
  const { t } = useI18n();
  const { createMessage, createErrorModal } = useMessage();
  const { handleResetApplication, defaultOptions } = useEdit();
  const cluster = reactive<Recordable>({});
  const { getLoading, changeLoading, getClusterSchema, handleSubmitParams } = useClusterSetting();

  const [registerForm, { submit, setFieldsValue }] = useForm({
    labelWidth: 120,
    colon: true,
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
    baseColProps: { span: 24 },
    showActionButtonGroup: false,
  });

  async function handleSubmitCluster(values: Recordable) {
    try {
      changeLoading(true);
      const params = handleSubmitParams(values);

      if (Object.keys(params).length > 0) {
        Object.assign(params, {
          id: cluster.id,
        });
        const res = await fetchCheckCluster(params);
        if (res === 'success') {
          const resp = await fetchUpdateCluster(params);
          if (resp.status) {
            createMessage.success(values.clusterName.concat(' update successful!'));
            close(undefined, { path: '/flink/setting', query: { activeKey: 'cluster' } });
          } else {
            createMessage.error(resp.msg);
          }
        } else if (res === 'exists') {
          createErrorModal({
            title: 'Failed',
            content: 'the cluster name: ' + values.clusterName + ' is already exists,please check',
          });
        } else {
          createErrorModal({
            title: 'Failed',
            content: 'the address is invalid or connection failure, please check',
          });
        }
      }
    } catch (error) {
      console.error(error);
    } finally {
      changeLoading(false);
    }
  }
  // Get cluster data
  async function getClusterInfo() {
    const res = await fetchGetCluster({ id: route?.query?.clusterId });
    Object.assign(cluster, res);
    defaultOptions.value = JSON.parse(res.options || '{}');
    handleReset();
  }
  function handleReset() {
    const resetParams = handleResetApplication();
    nextTick(() => {
      setFieldsValue({
        clusterName: cluster.clusterName,
        executionMode: cluster.executionMode,
        address: cluster.address,
        clusterId: cluster.clusterId,
        description: cluster.description,
        dynamicOptions: cluster.dynamicOptions,
        resolveOrder: cluster.resolveOrder,
        yarnQueue: cluster.yarnQueue,
        versionId: cluster.versionId || null,
        k8sRestExposedType: cluster.k8sRestExposedType,
        flinkImage: cluster.flinkImage,
        serviceAccount: cluster.serviceAccount,
        kubeConfFile: cluster.kubeConfFile,
        flameGraph: cluster.flameGraph,
        k8sNamespace: cluster.k8sNamespace,
        ...resetParams,
      });
    });
  }
  onMounted(() => {
    getClusterInfo();
  });
</script>
<template>
  <PageWrapper content-background content-class="py-30px">
    <BasicForm @register="registerForm" @submit="handleSubmitCluster" :schemas="getClusterSchema">
      <template #formFooter>
        <div class="flex items-center w-full justify-center">
          <a-button
            @click="close(undefined, { path: '/flink/setting', query: { activeKey: 'cluster' } })"
          >
            {{ t('common.cancelText') }}
          </a-button>
          <a-button class="ml-4" :loading="getLoading" type="primary" @click="submit()">
            {{ t('common.submitText') }}
          </a-button>
        </div>
      </template></BasicForm
    >
  </PageWrapper>
</template>
