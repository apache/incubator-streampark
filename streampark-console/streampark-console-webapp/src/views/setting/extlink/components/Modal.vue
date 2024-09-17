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
    name: 'ExternalLinkModal',
  });
</script>
<script lang="ts" setup>
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { Icon } from '/@/components/Icon';
  import { fetchExternalLinkCreate, fetchExternalLinkUpdate } from '/@/api/setting/externalLink';

  import { useMessage } from '/@/hooks/web/useMessage';
  import { ExternalLink } from '/@/api/setting/types/externalLink.type';
  import { ResultEnum } from '/@/enums/httpEnum';
  import { renderColorField, renderPreview } from './useExternalLink';

  import { h, ref } from 'vue';

  const DEFAULT_BADGE_COLOR = '#eb8c34';
  const emit = defineEmits(['reload', 'register']);
  const { t } = useI18n();
  const externalLinkId = ref<string | null>(null);
  const { Swal } = useMessage();
  const [registerForm, { validate, resetFields, setFieldsValue }] = useForm({
    schemas: [
      {
        field: 'badgeLabel',
        label: t('setting.externalLink.form.badgeLabel'),
        component: 'Input',
        componentProps: {
          placeholder: t('setting.externalLink.form.badgeLabelPlaceholder'),
          allowClear: true,
        },
      },
      {
        field: 'badgeName',
        label: t('setting.externalLink.form.badgeName'),
        component: 'Input',
        componentProps: {
          placeholder: t('setting.externalLink.form.badgeNamePlaceholder'),
          allowClear: true,
        },
        rules: [
          {
            required: true,
            message: t('setting.externalLink.form.badgeNameIsRequired'),
          },
        ],
      },
      {
        field: 'linkUrl',
        label: t('setting.externalLink.form.linkUrl'),
        component: 'Input',
        componentProps: {
          placeholder: t('setting.externalLink.form.linkUrlPlaceholder'),
          allowClear: true,
        },
        afterItem: () =>
          h(
            'span',
            { class: 'pop-tip' },
            'Supported variables: {job_id}, {yarn_id}, {job_name}, Example: https://grafana/flink-monitoring?var-JobId=var-JobId={job_id}',
          ),
        rules: [
          {
            required: true,
            message: t('setting.externalLink.form.linkUrlIsRequired'),
          },
        ],
      },
      {
        field: 'badgeColor',
        label: t('setting.externalLink.form.badgeColor'),
        component: 'Input',
        defaultValue: DEFAULT_BADGE_COLOR,
        render: ({ model, field }) => renderColorField(model, field),
        rules: [
          {
            required: true,
            message: t('setting.externalLink.form.badgeColorIsRequired'),
          },
        ],
      },
      {
        field: 'preview',
        label: t('setting.externalLink.form.badgePreview'),
        component: 'Input',
        render: ({ model }) => renderPreview(model),
      },
    ],
    colon: true,
    showActionButtonGroup: false,
    layout: 'vertical',
    baseColProps: { span: 22, offset: 1 },
  });

  const [registerModal, { closeModal, changeOkLoading }] = useModalInner((data: ExternalLink) => {
    resetFields();
    externalLinkId.value = '';
    if (data && Object.keys(data).length > 0) {
      externalLinkId.value = data.id || '';
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
          badgeLabel: formValue.badgeLabel,
          badgeColor: formValue.badgeColor,
          badgeName: formValue.badgeName,
          linkUrl: formValue.linkUrl,
        });
        if (data.code === ResultEnum.SUCCESS) {
          Swal.fire({
            icon: 'success',
            title: t('setting.externalLink.operateMessage.updateLinkSuccessful'),
            showConfirmButton: false,
            timer: 2000,
          });
        } else {
          Swal.fire({
            icon: 'error',
            title: t('setting.externalLink.operateMessage.updateLinkFailed'),
            text: data['message'].replaceAll(/\[StreamPark]/g, ''),
          });
        }
      } else {
        const { data } = await fetchExternalLinkCreate(formValue);
        if (data.code === ResultEnum.SUCCESS) {
          Swal.fire({
            icon: 'success',
            title: t('setting.externalLink.operateMessage.createLinkSuccessful'),
            showConfirmButton: false,
            timer: 2000,
          });
        } else {
          Swal.fire({
            title: t('setting.externalLink.operateMessage.createLinkFailed'),
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
    :okButtonProps="{ class: 'e2e-extlink-submit-btn' }"
    :cancelButtonProps="{ class: 'e2e-extlink-cancel-btn' }"
    @register="registerModal"
    :width="600"
    :ok-text="t('common.submitText')"
    v-bind="$attrs"
    @ok="handleSubmit"
  >
    <template #title>
      <Icon icon="ant-design:link-outlined" />
      {{ t('setting.externalLink.externalLinkSetting') }}
    </template>
    <div class="mt-18px">
      <BasicForm @register="registerForm" />
    </div>
  </BasicModal>
</template>
