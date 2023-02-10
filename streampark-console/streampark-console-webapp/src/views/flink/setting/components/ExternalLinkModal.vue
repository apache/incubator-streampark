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
  import { useMessage } from '/@/hooks/web/useMessage';
  import { ExternalLink } from '/@/api/flink/setting/types/externalLink.type';
  import { ResultEnum } from '/@/enums/httpEnum';

  export default defineComponent({
    name: 'ExternalLinkModal',
  });
</script>
<script lang="ts" setup>
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { Icon } from '/@/components/Icon';
  import {
    fetchExternalLinkCreate,
    fetchExternalLinkUpdate,
  } from '/@/api/flink/setting/externalLink';
  import { h, ref } from 'vue';

  const emit = defineEmits(['reload', 'register']);
  const { t } = useI18n();
  const externalLinkId = ref<string | null>(null);
  const { Swal } = useMessage();
  const [registerForm, { validate, resetFields, setFieldsValue }] = useForm({
    labelWidth: 120,
    schemas: [
      {
        field: 'name',
        label: t('flink.setting.externalLink.form.name'),
        component: 'Input',
        componentProps: {
          placeholder: t('flink.setting.externalLink.form.namePlaceholder'),
          allowClear: true,
        },
        rules: [
          {
            required: true,
            message: t('flink.setting.externalLink.form.nameIsRequired'),
          },
        ],
      },
      {
        field: 'linkUrl',
        label: t('flink.setting.externalLink.form.linkUrl'),
        component: 'Input',
        componentProps: {
          placeholder: t('flink.setting.externalLink.form.linkUrlPlaceholder'),
          allowClear: true,
        },
        afterItem: () =>
          h(
            'span',
            { class: 'conf-switch' },
            'Supported variables: {job_id}, {yarn_id}, {job_name},Example: https://grafana/flink-monitoring?var-JobId=var-JobId={job_id}',
          ),
        rules: [
          {
            required: true,
            message: t('flink.setting.externalLink.form.linkUrlIsRequired'),
          },
        ],
      },
      {
        field: 'imageUrl',
        label: t('flink.setting.externalLink.form.imageUrl'),
        component: 'Input',
        componentProps: {
          placeholder: t('flink.setting.externalLink.form.imageUrlPlaceholder'),
          allowClear: true,
        },
        rules: [
          {
            required: true,
            message: t('flink.setting.externalLink.form.imageUrlIsRequired'),
          },
        ],
      },
    ],
    colon: true,
    showActionButtonGroup: false,
    labelCol: { lg: { span: 6, offset: 0 }, sm: { span: 6, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 4, offset: 0 } },
    baseColProps: { span: 24 },
  });

  const [registerModal, { closeModal, changeOkLoading }] = useModalInner((data: ExternalLink) => {
    resetFields();
    externalLinkId.value = '';
    if (data && Object.keys(data).length > 0) {
      externalLinkId.value = data.id;
      setFieldsValue(data);
    }
  });

  // submit new external link
  async function handleSubmit() {
    try {
      changeOkLoading(true);
      const formValue = await validate();
      if (externalLinkId.value) {
        const { data } = await fetchExternalLinkUpdate({
          id: externalLinkId.value,
          name: formValue.name,
          imageUrl: formValue.imageUrl,
          linkUrl: formValue.linkUrl,
        });
        if (data.code === ResultEnum.SUCCESS) {
          Swal.fire({
            icon: 'success',
            title: t('flink.setting.externalLink.operateMessage.updateLinkSuccessful'),
            showConfirmButton: false,
            timer: 2000,
          });
        } else {
          Swal.fire({
            icon: 'error',
            title: t('flink.setting.externalLink.operateMessage.updateLinkFailed'),
            text: data['message'].replaceAll(/\[StreamPark]/g, ''),
          });
        }
      } else {
        const { data } = await fetchExternalLinkCreate(formValue);
        if (data.code === ResultEnum.SUCCESS) {
          Swal.fire({
            icon: 'success',
            title: t('flink.setting.externalLink.operateMessage.createLinkSuccessful'),
            showConfirmButton: false,
            timer: 2000,
          });
        } else {
          Swal.fire({
            title: t('flink.setting.externalLink.operateMessage.createLinkFailed'),
            text: data['message'].replaceAll(/\[StreamPark]/g, ''),
            icon: 'error',
          });
        }
      }
      closeModal();
      emit('reload');
    } catch (error: any) {
      console.error(error);
    } finally {
      changeOkLoading(false);
    }
  }
</script>
<template>
  <BasicModal
    @register="registerModal"
    :ok-text="t('common.submitText')"
    v-bind="$attrs"
    @ok="handleSubmit"
  >
    <template #title>
      <Icon icon="ant-design:link-outlined" color="blue" />
      {{ t('flink.setting.externalLink.externalLinkSetting') }}
    </template>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
