<script setup lang="ts">
  import { SettingOutlined } from '@ant-design/icons-vue';
  import { h } from 'vue';
  import { Alert } from 'ant-design-vue';
  import { useForm } from '/@/components/Form';
  import { useModalInner } from '/@/components/Modal';
  import { BasicModal } from '/@/components/Modal';
  import { BasicForm } from '/@/components/Form';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { fetchUserPasswordUpdate } from '/@/api/system/user';
  import { useI18n } from 'vue-i18n';
  import { useMessage } from '/@/hooks/web/useMessage';

  const userStore = useUserStoreWithOut();
  const { t } = useI18n();
  const { createConfirm } = useMessage();
  const [registerModal, { changeOkLoading, closeModal }] = useModalInner(() => {
    resetFields();
  });
  const [registerForm, { validate, resetFields }] = useForm({
    labelWidth: 140,
    colon: true,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    schemas: [
      {
        field: 'username',
        label: t('sys.login.userName'),
        component: 'Input',
        render: () => h(Alert, { type: 'info', message: userStore.getUserInfo?.username }),
      },
      {
        field: 'oldPassword',
        label: t('sys.login.oldPassword'),
        component: 'InputPassword',
        itemProps: { hasFeedback: true },
        rules: [
          { required: true, message: t('sys.login.oldPasswordPlaceholder'), trigger: 'blur' },
          { min: 8, message: t('system.user.form.passwordHelp'), trigger: 'blur' },
        ],
      },
      {
        field: 'password',
        label: t('sys.login.newPassword'),
        component: 'InputPassword',
        itemProps: { hasFeedback: true },
        rules: [
          { required: true, message: t('sys.login.newPasswordPlaceholder'), trigger: 'blur' },
          { min: 8, message: t('system.user.form.passwordHelp'), trigger: 'blur' },
        ],
      },
      {
        field: 'confirmpassword',
        label: t('sys.login.confirmPassword'),
        component: 'InputPassword',
        itemProps: { hasFeedback: true },
        dynamicRules: ({ values }) => {
          return [
            {
              required: true,
              validator: (_, value) => {
                if (!value) {
                  return Promise.reject(t('sys.login.confirmPasswordPlaceholder'));
                }
                if (value !== values.password) {
                  return Promise.reject(t('sys.login.diffPwd'));
                }
                return Promise.resolve();
              },
            },
          ];
        },
      },
    ],
  });
  async function handleChangePassword() {
    try {
      changeOkLoading(true);
      const formValue = await validate();
      await fetchUserPasswordUpdate({
        userId: userStore.getUserInfo?.userId,
        oldPassword: formValue.oldPassword,
        password: formValue.password,
      });

      createConfirm({
        iconType: 'success',
        title: t('sys.modifyPassword.title'),
        content: t('sys.modifyPassword.success'),
        okText: t('sys.modifyPassword.logout'),
        okType: 'danger',
        onOk: () => {
          userStore.logout(true);
        },
      });
      closeModal();
    } catch (error) {
      console.error(error);
    } finally {
      changeOkLoading(false);
    }
  }
</script>
<template>
  <BasicModal v-bind="$attrs" @register="registerModal" @ok="handleChangePassword">
    <template #title>
      <SettingOutlined style="color: green" />
      {{ t('sys.modifyPassword.title') }}
    </template>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
