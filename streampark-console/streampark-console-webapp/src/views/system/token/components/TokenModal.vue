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
    :okButtonProps="{ class: 'e2e-token-submit-btn' }"
    :cancelButtonProps="{ class: 'e2e-token-cancel-btn' }"
    :width="600"
    @register="registerDrawer"
    showFooter
    centered
    :minHeight="140"
    @ok="handleSubmit"
  >
    <template #title>
      <Icon icon="ant-design:key-outlined" />
      {{ getTitle }}
    </template>
    <div class="mt-18px">
      <BasicForm @register="registerForm" />
    </div>
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { formSchema } from '../token.data';
  import { BasicModal, useModalInner } from '/@/components/Modal';

  import { fetchTokenCreate } from '/@/api/system/token';
  import { useI18n } from '/@/hooks/web/useI18n';
  import Icon from '/@/components/Icon';

  export default defineComponent({
    name: 'TokenModal',
    components: { Icon, BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isUpdate = ref(true);
      const { t } = useI18n();

      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        colon: true,
        schemas: formSchema,
        showActionButtonGroup: false,
        layout: 'vertical',
        baseColProps: { span: 22, offset: 1 },
      });

      const [registerDrawer, { setModalProps, closeModal }] = useModalInner(async (data) => {
        resetFields();
        setModalProps({ confirmLoading: false });
        isUpdate.value = !!data?.isUpdate;

        if (unref(isUpdate)) {
          setFieldsValue({
            ...data.record,
          });
        }
      });

      const getTitle = computed(() =>
        !unref(isUpdate) ? t('system.token.addToken') : t('system.token.modifyToken'),
      );

      async function handleSubmit() {
        try {
          const values = await validate();
          setModalProps({ confirmLoading: true });
          const res = await fetchTokenCreate(values);
          closeModal();
          emit('success', { isUpdate: unref(isUpdate), values: res });
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      return { t, registerDrawer, registerForm, getTitle, handleSubmit };
    },
  });
</script>
