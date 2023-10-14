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
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" showFooter width="650" @ok="handleSubmit">
    <template #title>
      {{ getTitle }}
    </template>
    <BasicForm @register="registerForm" :schemas="getYarnQueueFormSchema" />
  </BasicDrawer>
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';

  import {
    fetchCheckYarnQueue,
    fetchYarnQueueCreate,
    fetchYarnQueueUpdate,
  } from '/@/api/setting/yarnQueue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { renderYarnQueueLabel } from './useYarnQueueRender';

  export default defineComponent({
    name: 'YarnQueueDrawer',
    components: { BasicDrawer, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const { t } = useI18n();

      const isUpdate = ref(false);
      const getYarnQueueFormSchema = computed((): FormSchema[] => {
        return [
          { field: 'id', label: 'id', component: 'Input', show: false },
          {
            field: 'queueLabel',
            label: t('setting.yarnQueue.yarnQueueLabelExpression'),
            component: 'Input',
            componentProps: {
              placeholder: t('setting.yarnQueue.placeholder.yarnQueueLabelExpression'),
            },
            required: true,
            dynamicRules: ({ model }) => {
              return [
                {
                  validator: async (_, value) => {
                    const params = {
                      queueLabel: value,
                    };
                    if (unref(isUpdate)) Object.assign(params, { id: model?.id });
                    const res = await fetchCheckYarnQueue(params);
                    if (res.status === 0) {
                      return Promise.resolve();
                    } else {
                      switch (res.status) {
                        case 1:
                          return Promise.reject(t('setting.yarnQueue.checkResult.existedHint'));
                        case 2:
                          return Promise.reject(
                            t('setting.yarnQueue.checkResult.invalidFormatHint'),
                          );
                        case 3:
                          return Promise.reject(t('setting.yarnQueue.checkResult.emptyHint'));
                      }
                    }
                  },
                  trigger: 'blur',
                },
              ];
            },
            render: (renderCallbackParams) => renderYarnQueueLabel(renderCallbackParams),
          },
          {
            field: 'description',
            label: t('common.description'),
            component: 'InputTextArea',
            componentProps: { rows: 4 },
            rules: [{ max: 512, message: t('setting.yarnQueue.descriptionMessage') }],
          },
        ];
      });
      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        name: 'YarnQueueEditForm',
        colon: true,
        showActionButtonGroup: false,
        baseColProps: { span: 24 },
        labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
        wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
      });

      const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(
        async (data: Recordable) => {
          resetFields();
          setDrawerProps({ confirmLoading: false });
          isUpdate.value = !!data?.isUpdate;
          // if (isUpdate.value) teamId.value = data.record.id;
          if (unref(isUpdate)) {
            setFieldsValue(data.record);
          }
        },
      );

      const getTitle = computed(() =>
        !unref(isUpdate)
          ? t('setting.yarnQueue.createQueue')
          : t('setting.yarnQueue.modifyYarnQueue'),
      );
      // form submit
      async function handleSubmit() {
        try {
          const values = await validate();
          setDrawerProps({ confirmLoading: true });
          if (isUpdate.value) {
            await fetchYarnQueueUpdate(values);
          } else {
            delete values.id;
            await fetchYarnQueueCreate(values);
          }
          closeDrawer();
          emit('success', isUpdate.value);
        } finally {
          setDrawerProps({ confirmLoading: false });
        }
      }

      return { registerDrawer, registerForm, getTitle, getYarnQueueFormSchema, handleSubmit };
    },
  });
</script>
