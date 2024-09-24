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
  import { fetchCancel, fetchCheckSavepointPath } from '/@/api/flink/app';
  import { CancelParam } from '/@/api/flink/app.type';
  import { h } from 'vue';
  const emit = defineEmits(['register', 'updateOption']);
  const app = reactive<Recordable>({});

  const { t } = useI18n();
  const { createErrorSwal, Swal } = useMessage();
  const [registerModal, { closeModal }] = useModalInner((data) => {
    if (data) {
      Object.assign(app, data.application);
      resetFields();
    }
  });
  const [registerForm, { resetFields, validate }] = useForm({
    name: 'stopApplicationModal',
    layout: 'vertical',
    baseColProps: { span: 22, offset: 1 },
    schemas: [
      {
        field: 'triggerSavepoint',
        label: t('flink.app.operation.triggerSavePoint'),
        component: 'Switch',
        componentProps: {
          checkedChildren: 'ON',
          unCheckedChildren: 'OFF',
        },
        defaultValue: false,
        afterItem: () => h('span', { class: 'pop-tip' }, t('flink.app.operation.enableSavePoint')),
      },
      {
        field: 'customSavepoint',
        label: 'Savepoint path',
        component: 'Input',
        componentProps: {
          placeholder: t('flink.app.operation.customSavepoint'),
          allowClear: true,
        },
        ifShow: ({ values }) => !!values.triggerSavepoint,
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
        ifShow: ({ values }) => !!values.triggerSavepoint,
        afterItem: () => h('span', { class: 'pop-tip' }, t('flink.app.operation.enableDrain')),
      },
    ],
    colon: true,
    showActionButtonGroup: false,
  });

  /* submit */
  async function handleSubmit() {
    try {
      const { triggerSavepoint, customSavepoint, drain } = (await validate()) as Recordable;
      const stopReq: CancelParam = {
        id: app.id,
        restoreOrTriggerSavepoint: triggerSavepoint,
        savepointPath: customSavepoint,
        drain: drain,
      };

      if (triggerSavepoint) {
        if (customSavepoint) {
          const { data } = await fetchCheckSavepointPath({
            savepointPath: customSavepoint,
          });
          if (data.data === false) {
            await createErrorSwal(t('flink.app.operation.invalidSavePoint') + data.message);
          } else {
            await handleStopAction(stopReq);
          }
        } else {
          const { data } = await fetchCheckSavepointPath({
            id: app.id,
          });
          if (data.data) {
            await handleStopAction(stopReq);
          } else {
            await createErrorSwal(data.message);
          }
        }
      } else {
        await handleStopAction(stopReq);
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
    Swal.fire({
      icon: 'success',
      title: t('flink.app.operation.canceling'),
      showConfirmButton: false,
      timer: 2000,
    });
    closeModal();
  }
</script>
<template>
  <BasicModal
    :okButtonProps="{ id: 'e2e-flinkapp-stop-submit' }"
    :cancelButtonProps="{ id: 'e2e-flinkapp-stop-cancel' }"
    @register="registerModal"
    @ok="handleSubmit"
    :okText="t('common.apply')"
    :cancelText="t('common.cancelText')"
  >
    <template #title>
      <SvgIcon name="shutdown" style="color: red" />
      {{ t('flink.app.view.stop') }}
    </template>
    <BasicForm @register="registerForm" class="!pt-20px" />
  </BasicModal>
</template>
