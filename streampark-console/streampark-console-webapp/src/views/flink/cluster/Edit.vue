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
<script setup lang="ts" name="EditCluster">
  import { unref, ref } from 'vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { PageWrapper } from '/@/components/Page';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    fetchCheckCluster,
    fetchGetCluster,
    fetchUpdateCluster,
  } from '/@/api/flink/flinkCluster';

  import { useClusterSetting } from './useClusterSetting';
  import { nextTick, onMounted, reactive } from 'vue';
  import { useRoute } from 'vue-router';
  import { useEdit } from '../app/hooks/useEdit';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { fetchAlertSetting } from '/@/api/setting/alert';
  import { AlertSetting } from '/@/api/setting/types/alert.type';
  import { DeployMode } from '/@/enums/flinkEnum';
  import {
    fetchManagedEnvironment,
    fetchUpdateManagedEnvironment,
  } from '/@/api/flink/managedFlink';
  import type {
    ManagedFlinkEnvironment,
    ManagedFlinkEnvironmentForm,
  } from '/@/api/flink/managedFlink.type';
  import { parseVolcengineEnvironmentConfig } from '/@/api/flink/managedFlink.type';
  import { useUserStore } from '/@/store/modules/user';

  const go = useGo();
  const route = useRoute();
  const { t } = useI18n();
  const { Swal } = useMessage();
  const userStore = useUserStore();
  const { handleResetApplication, defaultOptions } = useEdit();
  const cluster = reactive<Recordable>({});
  const alerts = ref<AlertSetting[]>([]);
  const {
    getLoading,
    changeLoading,
    getClusterSchema,
    handleSubmitParams,
    prepareManagedEnvironment,
  } = useClusterSetting();
  const managedEnvironment = ref<ManagedFlinkEnvironment>();

  const [registerForm, { submit, setFieldsValue }] = useForm({
    name: 'flink_cluster',
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
        if (values.deployMode === DeployMode.MANAGED_APPLICATION) {
          const version = managedEnvironment.value?.version;
          if (version === undefined) {
            throw new Error(t('setting.flinkCluster.managed.metadataUnavailable'));
          }
          await fetchUpdateManagedEnvironment({
            ...(params as ManagedFlinkEnvironmentForm),
            clusterId: cluster.id,
            version,
          });
          await Swal.fire({
            icon: 'success',
            title: values.clusterName.concat(
              t('setting.flinkCluster.operateMessage.updateFlinkClusterSuccessful'),
            ),
            showConfirmButton: false,
            timer: 2000,
          });
          go('/flink/cluster');
          return;
        }
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
          go('/flink/cluster');
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
    if (res.deployMode === DeployMode.MANAGED_APPLICATION) {
      const teamId = userStore.getTeamId;
      if (!teamId) throw new Error('The active Team is required.');
      managedEnvironment.value = await fetchManagedEnvironment({
        teamId,
        clusterId: res.id,
      });
      await prepareManagedEnvironment(managedEnvironment.value);
    }
    Object.assign(defaultOptions, JSON.parse(res.options || '{}'));
    handleReset();
  }
  function handleReset() {
    const resetParams = handleResetApplication();
    const providerConfig = parseVolcengineEnvironmentConfig(managedEnvironment.value);
    nextTick(() => {
      let selectAlertId: string | undefined;
      if (cluster.alertId) {
        selectAlertId = unref(alerts)?.filter((t) => t.id == cluster.alertId)[0]?.id;
      }
      setFieldsValue({
        clusterName: cluster.clusterName,
        clusterId: cluster.clusterId,
        deployMode: cluster.deployMode,
        address: cluster.address,
        description: cluster.description,
        dynamicProperties: cluster.dynamicProperties,
        resolveOrder: cluster.resolveOrder,
        yarnQueue: cluster.yarnQueue,
        alertId: selectAlertId,
        versionId: cluster.versionId || null,
        k8sRestExposedType: cluster.k8sRestExposedType,
        flinkImage: cluster.flinkImage,
        serviceAccount: cluster.serviceAccount,
        k8sConf: cluster.k8sConf,
        k8sNamespace: cluster.k8sNamespace,
        cloudAccountId: managedEnvironment.value?.cloudAccountId,
        region: managedEnvironment.value?.region,
        projectId: providerConfig.projectId,
        resourcePoolId: providerConfig.resourcePoolId,
        draftDirectoryId: providerConfig.draftDirectoryId,
        tosBucket: providerConfig.tosBucket,
        ...resetParams,
      });
    });
  }
  onMounted(() => {
    fetchAlertSetting().then((res) => {
      alerts.value = res;
    });
    getClusterInfo();
  });
</script>
<template>
  <PageWrapper content-background content-full-height>
    <BasicForm
      @register="registerForm"
      @submit="handleSubmitCluster"
      :schemas="getClusterSchema"
      class="!my-30px"
    >
      <template #formFooter>
        <div class="flex items-center w-full justify-center">
          <a-button @click="go('/flink/cluster')">
            {{ t('common.cancelText') }}
          </a-button>
          <a-button
            id="e2e-flinkcluster-submit-btn"
            class="ml-4"
            :loading="getLoading"
            type="primary"
            @click="submit()"
          >
            {{ t('common.submitText') }}
          </a-button>
        </div>
      </template></BasicForm
    >
  </PageWrapper>
</template>
