<script setup lang="ts" name="PasswordModal">
  import { SettingOutlined } from '@ant-design/icons-vue';
  import { h } from 'vue';
  import { Alert } from 'ant-design-vue';
  import { useForm } from '/@/components/Form';
  import { useModalInner } from '/@/components/Modal';
  import { BasicModal } from '/@/components/Modal';
  import { BasicForm } from '/@/components/Form';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { fetchUserPasswordUpdate } from '/@/api/sys/user';
  import { useI18n } from 'vue-i18n';
  import { useMessage } from '/@/hooks/web/useMessage';

  const userStore = useUserStoreWithOut();
  const { t } = useI18n();
  const { createConfirm } = useMessage();
  const [registerModal, { changeOkLoading, closeModal }] = useModalInner();
  const [registerForm, { validate, resetFields }] = useForm({
    labelWidth: 140,
    colon: true,
    showActionButtonGroup: false,
    baseColProps: { span: 24 },
    schemas: [
      {
        field: 'username',
        label: 'UserName',
        component: 'Input',
        render: () => h(Alert, { type: 'info', message: userStore.getUserInfo?.username }),
      },
      {
        field: 'password',
        label: 'Password',
        component: 'InputPassword',
        itemProps: { hasFeedback: true },
        rules: [
          { required: true, message: 'Please input your password!', trigger: 'blur' },
          { min: 4, max: 16, message: 'The password contains 4 to 16 characters', trigger: 'blur' },
        ],
      },
      {
        field: 'confirmpassword',
        label: 'Confirm Password',
        component: 'InputPassword',
        itemProps: { hasFeedback: true },
        dynamicRules: ({ values }) => {
          return [
            {
              required: true,
              validator: (_, value) => {
                if (!value) {
                  return Promise.reject('Please confirm your password!');
                }
                if (value !== values.password) {
                  return Promise.reject('Two passwords that you enter is inconsistent!');
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
      console.log('formValue', formValue);
      await fetchUserPasswordUpdate({
        username: userStore.getUserInfo?.username,
        password: formValue.password,
      });
      resetFields();
      createConfirm({
        iconType: 'success',
        title: t('routes.demo.system.password'),
        content: 'The password has been changed successfully, and you are about to exit the system',
        okText: '立即退出',
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
      {{ t('routes.demo.system.password') }}
    </template>
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
