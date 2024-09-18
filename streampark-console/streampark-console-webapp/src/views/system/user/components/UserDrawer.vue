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
    :okButtonProps="{ class: 'e2e_user_pop_ok' }"
    :cancelButtonProps="{ class: 'e2e_user_pop_cancel' }"
    :okText="t('common.submitText')"
    @register="registerDrawer"
    showFooter
    width="40%"
    @ok="handleSubmit"
  >
    <template #title>
      <Icon icon="ant-design:user-add-outlined" />
      {{ getTitle }}
    </template>
    <BasicForm @register="registerForm" />
  </BasicDrawer>
  <Modal
    :visible="transferModalVisible"
    :confirm-loading="transferModalLoading"
    :ok-text="t('common.okText')"
    centered
    @ok="handleTransfer"
    @cancel="transferModalVisible = false"
  >
    <template #title>
      <Icon icon="ant-design:swap-outlined" />
      {{ t('system.user.form.notice') }}
    </template>
    <BasicForm @register="transferForm" class="!mt-30px !ml-36px" />
  </Modal>
</template>
<script lang="ts">
  import { computed, defineComponent, nextTick, ref, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { formSchema } from '../user.data';
  import { FormTypeEnum } from '/@/enums/formEnum';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { addUser, getUserList, transferUserResource, updateUser } from '/@/api/system/user';
  import Icon from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { Modal } from 'ant-design-vue';

  export default defineComponent({
    name: 'MenuDrawer',
    components: { Modal, BasicDrawer, Icon, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const { t } = useI18n();
      const userStore = useUserStoreWithOut();
      const formType = ref(FormTypeEnum.Edit);
      const userInfo = ref<Recordable>({});
      const transferModalVisible = ref(false);
      const transferModalLoading = ref(false);

      const [registerForm, { resetFields, setFieldsValue, updateSchema, validate, clearValidate }] =
        useForm({
          colon: true,
          labelWidth: 120,
          schemas: formSchema(unref(formType)),
          showActionButtonGroup: false,
          baseColProps: { lg: 22, md: 22 },
        });

      const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
        formType.value = data.formType;
        userInfo.value = data.record || {};
        resetFields();
        clearValidate();
        updateSchema(formSchema(unref(formType)));
        setDrawerProps({
          confirmLoading: false,
          showFooter: data.formType !== FormTypeEnum.View,
        });

        if (unref(formType) !== FormTypeEnum.Create) {
          const roleIds = data.record?.roleId ?? [];
          data.record.roleId = Array.isArray(roleIds) ? roleIds : roleIds.split(',');
          setFieldsValue(data.record);
        }
      });

      const [transferForm, { resetFields: resetTransferFields, validate: transferValidate }] =
        useForm({
          layout: 'vertical',
          showActionButtonGroup: false,
          baseColProps: { lg: 22, md: 22 },
          schemas: [
            {
              field: 'userId',
              label: t('system.user.form.transferResource'),
              component: 'ApiSelect',
              componentProps: {
                api: async () => {
                  let { records } = await getUserList({
                    page: 1,
                    pageSize: 999999,
                    teamId: userStore.getTeamId || '',
                  });
                  return records.filter((user) => user.userId !== userInfo.value.userId);
                },
                labelField: 'username',
                valueField: 'userId',
                showSearch: false,
                optionFilterGroup: 'username',
                placeholder: t('system.member.userNameRequire'),
              },
              rules: [
                {
                  required: true,
                  message: t('system.member.userNameRequire'),
                  trigger: 'blur',
                },
              ],
            },
          ],
        });

      const getTitle = computed(() => {
        return {
          [FormTypeEnum.Create]: t('system.user.form.create'),
          [FormTypeEnum.Edit]: t('system.user.form.edit'),
          [FormTypeEnum.View]: t('system.user.form.view'),
        }[unref(formType)];
      });

      async function handleSubmit() {
        try {
          const values = await validate();
          setDrawerProps({ confirmLoading: true });
          if (unref(formType) === FormTypeEnum.Edit) {
            const res: { needTransferResource: Boolean } = await updateUser(values);
            if (res?.needTransferResource) {
              transferModalVisible.value = true;
              nextTick(resetTransferFields);
              return;
            }
          } else {
            await addUser(values);
          }
          closeDrawer();
          emit('success');
        } finally {
          setDrawerProps({ confirmLoading: false });
        }
      }

      async function handleTransfer() {
        try {
          const values = await transferValidate();
          transferModalLoading.value = true;
          await transferUserResource({
            userId: userInfo.value.userId,
            targetUserId: values.userId,
          });
          emit('success');
          transferModalVisible.value = false;
        } catch (e) {
          console.error(e);
        } finally {
          transferModalLoading.value = false;
        }
      }

      return {
        t,
        registerDrawer,
        registerForm,
        transferForm,
        transferModalLoading,
        transferModalVisible,
        getTitle,
        handleSubmit,
        handleTransfer,
        closeDrawer,
      };
    },
  });
</script>
