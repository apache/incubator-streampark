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
    name: 'AppCreate',
  });
</script>
<script setup lang="ts" name="AppCreate">
  import { Switch, Alert } from 'ant-design-vue';
  import { onMounted, reactive, ref, unref } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { BasicForm, useForm } from '/@/components/Form';
  import { SettingTwoTone } from '@ant-design/icons-vue';
  import { useDrawer } from '/@/components/Drawer';
  import Mergely from './components/Mergely.vue';
  import { handleConfTemplate } from '/@/api/flink/config';
  import { decodeByBase64 } from '/@/utils/cipher';
  import FlinkSqlEditor from './components/flinkSql.vue';
  import Dependency from './components/Dependency.vue';
  import UseSysHadoopConf from './components/UseSysHadoopConf.vue';
  import PomTemplateTab from './components/PodTemplate/PomTemplateTab.vue';
  import UploadJobJar from './components/UploadJobJar.vue';
  import { fetchAppConf, fetchCreate, fetchMain, fetchUpload } from '/@/api/flink/app/app';
  import options from './data/option';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { useCreateSchema } from './hooks/useCreateSchema';
  import { handleSubmitParams } from './utils';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createLocalStorage } from '/@/utils/cache';
  import { buildUUID } from '/@/utils/uuid';
  import { useI18n } from '/@/hooks/web/useI18n';

  const { close } = useTabs();
  const flinkSql = ref();
  const dependencyRef = ref();
  const uploadLoading = ref(false);
  const uploadJar = ref('');
  const submitLoading = ref(false);

  const { t } = useI18n();
  const { createMessage } = useMessage();
  const optionsKeyMapping = new Map();
  const ls = createLocalStorage();
  options.forEach((item) => {
    optionsKeyMapping.set(item.key, item);
  });

  const k8sTemplate = reactive({
    podTemplate: '',
    jmPodTemplate: '',
    tmPodTemplate: '',
  });

  const { getCreateFormSchema, flinkEnvs, flinkClusters } = useCreateSchema(dependencyRef);
  const [registerAppForm, { setFieldsValue, getFieldsValue, submit }] = useForm({
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
    baseColProps: { span: 24 },
    colon: true,
    showActionButtonGroup: false,
  });

  const [registerConfDrawer, { openDrawer: openConfDrawer }] = useDrawer();

  /* Initialize the form */
  async function handleInitForm() {
    const defaultValue = {
      resolveOrder: 0,
      k8sRestExposedType: 0,
      restartSize: 1,
    };
    options.forEach((item) => {
      defaultValue[item.key] = item.defaultValue;
    });
    const v = flinkEnvs.value.filter((v) => v.isDefault)[0];
    if (v) {
      Object.assign(defaultValue, { versionId: v.id });
    }

    setFieldsValue(defaultValue);
  }

  /* Open the sqlConf drawer */
  async function handleSQLConf(checked: boolean, model: Recordable) {
    if (checked) {
      if (model.configOverride != null) {
        openConfDrawer(true, {
          configOverride: model.configOverride,
        });
      } else {
        const res = await handleConfTemplate();
        openConfDrawer(true, {
          configOverride: decodeByBase64(res),
        });
      }
    } else {
      openConfDrawer(false);
      setFieldsValue({ isSetConfig: false, configOverride: null });
    }
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
      setFieldsValue({ mainClass: res });
    } catch (error) {
      console.error(error);
    } finally {
      uploadLoading.value = false;
    }
  }

  function handleEditConfClose() {
    const formValue = getFieldsValue();
    if (!formValue.configOverride) {
      setFieldsValue({ isSetConfig: false });
    }
  }

  function handleCluster(values) {
    const cluster =
      unref(flinkClusters).filter((c) => {
        if (values.flinkClusterId) {
          return c.clusterId === values.flinkClusterId && c.clusterState === 1;
        } else {
          return c.clusterId === values.yarnSessionClusterId && c.clusterState === 1;
        }
      })[0] || null;
    if (cluster) {
      Object.assign(values, {
        clusterId: cluster.id,
        flinkClusterId: cluster.id,
        yarnSessionClusterId: cluster.clusterId,
      });
    }
  }

  /* custom mode */
  async function handleSubmitCustomJob(values) {
    handleCluster(values);
    const params = {
      jobType: 1,
      projectId: values.project || null,
      module: values.module || null,
      appType: values.appType,
    };
    handleSubmitParams(params, values, k8sTemplate);

    // common params...
    const resourceFrom = values.resourceFrom;
    if (resourceFrom != null) {
      if (resourceFrom === 'cvs') {
        params['resourceFrom'] = 1;
        //streampark flink
        if (values.appType === 1) {
          const configVal = values['config'];
          params['format'] = configVal.endsWith('.properties') ? 2 : 1;
          if (values.configOverride == null) {
            const res = await fetchAppConf({
              config: configVal,
            });
            params['config'] = res;
            handleCreateApp(params);
          } else {
            params['config'] = decodeByBase64(values.configOverride);
            handleCreateApp(params);
          }
        } else {
          params['jar'] = values.jar || null;
          params['mainClass'] = values.mainClass || null;
          handleCreateApp(params);
        }
      } else {
        // from upload
        Object.assign(params, {
          resourceFrom: 2,
          appType: 2,
          jar: unref(uploadJar),
          mainClass: values.mainClass,
        });
        handleCreateApp(params);
      }
    }
  }
  /* flink sql mode */
  function handleSubmitSQL(values) {
    // Trigger a pom confirmation operation.
    unref(dependencyRef)?.handleApplyPom();
    // common params...
    const dependency: { pom?: string; jar?: string } = {};
    if (values.dependency && values.dependency.length > 0) {
      Object.assign(dependency, {
        pom: values.dependency,
      });
    }
    if (values.uploadJars && values.uploadJars.length > 0) {
      Object.assign(dependency, {
        jar: values.dependency,
      });
    }

    let config = values.configOverride;
    if (config != null && config !== undefined && config.trim() != '') {
      config = decodeByBase64(config);
    } else {
      config = null;
    }

    handleCluster(values);
    const params = {
      jobType: 2,
      flinkSql: values.flinkSql,
      appType: 1,
      config: config,
      format: values.isSetConfig ? 1 : null,
      dependency:
        dependency.pom === undefined && dependency.jar === undefined
          ? null
          : JSON.stringify(dependency),
    };
    handleSubmitParams(params, values, k8sTemplate);
    handleCreateApp(params);
  }
  /* Submit to create */
  function handleAppCreate(formValue: Recordable) {
    try {
      submitLoading.value = true;
      if (formValue.jobType === 'sql') {
        if (formValue.flinkSql == null || formValue.flinkSql.trim() === '') {
          createMessage.warning('Flink Sql is required');
        } else {
          const access = flinkSql?.value?.handleVerifySql();
          if (!access) return;
        }
      }
      if (formValue.jobType === 'customcode') {
        handleSubmitCustomJob(formValue);
      } else {
        handleSubmitSQL(formValue);
      }
    } catch (error) {
      submitLoading.value = false;
    }
  }
  /* send create request */
  async function handleCreateApp(params: Recordable) {
    const param = {};
    for (const k in params) {
      const v = params[k];
      if (v != null && v !== undefined) {
        param[k] = v;
      }
    }
    const socketId = buildUUID();
    ls.set('DOWN_SOCKET_ID', socketId);
    Object.assign(param, { socketId });
    const { data } = await fetchCreate(param);
    if (data.data) {
      close(undefined, { path: '/flink/app' });
    } else {
      createMessage.error(data.message);
    }
  }

  onMounted(async () => {
    handleInitForm();
  });
</script>

<template>
  <PageWrapper contentFullHeight contentBackground contentClass="p-24px app_controller">
    <BasicForm @register="registerAppForm" @submit="handleAppCreate" :schemas="getCreateFormSchema">
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
      <template #isSetConfig="{ model, field }">
        <Switch checked-children="ON" un-checked-children="OFF" v-model:checked="model[field]" />
        <SettingTwoTone
          v-if="model[field]"
          class="ml-10px"
          theme="twoTone"
          two-tone-color="#4a9ff5"
          @click="handleSQLConf(true, model)"
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
      <template #podTemplate>
        <PomTemplateTab
          v-model:podTemplate="k8sTemplate.podTemplate"
          v-model:jmPodTemplate="k8sTemplate.jmPodTemplate"
          v-model:tmPodTemplate="k8sTemplate.tmPodTemplate"
        />
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
  </PageWrapper>
</template>
<style lang="less">
  @import url('./styles/Add.less');
</style>
