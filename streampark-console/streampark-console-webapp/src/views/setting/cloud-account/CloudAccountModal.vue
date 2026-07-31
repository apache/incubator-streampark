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
  import { computed, ref, unref } from 'vue';
  import { BasicForm, type FormSchema, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { fetchCloudAccountCreate, fetchCloudAccountUpdate } from '/@/api/setting/cloudAccount';
  import type { CloudAccount, CloudAccountForm } from '/@/api/setting/cloudAccount.type';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';

  defineOptions({ name: 'CloudAccountModal' });
  const emit = defineEmits(['register', 'success']);
  const { t } = useI18n();
  const { createMessage } = useMessage();
  const isUpdate = ref(false);

  const schemas = computed<FormSchema[]>(() => [
    { field: 'id', label: 'id', component: 'Input', show: false },
    { field: 'version', label: 'version', component: 'InputNumber', show: false },
    {
      field: 'accountName',
      label: t('setting.cloudAccount.accountName'),
      component: 'Input',
      required: true,
      rules: [{ max: 128 }],
    },
    {
      field: 'providerType',
      label: t('setting.cloudAccount.provider'),
      component: 'Select',
      defaultValue: 'VOLCENGINE',
      componentProps: {
        disabled: unref(isUpdate),
        options: [{ label: 'Volcengine', value: 'VOLCENGINE' }],
      },
      required: true,
    },
    {
      field: 'region',
      label: t('setting.cloudAccount.region'),
      component: 'Input',
      required: true,
      rules: [{ max: 64 }],
    },
    {
      field: 'accessKey',
      label: t('setting.cloudAccount.accessKey'),
      component: 'InputPassword',
      helpMessage: unref(isUpdate)
        ? t('setting.cloudAccount.rotateHint')
        : t('setting.cloudAccount.secretHint'),
      componentProps: {
        autocomplete: 'new-password',
        visibilityToggle: false,
      },
      dynamicRules: () => [{ required: !unref(isUpdate), max: 1024 }],
    },
    {
      field: 'secretKey',
      label: t('setting.cloudAccount.secretKey'),
      component: 'InputPassword',
      helpMessage: t('setting.cloudAccount.secretHint'),
      componentProps: {
        autocomplete: 'new-password',
        visibilityToggle: false,
      },
      dynamicRules: () => [{ required: !unref(isUpdate), max: 1024 }],
    },
    {
      field: 'description',
      label: t('common.description'),
      component: 'InputTextArea',
      componentProps: { rows: 3 },
      rules: [{ max: 255 }],
    },
  ]);

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    name: 'CloudAccountForm',
    layout: 'vertical',
    showActionButtonGroup: false,
    baseColProps: { span: 22, offset: 1 },
  });

  const [registerModal, { closeModal, setModalProps }] = useModalInner(
    async (data: { isUpdate: boolean; record?: CloudAccount }) => {
      await resetFields();
      isUpdate.value = data.isUpdate;
      setModalProps({ confirmLoading: false });
      if (data.record) {
        await setFieldsValue({
          id: data.record.id,
          version: data.record.version,
          accountName: data.record.accountName,
          providerType: data.record.providerType,
          region: data.record.region,
          description: data.record.description,
          accessKey: undefined,
          secretKey: undefined,
        });
      }
    },
  );

  const title = computed(() =>
    unref(isUpdate) ? t('setting.cloudAccount.edit') : t('setting.cloudAccount.create'),
  );

  async function handleSubmit() {
    try {
      const values = (await validate()) as CloudAccountForm;
      const hasAccessKey = !!values.accessKey;
      const hasSecretKey = !!values.secretKey;
      if (hasAccessKey !== hasSecretKey) {
        createMessage.error(t('setting.cloudAccount.rotateHint'));
        return;
      }
      setModalProps({ confirmLoading: true });
      if (unref(isUpdate)) {
        delete values.providerType;
        if (!hasAccessKey) {
          delete values.accessKey;
          delete values.secretKey;
        }
        await fetchCloudAccountUpdate(values);
      } else {
        delete values.id;
        delete values.version;
        await fetchCloudAccountCreate(values);
      }
      closeModal();
      emit('success', unref(isUpdate));
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<template>
  <BasicModal
    v-bind="$attrs"
    centered
    :title="title"
    :width="640"
    show-footer
    @register="registerModal"
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" :schemas="schemas" />
  </BasicModal>
</template>
