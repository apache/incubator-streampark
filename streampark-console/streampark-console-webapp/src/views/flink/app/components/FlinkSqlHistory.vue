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
    name: 'FlinkSqlHistory',
  });
</script>
<script setup lang="ts" name="FlinkSqlHistory">
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useI18n } from '/@/hooks/web/useI18n';
  const { t } = useI18n();
  const [registerModalInner] = useModalInner();
  const [registerForm] = useForm({
    labelWidth: 120,
    colon: true,
    schemas: [
      {
        field: 'compare_sql',
        label: 'Version',
        component: 'Select',
        componentProps: {
          placeholder: 'Please select the sql version to compare',
          mode: 'multiple',
          maxTagCount: 2,
        },
      },
    ],
    baseColProps: { span: 24 },
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
    showActionButtonGroup: false,
  });
</script>
<template>
  <BasicModal @register="registerModalInner">
    <template #title>
      <Icon icon="ant-design:swap-outlined" style="color: #4a9ff5" />
      {{ t('flink.app.flinkSql.compareFlinkSQL') }}
    </template>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
<style scoped></style>
