<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
-->
<script setup lang="ts">
import type { FormInst, FormRules } from 'naive-ui'
import { fetchUserPasswordUpdate } from '@/service'
import { useAuthStore } from '@/store'
import { useUserStoreWithOut } from '@/store/modules/user'

const show = defineModel<boolean>('show', { default: false })

const { t } = useI18n()
const authStore = useAuthStore()
const userStore = useUserStoreWithOut()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)

const formModel = ref({
  oldPassword: '',
  password: '',
  confirmPassword: '',
})

const rules: FormRules = {
  oldPassword: [
    { required: true, message: t('sys.login.oldPasswordPlaceholder'), trigger: 'blur' },
    { min: 8, message: t('system.user.form.passwordHelp'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('sys.login.newPasswordPlaceholder'), trigger: 'blur' },
    { min: 8, message: t('system.user.form.passwordHelp'), trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: t('sys.login.confirmPasswordPlaceholder'), trigger: 'blur' },
    {
      validator: (_rule, value) => {
        if (value !== formModel.value.password)
          return new Error(t('sys.login.diffPwd'))
        return true
      },
      trigger: 'blur',
    },
  ],
}

watch(show, (visible) => {
  if (!visible)
    return
  formModel.value = { oldPassword: '', password: '', confirmPassword: '' }
})

async function handleSubmit() {
  await formRef.value?.validate()
  const userId = userStore.getUserInfo?.userId
  if (!userId)
    return
  submitting.value = true
  try {
    const result = await fetchUserPasswordUpdate({
      userId,
      oldPassword: formModel.value.oldPassword,
      password: formModel.value.password,
    })
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    show.value = false
    window.$dialog?.success({
      title: t('sys.modifyPassword.title'),
      content: t('sys.modifyPassword.success'),
      positiveText: t('sys.modifyPassword.logout'),
      onPositiveClick: () => authStore.logout(),
    })
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    submitting.value = false
  }
}
</script>

<template>
  <n-modal
    v-model:show="show"
    preset="card"
    :title="t('sys.modifyPassword.title')"
    :style="{ width: '480px' }"
    :mask-closable="false"
  >
    <n-alert type="info" class="mb-16px" :show-icon="false">
      {{ userStore.getUserInfo?.username }}
    </n-alert>
    <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="120">
      <n-form-item :label="t('sys.login.oldPassword')" path="oldPassword">
        <n-input v-model:value="formModel.oldPassword" type="password" show-password-on="click" />
      </n-form-item>
      <n-form-item :label="t('sys.login.newPassword')" path="password">
        <n-input v-model:value="formModel.password" type="password" show-password-on="click" />
      </n-form-item>
      <n-form-item :label="t('sys.login.confirmPassword')" path="confirmPassword">
        <n-input v-model:value="formModel.confirmPassword" type="password" show-password-on="click" />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="show = false">
          {{ t('common.cancelText') }}
        </n-button>
        <n-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ t('common.okText') }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>
