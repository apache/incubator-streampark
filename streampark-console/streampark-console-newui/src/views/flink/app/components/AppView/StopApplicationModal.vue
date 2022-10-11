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
  import { reactive, defineComponent } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  export default defineComponent({
    name: 'StopApplicationModal',
  });
</script>

<script setup lang="ts" name="StopApplicationModal">
  import { BasicForm, useForm } from '/@/components/Form';
  import { SvgIcon } from '/@/components/Icon';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { fetchCancel, fetchCheckSavepointPath, fetchVerifySchema } from '/@/api/flink/app/app';
  import { CancelParam } from '/@/api/flink/app/app.type';

  const emit = defineEmits(['register', 'updateOption']);
  const app = reactive<Recordable>({});

  const { t } = useI18n();
  const { createMessage, createErrorModal } = useMessage();
  const [registerModal, { closeModal }] = useModalInner((data) => {
    if (data) {
      Object.assign(app, data.application);
      resetFields();
    }
  });
  const [registerForm, { resetFields, validate }] = useForm({
    name: 'stopApplicationModal',
    labelWidth: 120,
    schemas: [
      {
        field: 'stopSavePointed',
        label: 'Savepoint',
        component: 'Switch',
        componentProps: {
          checkedChildren: 'ON',
          unCheckedChildren: 'OFF',
        },
        defaultValue: true,
        helpMessage: 'trigger savePoint before taking cancel',
      },
      {
        field: 'customSavepoint',
        label: 'Custom SavePoint',
        component: 'Input',
        componentProps: {
          placeholder: 'Entry the custom savepoint path',
          allowClear: true,
        },
        helpMessage: 'restore the application from savepoint or latest checkpoint',
        ifShow: ({ values }) => !!values.stopSavePointed,
      },
      {
        field: 'drain',
        label: 'Drain',
        component: 'Switch',
        componentProps: {
          checkedChildren: 'ON',
          unCheckedChildren: 'OFF',
        },
        defaultValue: false,
        helpMessage: 'Send max watermark before stopped',
      },
    ],
    colon: true,
    showActionButtonGroup: false,
    labelCol: { lg: { span: 7, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 4, offset: 0 } },
    baseColProps: { span: 24 },
  });

  /* submit */
  async function handleSubmit() {
    try {
      const { stopSavePointed, drain, customSavepoint } = (await validate()) as Recordable;
      const stopReq = {
        id: app.id,
        savePointed: stopSavePointed,
        drain: drain,
        savePoint: customSavepoint,
      };

      if (stopSavePointed) {
        if (customSavepoint != null) {
          const { data } = await fetchVerifySchema({
            path: customSavepoint,
          });
          if (data.data === false) {
            createErrorModal({
              title: t('common.failed'),
              content: 'custom savePoint path is invalid, ' + data.message,
            });
          } else {
            handleStopAction(stopReq);
          }
        } else {
          const { data } = await fetchCheckSavepointPath({
            id: app.id,
          });
          if (data.data) {
            handleStopAction(stopReq);
          } else {
            createErrorModal({
              title: t('common.failed'),
              content: data.message,
            });
          }
        }
      } else {
        handleStopAction(stopReq);
      }
      emit('updateOption', {
        type: 'stopping',
        key: app.id,
        value: new Date().getTime(),
      });
    } catch (error) {
      console.error(error);
    }
  }

  async function handleStopAction(stopReq: CancelParam) {
    await fetchCancel(stopReq);
    createMessage.success('The current job is canceling');
    closeModal();
  }
</script>
<template>
  <BasicModal @register="registerModal" @ok="handleSubmit" okText="Apply" cancelText="Cancel">
    <template #title>
      <SvgIcon name="shutdown" style="color: red" />

      {{ t('flink.app.view.stop') }}
    </template>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
