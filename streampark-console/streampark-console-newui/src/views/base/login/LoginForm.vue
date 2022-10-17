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
  <LoginFormTitle v-show="getShow" class="enter-x mb-40px" />
  <Form
    class="p-4 enter-x"
    :model="formData"
    :rules="getFormRules"
    ref="formRef"
    v-show="getShow"
    @keypress.enter="handleLogin"
  >
    <FormItem name="account" class="enter-x">
      <Input
        v-model:value="formData.account"
        :placeholder="t('sys.login.userName')"
        class="fix-auto-fill"
      >
        <template #prefix>
          <user-outlined type="user" />
        </template>
      </Input>
    </FormItem>
    <FormItem name="password" class="enter-x">
      <InputPassword
        visibilityToggle
        v-model:value="formData.password"
        :placeholder="t('sys.login.password')"
      >
        <template #prefix>
          <lock-outlined type="user" />
        </template>
      </InputPassword>
    </FormItem>

    <ARow class="enter-x">
      <ACol :span="12">
        <FormItem>
          <!-- No logic, you need to deal with it yourself -->
          <Checkbox v-model:checked="rememberMe" size="small">
            {{ t('sys.login.rememberMe') }}
          </Checkbox>
        </FormItem>
      </ACol>
      <ACol :span="12">
        <FormItem :style="{ 'text-align': 'right' }">
          <!-- No logic, you need to deal with it yourself -->
          <Button type="link" size="small" @click="setLoginState(LoginStateEnum.RESET_PASSWORD)">
            {{ t('sys.login.forgetPassword') }}
          </Button>
        </FormItem>
      </ACol>
    </ARow>

    <FormItem class="enter-x">
      <Button type="primary" block @click="handleLogin" :loading="loading">
        {{ t('sys.login.loginButton') }}
      </Button>
      <!-- <Button size="large" class="mt-4 enter-x" block @click="handleRegister">
        {{ t('sys.login.registerButton') }}
      </Button> -->
    </FormItem>
    <!-- <ARow class="enter-x">
      <ACol :md="8" :xs="24">
        <Button block @click="setLoginState(LoginStateEnum.MOBILE)">
          {{ t('sys.login.mobileSignInFormTitle') }}
        </Button>
      </ACol>
      <ACol :md="8" :xs="24" class="!my-2 !md:my-0 xs:mx-0 md:mx-2">
        <Button block @click="setLoginState(LoginStateEnum.QR_CODE)">
          {{ t('sys.login.qrSignInFormTitle') }}
        </Button>
      </ACol>
      <ACol :md="6" :xs="24">
        <Button block @click="setLoginState(LoginStateEnum.REGISTER)">
          {{ t('sys.login.registerButton') }}
        </Button>
      </ACol>
    </ARow> -->
  </Form>
</template>
<script lang="ts" setup>
  import { reactive, ref, unref, computed } from 'vue';
  import { UserOutlined, LockOutlined } from '@ant-design/icons-vue';

  import { Checkbox, Form, Input, Row, Col, Button } from 'ant-design-vue';
  import LoginFormTitle from './LoginFormTitle.vue';

  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';

  import { useUserStore } from '/@/store/modules/user';
  import { LoginStateEnum, useLoginState, useFormRules, useFormValid } from './useLogin';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { loginApi } from '/@/api/system/user';
  //import { onKeyStroke } from '@vueuse/core';

  const ACol = Col;
  const ARow = Row;
  const FormItem = Form.Item;
  const InputPassword = Input.Password;
  const { t } = useI18n();
  const { notification, createErrorModal } = useMessage();
  const { prefixCls } = useDesign('login');
  const userStore = useUserStore();

  const { setLoginState, getLoginState } = useLoginState();
  const { getFormRules } = useFormRules();

  const formRef = ref();
  const loading = ref(false);
  const rememberMe = ref(false);

  const formData = reactive({
    account: 'admin',
    password: 'streampark',
  });

  const { validForm } = useFormValid(formRef);

  //onKeyStroke('Enter', handleLogin);

  const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN);
  async function createTeamModal(): Promise<void> {
    // let teamId = '';
    // const teamList: Array<Recordable> = await getTeamList({});
    // console.log('teamList', teamList);
    // const { createConfirm, createMessage } = useMessage();
    // createConfirm({
    //   iconType: 'warning',
    //   title: () => {
    //     return (
    //       <div>
    //         <Icon icon="ant-design:setting-outlined" />
    //         <span>Select Team</span>
    //       </div>
    //     );
    //   },
    //   content: () => {
    //     return (
    //       <div>
    //         <Icon icon="ant-design:setting-outlined" />
    //         <Select value={teamId} onChange={(value: string) => (teamId = value)}>
    //           {teamList.map((team: Recordable) => {
    //             return <Select.Option value={team.id}>{team.teamName}</Select.Option>;
    //           })}
    //         </Select>
    //       </div>
    //     );
    //   },
    //   onOk: () => {
    //     if (!teamId) {
    //       createMessage.warning('please select a team');
    //       return Promise.reject();
    //     }
    //     this.setTeamId(teamId);
    //     return Promise.resolve();
    //   },
    // });
  }
  async function handleLogin() {
    const loginFormValue = await validForm();
    if (!loginFormValue) return;
    try {
      loading.value = true;
      const { createMessage } = useMessage();
      // const { t } = useI18n();
      try {
        const { data } = await loginApi(
          {
            password: loginFormValue.password,
            username: loginFormValue.account,
          },
          'none',
        );

        const { code } = data;
        if (code != null && code != undefined) {
          if (code == 0 || code == 1) {
            const message =
              'SignIn failed,' +
              (code === 0 ? ' authentication error' : ' current User is locked.');
            createMessage.error(message);
            return;
          } else if (code == 403) {
            await createTeamModal();
          } else {
            console.log(data);
          }
        }
        userStore.setData(data.data);
        const userInfo = await userStore.afterLoginAction(true);
        userInfo &&
          notification.success({
            message: t('sys.login.loginSuccessTitle'),
            description: `${t('sys.login.loginSuccessDesc')}: ${userInfo.nickName}`,
            duration: 3,
          });
      } catch (error) {
        // createErrorModal({ title: t('sys.api.errorTip'), content: 'login failed' });
        return Promise.reject(error);
      }
      // const userInfo = await userStore.login({
      //   password: data.password,
      //   username: data.account,
      //   mode: 'none', //Do not default to the error message
      // });
    } catch (error) {
      createErrorModal({
        title: t('sys.api.errorTip'),
        content: (error as unknown as Error).message || t('sys.api.networkExceptionMsg'),
        getContainer: () => document.body.querySelector(`.${prefixCls}`) || document.body,
      });
    } finally {
      loading.value = false;
    }
  }
</script>
