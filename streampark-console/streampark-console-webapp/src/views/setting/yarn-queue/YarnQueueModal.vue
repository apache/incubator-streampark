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
  <BasicModal
    :okButtonProps="{ class: 'e2e-yarnqueue-submit-btn' }"
    :cancelButtonProps="{ class: 'e2e-yarnqueue-cancel-btn' }"
    :width="600"
    v-bind="$attrs"
    centered
    @register="registerModal"
    showFooter
    @ok="handleSubmit"
  >
    <template #title>
      <Icon icon="ant-design:menu-outlined" />
      {{ getTitle }}
    </template>
    <div class="mt-3">
      <BasicForm @register="registerForm" :schemas="getYarnQueueFormSchema" />
    </div>
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';

  import {
    fetchCheckYarnQueue,
    fetchYarnQueueCreate,
    fetchYarnQueueUpdate,
  } from '/@/api/setting/yarnQueue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { renderYarnQueueLabel } from './useYarnQueueRender';
  import Icon from '/@/components/Icon';

  export default defineComponent({
    name: 'YarnQueueDrawer',
    components: { Icon, BasicModal, BasicForm },
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
            componentProps: {
              placeholder: t('setting.yarnQueue.placeholder.description'),
              rows: 4,
            },
            rules: [{ max: 512, message: t('setting.yarnQueue.descriptionMessage') }],
          },
        ];
      });
      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        name: 'YarnQueueEditForm',
        colon: true,
        showActionButtonGroup: false,
        layout: 'vertical',
        baseColProps: { span: 22, offset: 1 },
      });

      const [registerModal, { setModalProps, closeModal }] = useModalInner(
        async (data: Recordable) => {
          resetFields();
          setModalProps({ confirmLoading: false });
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
          setModalProps({ confirmLoading: true });
          if (isUpdate.value) {
            await fetchYarnQueueUpdate(values);
          } else {
            delete values.id;
            await fetchYarnQueueCreate(values);
          }
          closeModal();
          emit('success', isUpdate.value);
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      return { registerModal, registerForm, getTitle, getYarnQueueFormSchema, handleSubmit };
    },
  });
</script>
