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
  <BasicDrawer
    :okText="t('common.submitText')"
    @register="registerDrawer"
    showFooter
    width="650"
    @ok="handleSubmit"
  >
    <template #title>
      <SvgIcon name="variable" />
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
  import { ref, h, computed, unref, reactive } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useFormValidate } from '/@/hooks/web/useFormValidate';
  import SvgIcon from '/@/components/Icon/src/SvgIcon.vue';
  import {
    fetchAddVariable,
    fetchCheckVariableCode,
    fetchUpdateVariable,
    fetchVariableInfo,
  } from '/@/api/resource/variable';
  import { VariableListRecord } from '/@/api/resource/variable/model/variableModel';
  import {ResultEnum} from "/@/enums/httpEnum";

  const emit = defineEmits(['success', 'register']);

  const { t } = useI18n();

  const isUpdate = ref(false);
  const { getItemProp, setValidateStatus, setHelp } = useFormValidate();
  const variableId = ref<Nullable<number>>(null);
  const variableInfo = reactive<Partial<VariableListRecord>>({});

  async function handleVariableCodeBlur(e) {
    const value = (e && e.target.value) || '';
    if (value.length) {
      setValidateStatus('validating');
      if (value.length < 3 || value.length > 50) {
        setValidateStatus('error');
        setHelp(t('flink.variable.form.len'));
        return Promise.reject();
      } else if (!new RegExp(/^([A-Za-z])+([A-Za-z0-9._-])+$/).test(value)) {
        setValidateStatus('error');
        setHelp(t('flink.variable.form.regExp'));
      } else {
        const { data } = await fetchCheckVariableCode({
          variableCode: value,
        });
        if (data.code === ResultEnum.ERROR) {
          setValidateStatus('error');
          setHelp(data.message);
          return Promise.reject();
        } else if (data.data) {
          setValidateStatus('success');
          setHelp('');
          return Promise.resolve();
        } else {
          setValidateStatus('error');
          setHelp(t('flink.variable.form.exists'));
          return Promise.reject();
        }
      }
    } else {
      setValidateStatus('error');
      setHelp(t('flink.variable.form.empty'));
      return Promise.reject();
    }
  }

  const getTeamFormSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'variableCode',
        label: t('flink.variable.table.variableCode'),
        component: 'Input',
        componentProps: { disabled: unref(isUpdate), onblur: handleVariableCodeBlur },
        itemProps: getItemProp.value,
        rules: unref(isUpdate) ? [] : [{ required: true }],
      },
      {
        field: 'variableValue',
        label: t('flink.variable.table.variableValue'),
        component: 'InputTextArea',
        componentProps: {
          rows: 4,
          placeholder: t('flink.variable.table.variableValuePlaceholder'),
        },
        rules: [{ required: true, message: t('flink.variable.table.variableValuePlaceholder') }],
      },
      {
        field: 'description',
        label: t('common.description'),
        component: 'InputTextArea',
        componentProps: { rows: 4 },
        rules: [{ max: 100, message: t('flink.variable.form.descriptionMessage') }],
      },
      {
        field: 'desensitization',
        label: t('flink.variable.form.desensitization'),
        component: 'Switch',
        componentProps: {
          checkedChildren: 'ON',
          unCheckedChildren: 'OFF',
        },
        defaultValue: false,
        afterItem: () =>
          h('span', { class: 'tip-info' }, t('flink.variable.form.desensitizationDesc')),
      },
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

  const [registerDrawer, { setDrawerProps, changeLoading, closeDrawer }] = useDrawerInner(
    async (data: Recordable) => {
      try {
        variableId.value = null;
        resetFields();
        setValidateStatus('');
        setHelp('');
        setDrawerProps({ confirmLoading: false });
        isUpdate.value = !!data?.isUpdate;
        if (unref(isUpdate)) {
          // is desensitization variable
          if (data.record.desensitization) {
            changeLoading(true);
            const res = await fetchVariableInfo({
              id: data.record.id,
            });
            Object.assign(variableInfo, res);
            changeLoading(false);
          } else {
            Object.assign(variableInfo, data.record);
          }
          variableId.value = data.record.id;
          setFieldsValue(variableInfo);
        }
      } catch (error) {
        changeLoading(false);
      }
    },
  );

  const getTitle = computed(() =>
    !unref(isUpdate) ? t('flink.variable.addVariable') : t('flink.variable.modifyVariable'),
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
