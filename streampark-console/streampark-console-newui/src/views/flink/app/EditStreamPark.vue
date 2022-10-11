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
    name: 'EditStreamPark',
  });
</script>
<script setup lang="ts" name="EditStreamPark">
  import { PageWrapper } from '/@/components/Page';
  import { BasicForm, useForm } from '/@/components/Form';
  import { onMounted, reactive, ref, nextTick, unref, defineComponent } from 'vue';
  import { AppListRecord } from '/@/api/flink/app/app.type';
  import configOptions from './data/option';
  import { fetchMain, fetchUpload, fetchUpdate, fetchGet } from '/@/api/flink/app/app';
  import { useRoute } from 'vue-router';
  import { handleSubmitParams } from './utils';
  import { fetchFlinkHistory } from '/@/api/flink/app/flinkSql';
  import { decodeByBase64 } from '/@/utils/cipher';
  import PomTemplateTab from './components/PodTemplate/PomTemplateTab.vue';
  import UploadJobJar from './components/UploadJobJar.vue';
  import FlinkSqlEditor from './components/flinkSql.vue';
  import Dependency from './components/Dependency.vue';
  import Different from './components/AppDetail/Different.vue';
  import Mergely from './components/Mergely.vue';
  import AppConf from './components/AppConf';
  import UseSysHadoopConf from './components/UseSysHadoopConf.vue';
  import CompareConf from './components/CompareConf';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { fetchConfHistory } from '/@/api/flink/config';
  import { useDrawer } from '/@/components/Drawer';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { useEditStreamParkSchema } from './hooks/useEditStreamPark';
  import { useEdit } from './hooks/useEdit';
  import { useI18n } from '/@/hooks/web/useI18n';

  const route = useRoute();
  const { close } = useTabs();
  const { t } = useI18n();
  const { createMessage } = useMessage();
  const app = reactive<Partial<AppListRecord>>({});
  const selectAlertId = ref<string>('');
  const flinkSqlHistory = ref<any[]>([]);
  const submitLoading = ref<boolean>(false);

  const configVersions = ref<Array<{ id: string }>>([]);

  const uploadLoading = ref(false);
  const uploadJar = ref('');
  const flinkSql = ref();
  const dependencyRef = ref();

  const k8sTemplate = reactive({
    podTemplate: '',
    jmPodTemplate: '',
    tmPodTemplate: '',
  });
  const { handleResetApplication, defaultOptions } = useEdit();
  const { getEditStreamParkFormSchema, registerDifferentDrawer, alerts, flinkEnvs } =
    useEditStreamParkSchema(configVersions, flinkSqlHistory, dependencyRef);

  const [registerForm, { setFieldsValue, getFieldsValue, submit }] = useForm({
    labelWidth: 120,
    colon: true,
    baseColProps: { span: 24 },
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
    showActionButtonGroup: false,
  });

  const [registerConfDrawer, { openDrawer: openConfDrawer }] = useDrawer();

  /* Form reset */
  function handleReset(executionMode?: string) {
    nextTick(() => {
      const resetParams = handleResetApplication();
      const defaultParams = {
        jobName: app.jobName,
        tags: app.tags,
        args: app.args,
        description: app.description,
        dynamicOptions: app.dynamicOptions,
        resolveOrder: app.resolveOrder,
        versionId: app.versionId || null,
        k8sRestExposedType: app.k8sRestExposedType,
        yarnQueue: app.yarnQueue,
        restartSize: app.restartSize,
        alertId: selectAlertId,
        cpMaxFailureInterval: app.cpMaxFailureInterval,
        cpFailureRateInterval: app.cpFailureRateInterval,
        cpFailureAction: app.cpFailureAction,
        clusterId: app.clusterId,
        flinkClusterId: app.flinkClusterId,
        flinkImage: app.flinkImage,
        k8sNamespace: app.k8sNamespace,
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
      setFieldsValue({ jar: uploadJar.value, mainClass: res });
    } catch (error) {
      console.error(error);
    } finally {
      uploadLoading.value = false;
    }
  }

  /* Handling update parameters */
  async function handleAppUpdate(values) {
    try {
      submitLoading.value = true;
      if (app.jobType === 1) {
        handleSubmitCustomJob(values);
      } else {
        if (app.jobType === 2) {
          if (values.flinkSql == null || values.flinkSql.trim() === '') {
            createMessage.warning('Flink Sql is required');
          } else {
            const access = await flinkSql?.value?.handleVerifySql();
            if (!access) return;
            handleSubmitSQL(values);
          }
        }
      }
    } catch (error) {
      console.error(error);
      submitLoading.value = false;
    }
  }

  function handleSubmitSQL(values) {
    try {
      // Trigger a pom confirmation operation.
      unref(dependencyRef)?.handleApplyPom();
      // common params...
      const dependency: { pom?: any; jar?: any } = {};
      if (values.dependency !== null && values.dependency.length > 0) {
        Object.assign(dependency, { pom: values.dependency });
      }
      if (values.uploadJars != null && values.uploadJars.length > 0) {
        Object.assign(dependency, { jar: values.dependency });
      }

      let config = values.configOverride;
      if (config != null && config.trim() !== '') {
        config = decodeByBase64(config);
      } else {
        config = null;
      }
      const params = {
        id: app.id,
        sqlId: values.sqlId || app.sqlId || null,
        flinkSql: values.flinkSql,
        config,
        format: values.isSetConfig ? 1 : null,
        dependency:
          dependency.pom === undefined && dependency.jar === undefined
            ? null
            : JSON.stringify(dependency),
      };
      handleSubmitParams(params, values, k8sTemplate);
      handleUpdateApp(params);
    } catch (error) {
      submitLoading.value = false;
    }
  }
  /* Submit an update */
  async function handleSubmitCustomJob(values: Recordable) {
    try {
      const format =
        values.strategy === 1 ? app.format : values.config.endsWith('.properties') ? 2 : 1;
      let config = values.configOverride || app.config;
      if (config != null && config.trim() !== '') {
        config = decodeByBase64(config);
      } else {
        config = null;
      }
      const configId = values.strategy === 1 ? app.configId : null;
      const params = {
        id: app.id,
        format: format,
        configId: configId,
        config,
      };
      handleSubmitParams(params, values, k8sTemplate);
      handleUpdateApp(params);
    } catch (error) {
      console.error('error', error);
      submitLoading.value = false;
    }
  }

  /* Send submission interface */
  async function handleUpdateApp(params: Recordable) {
    try {
      const updated = await fetchUpdate(params);
      if (updated) {
        close(undefined, { path: '/flink/app' });
      }
    } catch (error) {
      console.error('error', error);
    } finally {
      submitLoading.value = false;
    }
  }

  /* initialization information */
  async function handleStreamParkInfo() {
    const appId = route.query.appId;
    const res = await fetchGet({ id: appId as string });
    let configId = '';
    const confVersion = await fetchConfHistory({ id: route.query.appId });
    confVersion.forEach((value) => {
      if (value.effective) {
        configId = value.id;
      }
    });
    configVersions.value = confVersion;
    Object.assign(app, res);
    Object.assign(defaultOptions, JSON.parse(app.options || '{}'));
    setFieldsValue({
      jobType: res.jobType,
      executionMode: res.executionMode,
      flinkSql: decodeByBase64(res.flinkSql),
      dependency: res.dependency,
      module: res.module,
      configId,
      sqlId: app.sqlId,
      versionId: app.versionId,
    });
    nextTick(() => {
      unref(flinkSql)?.setContent(decodeByBase64(res.flinkSql));
    });
    if (app.jobType === 2) {
      const res = await fetchFlinkHistory({ id: appId });
      flinkSqlHistory.value = res;
    }
    if (app.alertId) {
      selectAlertId.value = unref(alerts).filter((t) => t.id == app.alertId)[0]?.id;
    }
    let isSetConfig = false;
    let configOverride = '';
    if (app.config && app.config.trim() !== '') {
      configOverride = decodeByBase64(app.config);
      isSetConfig = true;
    }
    const defaultFormValue = { isSetConfig, configOverride };
    configOptions.forEach((item) => {
      Object.assign(defaultFormValue, {
        [item.key]: item.defaultValue,
      });
    });
    setFieldsValue(defaultFormValue);
    handleReset();
  }

  function handleMergely(configOverride) {
    openConfDrawer(true, {
      configOverride,
    });
  }

  function handleEditConfClose() {
    const formValue = getFieldsValue();
    if (!formValue.configOverride) {
      setFieldsValue({ isSetConfig: false });
    }
  }
  onMounted(() => {
    if (!route?.query?.appId) {
      close(undefined, { path: '/flink/app' });
      createMessage.warning('appid can not be empty');
      return;
    }
    handleStreamParkInfo();
  });
</script>
<template>
  <PageWrapper contentBackground content-class="p-26px app_controller">
    <BasicForm
      @register="registerForm"
      @submit="handleAppUpdate"
      :schemas="getEditStreamParkFormSchema"
    >
      <template #podTemplate>
        <PomTemplateTab
          v-model:podTemplate="k8sTemplate.podTemplate"
          v-model:jmPodTemplate="k8sTemplate.jmPodTemplate"
          v-model:tmPodTemplate="k8sTemplate.tmPodTemplate"
        />
      </template>
      <template #uploadJobJar>
        <UploadJobJar :custom-request="handleCustomJobRequest" v-model:loading="uploadLoading" />
      </template>
      <template #flinkSql="{ model, field }">
        <FlinkSqlEditor
          ref="flinkSql"
          v-model:value="model[field]"
          :versionId="model['versionId']"
        />
      </template>
      <template #dependency="{ model, field }">
        <Dependency
          ref="dependencyRef"
          v-model:value="model[field]"
          :form-model="model"
          :flink-envs="flinkEnvs"
        />
      </template>
      <template #appConf="{ model }">
        <AppConf :model="model" :configVersions="configVersions" @open-mergely="handleMergely" />
      </template>
      <template #compareConf="{ model }">
        <CompareConf v-model:value="model.compareConf" :configVersions="configVersions" />
      </template>
      <template #useSysHadoopConf="{ model, field }">
        <UseSysHadoopConf v-model:hadoopConf="model[field]" />
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
    <Mergely
      @ok="(data) => setFieldsValue(data)"
      @close="handleEditConfClose"
      @register="registerConfDrawer"
    />
    <Different @register="registerDifferentDrawer" />
  </PageWrapper>
</template>
<style lang="less">
  @import url('./styles/Add.less');
</style>
