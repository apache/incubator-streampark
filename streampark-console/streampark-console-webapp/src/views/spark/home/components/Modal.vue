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
<script lang="ts" setup>
  import { useI18n } from '/@/hooks/web/useI18n';
  import { h, ref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { SvgIcon } from '/@/components/Icon';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { SparkEnvCheckEnum } from '/@/enums/sparkEnum';
  import { fetchSparkEnvCheck, fetchSparkEnvUpdate } from '/@/api/spark/home';
  import { fetchSparkEnvCreate } from '/@/api/spark/home';

  defineOptions({
    name: 'SparkModal',
  });
  const emit = defineEmits(['reload', 'register']);
  const versionId = ref<string | null>(null);
  const { t } = useI18n();
  const { Swal } = useMessage();
  const [registerForm, { setFieldsValue, validate, resetFields }] = useForm({
    colon: true,
    showActionButtonGroup: false,
    layout: 'vertical',
    baseColProps: { span: 22, offset: 1 },
    schemas: [
      {
        field: 'sparkName',
        label: t('spark.home.form.sparkName'),
        component: 'Input',
        componentProps: {
          placeholder: t('spark.home.placeholder.sparkName'),
          allowClear: true,
        },
        afterItem: () => h('span', { class: 'tip-info' }, t('spark.home.tips.sparkName')),
        rules: [{ required: true, message: t('spark.home.tips.sparkNameIsRequired') }],
      },
      {
        field: 'sparkHome',
        label: t('spark.home.form.sparkHome'),
        component: 'Input',
        componentProps: {
          placeholder: t('spark.home.placeholder.sparkHome'),
          allowClear: true,
        },
        afterItem: () => h('span', { class: 'tip-info' }, t('spark.home.tips.sparkHome')),
        rules: [{ required: true, message: t('spark.home.tips.sparkHomeIsRequired') }],
      },
      {
        field: 'description',
        label: t('spark.home.form.description'),
        component: 'InputTextArea',
        componentProps: {
          placeholder: t('spark.home.placeholder.description'),
          allowClear: true,
          rows: 3,
        },
      },
    ],
  });
  const [registerModalInner, { changeOkLoading, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    if (data) {
      versionId.value = data.versionId;
      await setFieldsValue(data);
    }
  });

  /* form submit */
  async function handleSubmit() {
    try {
      const formValue = await validate();
      changeOkLoading(true);
      // Detection environment
      const resp = await fetchSparkEnvCheck({
        id: versionId.value,
        sparkName: formValue.sparkName,
        sparkHome: formValue.sparkHome,
      });
      const checkResp = parseInt(resp);
      if (checkResp !== SparkEnvCheckEnum.OK) {
        switch (checkResp) {
          case SparkEnvCheckEnum.INVALID_PATH:
            Swal.fire('Failed', t('spark.home.tips.sparkHomePathIsInvalid'), 'error');
            break;
          case SparkEnvCheckEnum.NAME_REPEATED:
            Swal.fire('Failed', t('spark.home.tips.sparkNameIsRepeated'), 'error');
            break;
          case SparkEnvCheckEnum.SPARK_DIST_NOT_FOUND:
            Swal.fire('Failed', t('spark.home.tips.sparkDistNotFound'), 'error');
            break;
          case SparkEnvCheckEnum.SPARK_DIST_REPEATED:
            Swal.fire('Failed', t('spark.home.tips.sparkDistIsRepeated'), 'error');
            break;
        }
        changeOkLoading(false);
        return;
      }

      let message: string;
      let success = false;
      // create
      if (versionId.value == null) {
        const res = await fetchSparkEnvCreate(formValue);
        if (res.data) {
          success = true;
          message = formValue.sparkName.concat(t('spark.home.tips.createSparkHomeSuccessful'));
        } else {
          message = res.message;
        }
      } else {
        // update
        const res = await fetchSparkEnvUpdate({
          id: versionId.value,
          ...formValue,
        });
        if (res.data) {
          message = formValue.sparkName.concat(t('spark.home.tips.updateSparkHomeSuccessful'));
          success = true;
        } else {
          message = res.message;
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
    } catch (error) {
      console.warn('validate error:', error);
      return;
    } finally {
      changeOkLoading(false);
    }
  }
</script>
<template>
  <BasicModal
    @register="registerModalInner"
    :width="600"
    centered
    v-bind="$attrs"
    @ok="handleSubmit"
  >
    <template #title>
      <SvgIcon name="spark" />
      {{ versionId ? t('common.edit') : t('common.add') }}
    </template>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
