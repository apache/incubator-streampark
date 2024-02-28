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
  import { computed, ref } from 'vue';
  import { SettingTwoTone } from '@ant-design/icons-vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import {
    fetchEmailConfig,
    fetchEmailUpdate,
    fetchDockerConfig,
    fetchDockerUpdate,
  } from '/@/api/flink/setting';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { isNullOrUnDef } from '/@/utils/is';
  import { settingFormSchema } from './config';

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();
  const { t } = useI18n();
  defineOptions({ name: 'DockerSetting' });

  const settingConfig = ref<Recordable>({});
  const type = ref('docker');
  const title = computed(() => {
    if (type.value == 'docker') return t('setting.system.systemSettingItems.dockerSetting.name');
    if (type.value == 'email') return t('setting.system.systemSettingItems.emailSetting.name');
    return '';
  });
  const [registerModal, { closeModal, changeLoading }] = useModalInner(async (data) => {
    try {
      changeLoading(true);
      await resetFields();
      type.value = data.type;

      if (data.type === 'docker') {
        settingConfig.value = await fetchDockerConfig();
      } else if (data.type === 'email') {
        settingConfig.value = await fetchEmailConfig();
      }

      await setFieldsValue(
        Object.keys(settingConfig.value).reduce((pre, cur) => {
          if (!isNullOrUnDef(settingConfig.value[cur])) pre[cur] = settingConfig.value[cur];
          return pre;
        }, {}),
      );
    } catch (error) {
      console.error(error);
    } finally {
      changeLoading(false);
    }
  });
  const [registerForm, { validate, setFieldsValue, resetFields }] = useForm({
    colon: true,
    labelWidth: 180,
    name: 'SettingForm',
    labelCol: { span: 8 },
    wrapperCol: { span: 15 },
    baseColProps: { span: 23 },
    showActionButtonGroup: false,
  });
  const formSchemas = computed((): FormSchema[] => {
    if (Reflect.has(settingFormSchema, type.value)) {
      return settingFormSchema[type.value];
    }
    return Object.keys(settingConfig.value).map((key) => {
      return {
        field: key,
        label: key,
        component: 'Input',
      };
    });
  });
  async function handleOk() {
    try {
      const formData = await validate();
      if (type.value === 'docker') await fetchDockerUpdate(formData);
      if (type.value === 'email') await fetchEmailUpdate(formData);
      createMessage.success(t('setting.system.update.success'));
      closeModal();
      emit('success');
    } catch (error) {
      console.error(error);
    }
  }
  async function afterClose() {
    settingConfig.value = [];
  }
</script>

<template>
  <BasicModal
    @register="registerModal"
    :width="750"
    @ok="handleOk"
    :after-close="afterClose"
    centered
  >
    <template #title>
      <SettingTwoTone class="ml-10px" theme="twoTone" two-tone-color="#4a9ff5" />
      {{ title }}
    </template>
    <BasicForm @register="registerForm" :schemas="formSchemas" />
  </BasicModal>
</template>
