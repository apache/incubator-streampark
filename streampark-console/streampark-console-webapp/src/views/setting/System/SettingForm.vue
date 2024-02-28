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
  import { SystemSetting } from '/@/api/flink/setting/types/setting.type';
  import {
    fetchEmailConfig,
    fetchEmailUpdate,
    fetchDockerConfig,
    fetchDockerUpdate,
  } from '/@/api/flink/setting';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { isNullOrUnDef } from '/@/utils/is';

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();
  const { t } = useI18n();
  defineOptions({ name: 'DockerSetting' });

  const settings = ref<SystemSetting[]>();
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
      let res: any;
      type.value = data.type;

      if (data.type === 'docker') {
        res = await fetchDockerConfig();
      } else if (data.type === 'email') {
        res = await fetchEmailConfig();
      }
      settings.value = res
        ?.filter((i) => i.settingKey.startsWith(data.type))
        ?.sort((a, b) => a.orderNum - b.orderNum);

      await setFieldsValue(
        data.settings.reduce((pre, cur) => {
          if (!isNullOrUnDef(cur.settingValue)) pre[cur.settingKey] = cur.settingValue;
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
    return (
      settings.value?.map((item) => {
        const component =
          item.type === 1
            ? item.settingKey.endsWith('password')
              ? 'InputPassword'
              : 'Input'
            : 'Switch';

        const getField = () => {
          if (type.value == 'docker') {
            return item.settingKey.replaceAll('docker.register.', '');
          }
          if (type.value == 'email') {
            return item.settingKey.replaceAll('alert.email.', '');
          }
          return item.settingKey;
        };
        return {
          field: getField(),
          label: item.settingName,
          helpMessage: item.description,
          component,
          componentProps:
            component == 'Switch'
              ? {
                  checkedChildren: 'ON',
                  unCheckedChildren: 'OFF',
                }
              : {
                  autocomplete: 'new-password',
                },
          //TODO Required or not according to the back-end interface
          required: item.type == 1,
        };
      }) ?? []
    );
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
    settings.value = [];
  }
</script>

<template>
  <BasicModal @register="registerModal" :width="750" @ok="handleOk" :after-close="afterClose">
    <template #title>
      <SettingTwoTone class="ml-10px" theme="twoTone" two-tone-color="#4a9ff5" />
      {{ title }}
    </template>
    <BasicForm @register="registerForm" :schemas="formSchemas" />
  </BasicModal>
</template>
