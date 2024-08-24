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
<script setup lang="ts">
  import { useGo } from '/@/hooks/web/usePage';
  import ProgramArgs from './components/ProgramArgs.vue';
  import { onMounted, ref } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { createAsyncComponent } from '/@/utils/factory/createAsyncComponent';

  import { BasicForm, useForm } from '/@/components/Form';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createLocalStorage } from '/@/utils/cache';
  import { buildUUID } from '/@/utils/uuid';
  import { useI18n } from '/@/hooks/web/useI18n';
  import VariableReview from './components/VariableReview.vue';
  import { encryptByBase64 } from '/@/utils/cipher';
  import { AppTypeEnum, JobTypeEnum, ResourceFromEnum } from '/@/enums/flinkEnum';
  import { useSparkSchema } from './hooks/useAppFormSchema';
  import { fetchCreateSparkApp } from '/@/api/spark/app';
  import { SparkApplication } from '/@/api/spark/app.type';

  const SparkSqlEditor = createAsyncComponent(() => import('./components/SparkSql.vue'), {
    loading: true,
  });

  defineOptions({
    name: 'SparkApplicationAction',
  });
  const go = useGo();
  const sparkSql = ref();
  const submitLoading = ref(false);

  const { t } = useI18n();
  const { createMessage } = useMessage();
  const ls = createLocalStorage();

  const { formSchema, sparkEnvs, suggestions } = useSparkSchema();

  const [registerAppForm, { setFieldsValue, submit }] = useForm({
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
    baseColProps: { span: 24 },
    colon: true,
    showActionButtonGroup: false,
  });

  const [registerReviewDrawer, { openDrawer: openReviewDrawer }] = useDrawer();

  /* Initialize the form */
  async function handleInitForm() {
    const defaultValue = {};
    const v = sparkEnvs.value.filter((v) => v.isDefault)[0];
    if (v) {
      Object.assign(defaultValue, { versionId: v.id });
    }
    await setFieldsValue(defaultValue);
  }

  /* custom mode */
  async function handleCustomJobMode(values: Recordable) {
    const params = {
      jobType: JobTypeEnum.SQL,
      executionMode: values.executionMode,
      appType: AppTypeEnum.APACHE_SPARK,
      versionId: values.versionId,
      sparkSql: null,
      jar: values.teamResource,
      mainClass: values.mainClass,
      appName: values.jobName,
      tags: values.tags,
      yarnQueue: values.yarnQueue,
      resourceFrom: ResourceFromEnum.UPLOAD,
      config: null,
      appProperties: values.appProperties,
      appArgs: values.args,
      hadoopUser: values.hadoopUser,
      description: values.description,
    };
    handleCreateAction(params);
  }
  /* spark sql mode */
  async function handleSQLMode(values: Recordable) {
    let config = values.configOverride;
    if (config != null && config !== undefined && config.trim() != '') {
      config = encryptByBase64(config);
    } else {
      config = null;
    }
    handleCreateAction({
      jobType: JobTypeEnum.SQL,
      executionMode: values.executionMode,
      appType: AppTypeEnum.APACHE_SPARK,
      versionId: values.versionId,
      sparkSql: values.sparkSql,
      jar: null,
      mainClass: null,
      appName: values.jobName,
      tags: values.tags,
      yarnQueue: values.yarnQueue,
      resourceFrom: ResourceFromEnum.UPLOAD,
      config,
      appProperties: values.appProperties,
      appArgs: values.args,
      hadoopUser: values.hadoopUser,
      description: values.description,
    });
  }
  /* Submit to create */
  async function handleAppSubmit(formValue: Recordable) {
    try {
      submitLoading.value = true;
      if (formValue.jobType == JobTypeEnum.SQL) {
        if (formValue.sparkSql == null || formValue.sparkSql.trim() === '') {
          createMessage.warning(t('flink.app.editStreamPark.sparkSqlRequired'));
        } else {
          const access = await sparkSql?.value?.handleVerifySql();
          if (!access) {
            createMessage.warning(t('flink.app.editStreamPark.sqlCheck'));
            throw new Error(access);
          }
        }
        handleSQLMode(formValue);
      } else {
        handleCustomJobMode(formValue);
      }
    } catch (error) {
      submitLoading.value = false;
    }
  }
  /* send create request */
  async function handleCreateAction(params: Recordable) {
    const param: SparkApplication = {};
    for (const k in params) {
      const v = params[k];
      if (v != null && v !== undefined) {
        param[k] = v;
      }
    }
    const socketId = buildUUID();
    ls.set('DOWN_SOCKET_ID', socketId);
    Object.assign(param, { socketId });
    await fetchCreateSparkApp(param);
    submitLoading.value = false;
    go('/spark/app');
  }

  onMounted(async () => {
    handleInitForm();
  });
</script>

<template>
  <PageWrapper contentFullHeight contentBackground contentClass="p-26px app_controller">
    <BasicForm @register="registerAppForm" @submit="handleAppSubmit" :schemas="formSchema">
      <template #sparkSql="{ model, field }">
        <SparkSqlEditor
          ref="sparkSql"
          v-model:value="model[field]"
          :versionId="model['versionId']"
          :suggestions="suggestions"
          @preview="(value) => openReviewDrawer(true, { value, suggestions })"
        />
      </template>
      <template #args="{ model }">
        <template v-if="model.args !== undefined">
          <ProgramArgs
            v-model:value="model.args"
            :suggestions="suggestions"
            @preview="(value) => openReviewDrawer(true, { value, suggestions })"
          />
        </template>
      </template>
      <template #formFooter>
        <div class="flex items-center w-full justify-center">
          <a-button @click="go('/spark/app')">
            {{ t('common.cancelText') }}
          </a-button>
          <a-button class="ml-4" :loading="submitLoading" type="primary" @click="submit()">
            {{ t('common.submitText') }}
          </a-button>
        </div>
      </template>
    </BasicForm>
    <VariableReview @register="registerReviewDrawer" />
  </PageWrapper>
</template>
