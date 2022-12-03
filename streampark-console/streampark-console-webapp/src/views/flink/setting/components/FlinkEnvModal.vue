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
  import { useI18n } from '/@/hooks/web/useI18n';

  export default defineComponent({
    name: 'FlinkModal',
  });
</script>
<script lang="ts" setup name="FlinkModal">
  import { h, ref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { SvgIcon } from '/@/components/Icon';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    fetchExistsEnv,
    fetchFlinkCreate,
    fetchFlinkUpdate,
  } from '/@/api/flink/setting/flinkEnv';

  const emit = defineEmits(['reload', 'register']);
  const versionId = ref<string | null>(null);
  const { t } = useI18n();
  const { createConfirm, Swal, createMessage } = useMessage();
  const [registerForm, { setFieldsValue, validate, resetFields }] = useForm({
    labelWidth: 120,
    colon: true,
    showActionButtonGroup: false,
    labelCol: { lg: 7, sm: 7 },
    wrapperCol: { lg: 16, sm: 4 },
    baseColProps: { span: 24 },
    schemas: [
      {
        field: 'flinkName',
        label: t('flink.setting.flink.flinkName'),
        component: 'Input',
        componentProps: {
          placeholder: t('flink.setting.flink.flinkNamePlaceholder'),
          allowClear: true,
        },
        afterItem: () =>
          h(
            'span',
            { class: 'conf-switch' },
            t('flink.setting.flink.operateMessage.flinkNameTips'),
          ),
        rules: [
          { required: true, message: t('flink.setting.flink.operateMessage.flinkNameIsRequired') },
        ],
      },
      {
        field: 'flinkHome',
        label: t('flink.setting.flink.flinkHome'),
        component: 'Input',
        componentProps: {
          placeholder: t('flink.setting.flink.flinkHomePlaceholder'),
          allowClear: true,
        },
        afterItem: () =>
          h(
            'span',
            { class: 'conf-switch' },
            t('flink.setting.flink.operateMessage.flinkHomeTips'),
          ),
        rules: [
          { required: true, message: t('flink.setting.flink.operateMessage.flinkHomeIsRequired') },
        ],
      },
      {
        field: 'description',
        label: t('flink.setting.flink.description'),
        component: 'InputTextArea',
        componentProps: {
          placeholder: t('flink.setting.flink.descriptionPlaceholder'),
          allowClear: true,
        },
      },
    ],
  });
  const [registerModalInner, { changeOkLoading, closeModal }] = useModalInner(async (data) => {
    resetFields();
    if (data) {
      versionId.value = data.versionId;
      setFieldsValue(data);
    }
  });

  /* form submit */
  async function handleSubmit() {
    try {
      changeOkLoading(true);
      const formValue = await validate();
      // Detection environment
      const { data: resp } = await fetchExistsEnv({
        id: versionId.value,
        flinkName: formValue.flinkName,
        flinkHome: formValue.flinkHome,
      });
      // Environment detection is successful
      if (resp.data) {
        let message: string;
        let success = false;
        // create
        if (versionId.value == null) {
          const { data } = await fetchFlinkCreate(formValue);
          if (data.data) {
            success = true;
            message = formValue.flinkName.concat(
              t('flink.setting.flink.operateMessage.createFlinkHomeSuccessful'),
            );
          } else {
            message = data.message;
          }
        } else {
          // update
          const { data } = await fetchFlinkUpdate({
            id: versionId.value,
            ...formValue,
          });
          if (data.data) {
            message = formValue.flinkName.concat(
              t('flink.setting.flink.operateMessage.updateFlinkHomeSuccessful'),
            );
            success = true;
          } else {
            message = data.message;
          }
        }
        if (success) {
          Swal.fire({
            icon: 'success',
            title: message,
            showConfirmButton: false,
            timer: 2000,
          });
          closeModal();
          emit('reload');
        } else {
          Swal.fire('Failed', message.replaceAll(/\[StreamPark]/g, ''), 'error');
        }
      } else {
        if (resp.status === 'error') {
          Swal.fire(
            'Failed',
            'can no found flink-dist or found multiple flink-dist, FLINK_HOME error.',
            'error',
          );
        } else {
          Swal.fire('Failed', t('flink.setting.flink.operateMessage.flinkNameIsUnique'), 'error');
        }
      }
    } catch (error: any) {
      /* custom alert message */
      if (error?.response?.data?.message) {
        createConfirm({
          iconType: 'error',
          title: 'Operation Failed',
          content: h(
            'div',
            { class: 'whitespace-pre-wrap' },
            error?.response?.data?.message.replaceAll(/\[StreamPark]/g, ''),
          ),
        });
      } else {
        createMessage.error('error');
      }
    } finally {
      changeOkLoading(false);
    }
  }
</script>
<template>
  <BasicModal @register="registerModalInner" v-bind="$attrs" @ok="handleSubmit">
    <template #title>
      <SvgIcon name="flink" />
      {{ t('common.add') }}
    </template>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<style lang="less">
  .conf-switch {
    display: inline-block;
    margin-top: 10px;
    color: darkgrey;
  }
</style>
