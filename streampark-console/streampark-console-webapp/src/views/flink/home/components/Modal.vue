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
  import { fetchCheckEnv, fetchFlinkCreate, fetchFlinkUpdate } from '/@/api/flink/flinkEnv';
  import { FlinkEnvCheckEnum } from '/@/enums/flinkEnum';
  const emit = defineEmits(['reload', 'register']);
  const versionId = ref<string | null>(null);
  const { t } = useI18n();
  const { Swal } = useMessage();
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
        label: t('setting.flinkHome.flinkName'),
        component: 'Input',
        componentProps: {
          placeholder: t('setting.flinkHome.flinkNamePlaceholder'),
          allowClear: true,
        },
        afterItem: () =>
          h('span', { class: 'tip-info' }, t('setting.flinkHome.operateMessage.flinkNameTips')),
        rules: [
          { required: true, message: t('setting.flinkHome.operateMessage.flinkNameIsRequired') },
        ],
      },
      {
        field: 'flinkHome',
        label: t('setting.flinkHome.flinkHome'),
        component: 'Input',
        componentProps: {
          placeholder: t('setting.flinkHome.flinkHomePlaceholder'),
          allowClear: true,
        },
        afterItem: () =>
          h('span', { class: 'tip-info' }, t('setting.flinkHome.operateMessage.flinkHomeTips')),
        rules: [
          { required: true, message: t('setting.flinkHome.operateMessage.flinkHomeIsRequired') },
        ],
      },
      {
        field: 'description',
        label: t('setting.flinkHome.description'),
        component: 'InputTextArea',
        componentProps: {
          placeholder: t('setting.flinkHome.descriptionPlaceholder'),
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
    changeOkLoading(true);
    let formValue;
    try {
      formValue = await validate();
    } catch (error) {
      console.warn('validate error:', error);
      return;
    } finally {
      changeOkLoading(false);
    }
    // Detection environment
    const resp = await fetchCheckEnv({
      id: versionId.value,
      flinkName: formValue.flinkName,
      flinkHome: formValue.flinkHome,
    });
    if (resp !== FlinkEnvCheckEnum.OK) {
      switch (resp) {
        case FlinkEnvCheckEnum.INVALID_PATH:
          Swal.fire(
            'Failed',
            t('setting.flinkHome.operateMessage.flinkHomePathIsInvalid'),
            'error',
          );
          break;
        case FlinkEnvCheckEnum.NAME_REPEATED:
          Swal.fire('Failed', t('setting.flinkHome.operateMessage.flinkNameIsRepeated'), 'error');
          break;
        case FlinkEnvCheckEnum.FLINK_DIST_NOT_FOUND:
          Swal.fire('Failed', t('setting.flinkHome.operateMessage.flinkDistNotFound'), 'error');
          break;
        case FlinkEnvCheckEnum.FLINK_DIST_REPEATED:
          Swal.fire('Failed', t('setting.flinkHome.operateMessage.flinkDistIsRepeated'), 'error');
          break;
      }
      changeOkLoading(false);
      return;
    }

    try {
      let message: string;
      let success = false;
      // create
      if (versionId.value == null) {
        const { data } = await fetchFlinkCreate(formValue);
        if (data.data) {
          success = true;
          message = formValue.flinkName.concat(
            t('setting.flinkHome.operateMessage.createFlinkHomeSuccessful'),
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
            t('setting.flinkHome.operateMessage.updateFlinkHomeSuccessful'),
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
