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
<script setup lang="ts">
defineOptions({ name: 'LoginForm' })
import type { FormInst } from 'naive-ui'
import { useAuthStore } from '@/store'
import { fetchSignType } from '@/service'
import { PageEnum } from '@/enums/pageEnum'

type LoginType = 'PASSWORD' | 'LDAP'

const { t } = useI18n()
const authStore = useAuthStore()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const enableSSO = ref(false)
const enableLDAP = ref(false)
const loginType = ref<LoginType>('PASSWORD')
const formValue = ref({ account: 'admin', password: 'streampark' })

const ssoUrl = computed(() => {
    const base = import.meta.env.VITE_BASE_ADDRESS || window.location.origin
    return `${base}${PageEnum.SSO_LOGIN}`
})

const loginText = computed(() => {
    if (loginType.value === 'PASSWORD') {
        return {
            buttonText: t('sys.login.loginButton'),
            linkText: t('sys.login.ldapTip'),
        }
    }
    return {
        buttonText: t('sys.login.ldapTip'),
        linkText: t('sys.login.passwordTip'),
    }
})

const rules = computed(() => ({
    account: { required: true, message: t('sys.login.accountPlaceholder'), trigger: 'blur' },
    password: { required: true, message: t('sys.login.passwordPlaceholder'), trigger: 'blur' },
}))

function toggleLoginType() {
    loginType.value = loginType.value === 'PASSWORD' ? 'LDAP' : 'PASSWORD'
}

async function handleLogin() {
    await formRef.value?.validate(async (errors) => {
        if (errors) return
        loading.value = true
        try {
            const result = await authStore.login(
                formValue.value.account,
                formValue.value.password,
                loginType.value,
            )
            if (!result.needTeamSelect) window.$message?.success(t('sys.login.loginSuccessTitle'))
        } catch (e: any) {
            showCatchError(e, t('sys.api.apiRequestFailed'))
        } finally {
            loading.value = false
        }
    })
}

onMounted(async () => {
    const result = await fetchSignType()
    if (result.isSuccess && Array.isArray(result.data)) {
        enableSSO.value = result.data.includes('sso')
        enableLDAP.value = result.data.includes('ldap')
    }
})
</script>

<template>
    <div>
        <n-h2 depth="3" class="text-center">
            {{ t('sys.login.signInTitle') }}
        </n-h2>
        <n-form ref="formRef" :rules="rules" :model="formValue" :show-label="false" size="large">
            <n-form-item path="account">
                <n-input
                    v-model:value="formValue.account"
                    clearable
                    :placeholder="t('sys.login.userName')"
                    :input-props="{ autocomplete: 'username' }"
                />
            </n-form-item>
            <n-form-item path="password">
                <n-input
                    v-model:value="formValue.password"
                    type="password"
                    show-password-on="click"
                    clearable
                    :placeholder="t('sys.login.password')"
                    :input-props="{ autocomplete: 'current-password' }"
                    @keyup.enter="handleLogin"
                />
            </n-form-item>
            <n-button
                id="e2e-login-btn"
                block
                type="primary"
                size="large"
                :loading="loading"
                @click="handleLogin"
            >
                {{ loginText.buttonText }}
            </n-button>
            <div v-if="enableLDAP" class="mt-12px text-center">
                <n-button text @click="toggleLoginType">
                    {{ loginText.linkText }}
                </n-button>
            </div>
            <div v-if="enableSSO" class="mt-12px text-center">
                <n-button text tag="a" :href="ssoUrl" target="_self">
                    {{ t('sys.login.ssoSignIn') }}
                </n-button>
            </div>
        </n-form>
    </div>
</template>
