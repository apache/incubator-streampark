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
    :okButtonProps="{ class: 'e2e-team-submit-btn' }"
    :cancelButtonProps="{ class: 'e2e-team-cancel-btn' }"
    :width="600"
    v-bind="$attrs"
    @register="registerModal"
    showFooter
    @ok="handleSubmit"
    centered
  >
    <template #title>
      <Icon icon="ant-design:team-outlined" />
      {{ getTitle }}
    </template>
    <div class="mt-3">
      <BasicForm @register="registerForm" :schemas="getTeamFormSchema" />
    </div>
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';

  import { fetchTeamCreate, fetchTeamUpdate } from '/@/api/system/team';
  import { Icon } from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';

  export default defineComponent({
    name: 'TeamDrawer',
    components: { BasicModal, BasicForm, Icon },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const { t } = useI18n();

      const isUpdate = ref(false);
      const teamId = ref<Nullable<number>>(null);
      const getTeamFormSchema = computed((): FormSchema[] => {
        return [
          {
            field: 'teamName',
            label: t('system.team.table.teamName'),
            component: 'Input',
            componentProps: {
              disabled: isUpdate.value,
              placeholder: t('system.team.table.teamNamePlaceholder'),
            },
            required: !isUpdate.value,
            dynamicRules: () => {
              if (!isUpdate.value) {
                return [{ required: true, min: 4, message: t('system.team.table.teamMessage') }];
              }
              return [];
            },
          },
          {
            field: 'description',
            label: t('common.description'),
            component: 'InputTextArea',
            componentProps: { rows: 4 },
            rules: [{ max: 100, message: t('system.team.table.descriptionMessage') }],
          },
        ];
      });
      const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
        name: 'TeamEditForm',
        colon: true,
        showActionButtonGroup: false,
        layout: 'vertical',
        baseColProps: { span: 22, offset: 1 },
      });

      const [registerModal, { setModalProps, closeModal }] = useModalInner(
        async (data: Recordable) => {
          teamId.value = null;
          resetFields();
          setModalProps({ confirmLoading: false });
          isUpdate.value = !!data?.isUpdate;
          if (isUpdate.value) teamId.value = data.record.id;
          if (unref(isUpdate)) {
            setFieldsValue({
              ...data.record,
            });
          }
        },
      );

      const getTitle = computed(() =>
        !unref(isUpdate) ? t('system.team.addTeam') : t('system.team.modifyTeam'),
      );
      // form submit
      async function handleSubmit() {
        try {
          const values = await validate();
          setModalProps({ confirmLoading: true });
          await (isUpdate.value
            ? fetchTeamUpdate({ id: teamId.value, ...values })
            : fetchTeamCreate(values));
          closeModal();
          emit('success', isUpdate.value);
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      return { registerModal, registerForm, getTitle, getTeamFormSchema, handleSubmit };
    },
  });
</script>
