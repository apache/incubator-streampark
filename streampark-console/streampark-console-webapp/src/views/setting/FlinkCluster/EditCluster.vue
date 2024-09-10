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
  import { useGo } from '/@/hooks/web/usePage';
  export default defineComponent({
    name: 'EditCluster',
  });
</script>
<script setup lang="ts" name="EditCluster">
  import { PageWrapper } from '/@/components/Page';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    fetchCheckCluster,
    fetchGetCluster,
    fetchUpdateCluster,
  } from '/@/api/flink/setting/flinkCluster';

  import { useClusterSetting } from './useClusterSetting';
  import { nextTick, onMounted, reactive } from 'vue';
  import { useRoute } from 'vue-router';
  import { useEdit } from '../../flink/app/hooks/useEdit';
  import { useI18n } from '/@/hooks/web/useI18n';

  const go = useGo();
  const route = useRoute();
  const { t } = useI18n();
  const { Swal } = useMessage();
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

  // submit edit
  async function handleSubmitCluster(values: Recordable) {
    try {
      changeLoading(true);
      const params = handleSubmitParams(values);

      if (Object.keys(params).length > 0) {
        Object.assign(params, {
          id: cluster.id,
        });
        const res = await fetchCheckCluster(params);
        const status = parseInt(res.status);
        if (status === 0) {
          fetchUpdateCluster(params);
          Swal.fire({
            icon: 'success',
            title: values.clusterName.concat(
              t('setting.flinkCluster.operateMessage.updateFlinkClusterSuccessful'),
            ),
            showConfirmButton: false,
            timer: 2000,
          });
          go('/setting/flinkCluster');
        } else {
          Swal.fire('Failed', res.msg, 'error');
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
    Object.assign(defaultOptions, JSON.parse(res.options || '{}'));
    handleReset();
  }
  function handleReset() {
    const resetParams = handleResetApplication();
    nextTick(() => {
      setFieldsValue({
        clusterName: cluster.clusterName,
        clusterId: cluster.clusterId,
        executionMode: cluster.executionMode,
        address: cluster.address,
        description: cluster.description,
        dynamicProperties: cluster.dynamicProperties,
        resolveOrder: cluster.resolveOrder,
        yarnQueue: cluster.yarnQueue,
        versionId: cluster.versionId || null,
        k8sRestExposedType: cluster.k8sRestExposedType,
        flinkImage: cluster.flinkImage,
        serviceAccount: cluster.serviceAccount,
        k8sConf: cluster.k8sConf,
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
  <PageWrapper content-full-height fixed-height content-class="flex flex-col bg-white">
    <BasicForm
      @register="registerForm"
      @submit="handleSubmitCluster"
      :schemas="getClusterSchema"
      class="mt-30px"
    >
      <template #formFooter>
        <div class="flex items-center w-full justify-center">
          <a-button @click="go('/setting/flinkCluster')">
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
<style lang="less">
  @import url('./Cluster.less');
</style>
