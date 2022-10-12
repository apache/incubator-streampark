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
  export default defineComponent({
    name: 'EditFlink',
  });
</script>
<script setup lang="ts" name="EditFlink">
  import { PageWrapper } from '/@/components/Page';
  import { BasicForm, useForm } from '/@/components/Form';
  import { onMounted, reactive, ref, nextTick, defineComponent, unref } from 'vue';
  import { Alert } from 'ant-design-vue';
  import { fetchMain, fetchUpload, fetchUpdate } from '/@/api/flink/app/app';
  import { handleFormValue, handleYarnQueue } from './utils';
  import PomTemplateTab from './components/PodTemplate/PomTemplateTab.vue';
  import { fetchListJars } from '/@/api/flink/project';
  import { useEditFlinkSchema } from './hooks/useEditFlinkSchema';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { useEdit } from './hooks/useEdit';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useRoute } from 'vue-router';

  const route = useRoute();
  const { t } = useI18n();
  const { createMessage } = useMessage();
  const { close } = useTabs();

  const submitLoading = ref<boolean>(false);
  const jars = ref<string[]>([]);

  const uploadLoading = ref(false);
  const uploadJar = ref('');

  const k8sTemplate = reactive({
    podTemplate: '',
    jmPodTemplate: '',
    tmPodTemplate: '',
  });

  const { getEditFlinkFormSchema, alerts } = useEditFlinkSchema(jars);
  const { handleGetApplication, app, handleResetApplication, selectAlertId } = useEdit(alerts);
  const [registerForm, { setFieldsValue, submit }] = useForm({
    labelWidth: 120,
    colon: true,
    baseColProps: { span: 24 },
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
    showActionButtonGroup: false,
  });

  /* Form reset */
  function handleReset(executionMode?: string) {
    nextTick(async () => {
      const resetParams = await handleResetApplication();
      const defaultParams = {
        jobName: app.jobName,
        tags: app.tags,
        mainClass: app.mainClass,
        args: app.args,
        jar: app.jar,
        description: app.description,
        dynamicOptions: app.dynamicOptions,
        resolveOrder: app.resolveOrder,
        executionMode: app.executionMode,
        yarnQueue: app.yarnQueue,
        restartSize: app.restartSize,
        cpMaxFailureInterval: app.cpMaxFailureInterval,
        cpFailureRateInterval: app.cpFailureRateInterval,
        cpFailureAction: app.cpFailureAction,
        versionId: app.versionId || null,
        k8sRestExposedType: app.k8sRestExposedType,
        clusterId: app.clusterId,
        flinkClusterId: app.flinkClusterId,
        flinkImage: app.flinkImage,
        k8sNamespace: app.k8sNamespace,
        alertId: selectAlertId,
        yarnSessionClusterId: app.yarnSessionClusterId,
        ...resetParams,
      };
      if (!executionMode) {
        Object.assign(defaultParams, { executionMode: app.executionMode });
      }
      setFieldsValue(defaultParams);
    });
  }
  /* Custom job upload */
  async function handleCustomJobRequest(data) {
    const formData = new FormData();
    formData.append('file', data.file);
    try {
      const path = await fetchUpload(formData);
      uploadLoading.value = false;
      uploadJar.value = data.file.name;
      const res = await fetchMain({
        jar: path,
      });
      setFieldsValue({ mainClass: res, jar: unref(uploadJar) });
    } catch (error) {
      console.error(error);
    } finally {
      uploadLoading.value = false;
    }
  }

  /* Handling update parameters */
  function handleAppUpdate(values) {
    submitLoading.value = true;
    try {
      const options = handleFormValue(values);
      const params = {
        id: app.id,
        jobName: values.jobName,
        tags: values.tags,
        resolveOrder: values.resolveOrder,
        versionId: values.versionId,
        executionMode: values.executionMode,
        jar: values.jar,
        mainClass: values.mainClass,
        args: values.args,
        options: JSON.stringify(options),
        yarnQueue: handleYarnQueue(values),
        cpMaxFailureInterval: values.cpMaxFailureInterval || null,
        cpFailureRateInterval: values.cpFailureRateInterval || null,
        cpFailureAction: values.cpFailureAction || null,
        dynamicOptions: values.dynamicOptions,
        restartSize: values.restartSize,
        // alertEmail: values.alertEmail || null,
        alertId: values.alertId,
        description: values.description,
        k8sRestExposedType: values.k8sRestExposedType,
        k8sNamespace: values.k8sNamespace || null,
        clusterId: values.clusterId || null,
        flinkClusterId: values.flinkClusterId || null,
        flinkImage: values.flinkImage || null,
        resourceFrom: values.resourceFrom,
        yarnSessionClusterId: values.yarnSessionClusterId || null,
      };
      if (params.executionMode === 6) {
        Object.assign(params, {
          k8sPodTemplate: k8sTemplate.podTemplate,
          k8sJmPodTemplate: k8sTemplate.jmPodTemplate,
          k8sTmPodTemplate: k8sTemplate.tmPodTemplate,
        });
      }
      handleUpdateApp(params);
    } catch (error) {
      submitLoading.value = false;
    }
  }

  /* Submit an update */
  async function handleUpdateApp(params: Recordable) {
    const updated = await fetchUpdate(params);
    if (updated) {
      createMessage.success('update successful');
      close(undefined, { path: '/flink/app' });
    }
  }

  onMounted(async () => {
    if (!route?.query?.appId) {
      close(undefined, { path: '/flink/app' });
      createMessage.warning('appid can not be empty');
      return;
    }
    const value = await handleGetApplication();
    setFieldsValue(value);
    if (app.resourceFrom === 1) {
      jars.value = await fetchListJars({
        id: app.projectId,
        module: app.module,
      });
    }
    handleReset();
  });
</script>
<template>
  <PageWrapper contentBackground>
    <BasicForm @register="registerForm" @submit="handleAppUpdate" :schemas="getEditFlinkFormSchema">
      <template #podTemplate>
        <PomTemplateTab
          v-model:podTemplate="k8sTemplate.podTemplate"
          v-model:jmPodTemplate="k8sTemplate.jmPodTemplate"
          v-model:tmPodTemplate="k8sTemplate.tmPodTemplate"
        />
      </template>
      <template #uploadJobJar>
        <UploadJobJar :custom-request="handleCustomJobRequest" v-model:loading="uploadLoading">
          <template #uploadInfo>
            <Alert v-if="uploadJar" class="uploadjar-box" type="info">
              <template #message>
                <span class="tag-dependency-pom">
                  {{ uploadJar }}
                </span>
              </template>
            </Alert>
          </template>
        </UploadJobJar>
      </template>
      <template #formFooter>
        <div class="flex items-center w-full justify-center">
          <a-button @click="close(undefined, { path: '/flink/app' })">
            {{ t('common.cancelText') }}
          </a-button>
          <a-button class="ml-4" :loading="submitLoading" type="primary" @click="submit()">
            {{ t('common.submitText') }}
          </a-button>
        </div>
      </template>
    </BasicForm>
  </PageWrapper>
</template>
<style scoped></style>
