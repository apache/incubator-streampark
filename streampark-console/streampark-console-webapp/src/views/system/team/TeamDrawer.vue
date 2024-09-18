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
    :okButtonProps="{ class: 'e2e_team_pop_ok' }"
    :cancelButtonProps="{ class: 'e2e_team_pop_cancel' }"
    v-bind="$attrs"
    @register="registerDrawer"
    showFooter
    width="650"
    @ok="handleSubmit"
  >
    <template #title>
      <Icon icon="ant-design:team-outlined" />
      {{ getTitle }}
    </template>
    <BasicForm @register="registerForm" :schemas="getTeamFormSchema" />
  </BasicDrawer>
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';

  import { fetchTeamCreate, fetchTeamUpdate } from '/@/api/system/team';
  import { Icon } from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';

  export default defineComponent({
    name: 'TeamDrawer',
    components: { BasicDrawer, BasicForm, Icon },
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
        baseColProps: { span: 24 },
        labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
        wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
      });

      const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(
        async (data: Recordable) => {
          teamId.value = null;
          resetFields();
          setDrawerProps({ confirmLoading: false });
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
          setDrawerProps({ confirmLoading: true });
          await (isUpdate.value
            ? fetchTeamUpdate({ id: teamId.value, ...values })
            : fetchTeamCreate(values));
          closeDrawer();
          emit('success', isUpdate.value);
        } finally {
          setDrawerProps({ confirmLoading: false });
        }
      }

      return { registerDrawer, registerForm, getTitle, getTeamFormSchema, handleSubmit };
    },
  });
</script>
