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
      <Icon icon="ant-design:user-add-outlined" />
      {{ getTitle }}
    </template>
    <BasicForm @register="registerForm" :schemas="getMemberFormSchema" />
  </BasicDrawer>
</template>

<script lang="ts">
  export default defineComponent({
    name: 'MemberDrawer',
  });
</script>

<script setup lang="ts" name="MemberDrawer">
  import { defineComponent, ref, computed, unref } from 'vue';
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';

  import { Icon } from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { RoleListItem } from '/@/api/demo/model/systemModel';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { RuleObject } from 'ant-design-vue/lib/form';
  import { StoreValue } from 'ant-design-vue/lib/form/interface';
  import { fetchAddMember, fetchCheckUserName, fetchUpdateMember } from '/@/api/sys/member';

  const { t } = useI18n();
  const userStore = useUserStoreWithOut();

  const emit = defineEmits(['success', 'register']);
  const props = defineProps({
    roleOptions: {
      type: Array as PropType<Array<Partial<RoleListItem>>>,
      default: () => [],
    },
  });

  const isUpdate = ref(false);
  const validateStatus = ref<'success' | 'warning' | 'error' | 'validating' | ''>('');
  const help = ref('');
  const editParams: { userId: Nullable<number>; id: Nullable<number>; teamId: Nullable<number> } = {
    userId: null,
    id: null,
    teamId: null,
  };

  async function checkUserName(_rule: RuleObject, value: StoreValue) {
    if (value) {
      if (value.length > 20) {
        validateStatus.value = 'error';
        help.value = 'User name should not be longer than 20 characters';
        return Promise.reject();
      } else if (value.length < 4) {
        validateStatus.value = 'error';
        help.value = 'User name should not be less than 4 characters';
        return Promise.reject();
      } else {
        validateStatus.value = 'validating';
        const res = await fetchCheckUserName({
          username: value,
        });
        if (res) {
          validateStatus.value = 'error';
          help.value = "Sorry, the user name doesn't exists";
          return Promise.reject();
        } else {
          validateStatus.value = 'success';
          help.value = '';
          return Promise.resolve();
        }
      }
    } else {
      validateStatus.value = 'error';
      help.value = 'User name cannot be empty';
      return Promise.reject();
    }
  }
  const getMemberFormSchema = computed((): FormSchema[] => {
    return [
      {
        field: 'userName',
        label: t('system.member.table.userName'),
        component: 'Input',
        componentProps: { disabled: unref(isUpdate) },
        itemProps: { validateStatus: unref(validateStatus), help: unref(help), hasFeedback: true },
        rules: unref(isUpdate)
          ? []
          : [{ required: true, validator: checkUserName, trigger: 'blur' }],
      },
      {
        field: 'roleId',
        label: t('system.member.table.roleName'),
        component: 'Select',
        componentProps: {
          options: props.roleOptions,
          fieldNames: { label: 'roleName', value: 'roleId' },
        },
        rules: [{ required: true, message: 'please select role' }],
      },
    ];
  });
  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    name: 'MemberForm',
    colon: true,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    labelCol: { lg: { span: 5, offset: 0 }, sm: { span: 7, offset: 0 } },
    wrapperCol: { lg: { span: 16, offset: 0 }, sm: { span: 17, offset: 0 } },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(
    async (data: Recordable) => {
      validateStatus.value = '';
      help.value = '';
      Object.assign(editParams, { userId: null, id: null });
      resetFields();
      setDrawerProps({ confirmLoading: false });
      isUpdate.value = !!data?.isUpdate;
      if (isUpdate.value) {
        Object.assign(editParams, {
          userId: data.record.userId,
          id: data.record.id,
          teamId: userStore.getTeamId,
        });
      }
      if (unref(isUpdate)) {
        setFieldsValue({
          userName: data.record.userName,
          roleId: data.record.roleId,
        });
      }
    },
  );

  const getTitle = computed(() =>
    !unref(isUpdate) ? t('system.member.addMember') : t('system.member.modifyMember'),
  );
  // form submit
  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });
      await (isUpdate.value
        ? fetchUpdateMember({ ...editParams, ...values })
        : fetchAddMember({ teamId: userStore.getTeamId, ...values }));
      closeDrawer();
      emit('success', isUpdate.value);
    } catch (e) {
      console.error(e);
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
