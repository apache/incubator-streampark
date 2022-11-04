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
<template>
  <BasicDrawer okText="Submit" @register="registerDrawer" showFooter width="650" @ok="handleSubmit">
    <template #title>
      <Icon icon="ant-design:code-outlined" />
      {{ getTitle }}
    </template>
    <BasicForm @register="registerForm" :schemas="getTeamFormSchema" />
  </BasicDrawer>
</template>
<script lang="ts">
  export default {
    name: 'TeamDrawer',
  };
</script>

<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { Icon } from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useFormValidate } from '/@/hooks/web/useFormValidate';
  import {
    fetchAddVariable,
    fetchCheckVariableCode,
    fetchUpdateVariable,
  } from '/@/api/system/variable';
  import { h } from 'vue';

  const emit = defineEmits(['success', 'register']);

  const { t } = useI18n();

  const isUpdate = ref(false);
  const { getItemProp, setValidateStatus, setHelp } = useFormValidate();
  const variableId = ref<Nullable<number>>(null);

  async function handleVariableCodeBlur(e) {
    const value = (e && e.target.value) || '';
    if (value.length) {
      setValidateStatus('validating');
      if (value.length < 3 || value.length > 50) {
        setValidateStatus('error');
        setHelp(
          'Sorry, variable code length should be no less than 3 and no more than 50 characters.',
        );
        return Promise.reject();
      } else if (!new RegExp(/^([A-Za-z])+([A-Za-z0-9._-])+$/).test(value)) {
        setValidateStatus('error');
        setHelp(
          'Sorry, variable code can only contain letters, numbers, middle bars, bottom bars and dots, and the beginning can only be letters, For example, kafka_cluster.brokers-520',
        );
      } else {
        const { data } = await fetchCheckVariableCode({
          variableCode: value,
        });
        if (data.status !== 'success') {
          setValidateStatus('error');
          setHelp(data.message);
          return Promise.reject();
        } else if (data.data) {
          setValidateStatus('success');
          setHelp('');
          return Promise.resolve();
        } else {
          setValidateStatus('error');
          setHelp('Sorry, the Variable Code already exists');
          return Promise.reject();
        }
      }
    } else {
      setValidateStatus('error');
      setHelp('Variable Code cannot be empty');
      return Promise.reject();
    }
  }

  const getTeamFormSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'variableCode',
        label: t('system.variable.table.variableCode'),
        component: 'Input',
        componentProps: { disabled: unref(isUpdate), onblur: handleVariableCodeBlur },
        itemProps: getItemProp.value,
        rules: unref(isUpdate) ? [] : [{ required: true }],
      },
      {
        field: 'variableValue',
        label: t('system.variable.table.variableValue'),
        component: 'InputTextArea',
        componentProps: { rows: 4, placeholder: 'Please enter Variable Value' },
        rules: [{ required: true, message: 'please enter Variable Value' }],
      },
      {
        field: 'description',
        label: t('system.variable.table.description'),
        component: 'InputTextArea',
        componentProps: { rows: 4 },
        rules: [{ max: 100, message: t('system.variable.form.descriptionMessage') }],
      },
      {
        field: 'desensitization',
        label: 'Desensitization',
        component: 'Switch',
        componentProps: {
          checkedChildren: 'ON',
          unCheckedChildren: 'OFF',
        },
        defaultValue: false,
        afterItem: h('span', { class: 'conf-switch' }, 'Whether desensitization is required, e.g: desensitization of sensitive data such as passwords, if enable variable value will be displayed as ********'),
      }
    ];
  });

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    name: 'VariableForm',
    colon: true,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(
    async (data: Recordable) => {
      variableId.value = null;
      resetFields();
      setValidateStatus('');
      setHelp('');
      setDrawerProps({ confirmLoading: false });
      isUpdate.value = !!data?.isUpdate;
      if (isUpdate.value) variableId.value = data.record.id;
      if (unref(isUpdate)) {
        setFieldsValue({
          ...data.record,
        });
      }
    },
  );

  const getTitle = computed(() =>
    !unref(isUpdate) ? t('system.variable.addVariable') : t('system.variable.modifyVariable'),
  );
  // form submit
  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });
      await (isUpdate.value
        ? fetchUpdateVariable({ id: variableId.value, ...values })
        : fetchAddVariable(values));
      closeDrawer();
      emit('success', isUpdate.value);
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less">
  .conf-switch {
    display: inline-block;
    margin-top: 10px;
    color: darkgrey;
  }
</style>
