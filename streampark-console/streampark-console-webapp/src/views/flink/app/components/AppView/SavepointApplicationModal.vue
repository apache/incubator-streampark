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
    name: 'SavepointApplicationModal',
  });
</script>

<script setup lang="ts" name="SavepointApplicationModal">
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { fetchCheckSavepointPath } from "/@/api/flink/app/app";
  import { trigger } from '/@/api/flink/app/savepoint';
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
    name: 'savepointApplicationModal',
    labelWidth: 120,
    schemas: [
      {
        field: 'customSavepoint',
        label: 'Custom SavePoint',
        component: 'Input',
        componentProps: {
          placeholder: 'Optional: Entry the custom savepoint path',
          allowClear: true,
        },
        required: false,
        show: true
      }
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
      const { customSavepoint } = (await validate()) as Recordable;
      const savepointReq = {
        appId: app.id,
        savepointPath: customSavepoint,
      };
      if (customSavepoint) {
        const { data } = await fetchCheckSavepointPath({
          savePoint: customSavepoint,
        });
        if (data.data === false) {
          createErrorSwal('custom savePoint path is invalid, ' + data.message);
        } else {
          await handleSavepointAction(savepointReq);
          emit('updateOption', {
            type: 'savepointing',
            key: app.id,
            value: new Date().getTime(),
          });
        }
      } else {
        const { data } = await fetchCheckSavepointPath({
          id: app.id,
        });
        if (data.data) {
          await handleSavepointAction(savepointReq);

        } else {
          createErrorSwal(data.message);
        }
      }
    } catch (error) {
      console.error(error);
    }
  }

  async function handleSavepointAction(
    savepointTriggerReq: { appId: string | number,  savepointPath: string | null}) {
    await trigger(savepointTriggerReq);
    Swal.fire({
      icon: 'success',
      title: 'The current savepoint request is sent.',
      showConfirmButton: false,
      timer: 2000,
    });
    closeModal();
  }
</script>
<template>
  <BasicModal
    @register="registerModal"
    @ok="handleSubmit"
    :okText="t('common.apply')"
    :cancelText="t('common.cancelText')"
  >
    <template #title>
      {{ t('flink.app.view.savepoint') }}
    </template>
    <BasicForm @register="registerForm" class="!pt-20px" />
  </BasicModal>
</template>
