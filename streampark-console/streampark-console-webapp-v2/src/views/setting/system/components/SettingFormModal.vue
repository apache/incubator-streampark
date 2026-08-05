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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<script setup lang="ts">
import type { FormInst, FormRules } from 'naive-ui'
import {
    fetchDockerConfig,
    fetchDockerUpdate,
    fetchEmailConfig,
    fetchEmailUpdate,
    fetchVerifyDocker,
    fetchVerifyEmail,
} from '@/service'
import { SvgIcon } from '@/components/Icon'
import HelpInfo from '@/components/common/HelpInfo.vue'

export type SettingFormType = 'docker' | 'email'

const props = defineProps<{
    show: boolean
    type: SettingFormType | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    success: []
}>()

const { t } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInst | null>(null)

const dockerModel = ref({
    address: '',
    namespace: '',
    username: '',
    password: '',
})

const emailModel = ref({
    host: '',
    port: null as number | null,
    from: '',
    userName: '',
    password: '',
    ssl: false,
})

const title = computed(() => {
    if (props.type === 'docker') return t('setting.system.systemSettingItems.dockerSetting.name')
    if (props.type === 'email') return t('setting.system.systemSettingItems.emailSetting.name')
    return ''
})

const dockerRules: FormRules = {
    address: [
        { required: true, message: t('setting.system.docker.address.label'), trigger: 'blur' },
    ],
    namespace: [
        { required: true, message: t('setting.system.docker.namespace.label'), trigger: 'blur' },
    ],
    username: [
        { required: true, message: t('setting.system.docker.username.label'), trigger: 'blur' },
    ],
    password: [
        { required: true, message: t('setting.system.docker.password.label'), trigger: 'blur' },
    ],
}

const emailRules: FormRules = {
    host: [{ required: true, message: t('setting.system.email.host.label'), trigger: 'blur' }],
    port: [
        {
            required: true,
            type: 'number',
            message: t('setting.system.email.port.label'),
            trigger: 'blur',
        },
    ],
    from: [{ required: true, message: t('setting.system.email.from.label'), trigger: 'blur' }],
    userName: [
        { required: true, message: t('setting.system.email.userName.label'), trigger: 'blur' },
    ],
    password: [
        { required: true, message: t('setting.system.email.password.label'), trigger: 'blur' },
    ],
}

async function loadConfig(type: SettingFormType) {
    loading.value = true
    try {
        if (type === 'docker') {
            const result = await fetchDockerConfig()
            if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
            dockerModel.value = {
                address: result.data?.address ?? '',
                namespace: result.data?.namespace ?? '',
                username: result.data?.username ?? '',
                password: result.data?.password ?? '',
            }
        } else {
            const result = await fetchEmailConfig()
            if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
            emailModel.value = {
                host: result.data?.host ?? '',
                port: result.data?.port != null ? Number(result.data.port) : null,
                from: result.data?.from ?? '',
                userName: result.data?.userName ?? '',
                password: result.data?.password ?? '',
                ssl: Boolean(result.data?.ssl),
            }
        }
        nextTick(() => formRef.value?.restoreValidation())
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        loading.value = false
    }
}

watch(
    () => [props.show, props.type] as const,
    ([show, type]) => {
        if (show && type) loadConfig(type)
    },
)

function resetModels() {
    dockerModel.value = { address: '', namespace: '', username: '', password: '' }
    emailModel.value = { host: '', port: null, from: '', userName: '', password: '', ssl: false }
}

function closeModal() {
    emit('update:show', false)
}

watch(
    () => props.show,
    (show) => {
        if (!show) resetModels()
    },
)

async function verifyAndUpdateDocker() {
    const payload = { ...dockerModel.value }
    const verify = await fetchVerifyDocker(payload)
    if (!verify.isSuccess) throwApiFailure(verify, t('sys.api.apiRequestFailed'))

    const status = verify.data?.status ?? 0
    if (status === 400) {
        window.$dialog?.error({
            title: t('setting.system.update.dockerNotStart'),
            positiveText: t('common.okText'),
        })
        return false
    }
    if (status === 500) {
        window.$dialog?.error({
            title: verify.data?.msg || t('sys.api.apiRequestFailed'),
            positiveText: t('common.okText'),
        })
        return false
    }
    if (status !== 200) return false

    const update = await fetchDockerUpdate(payload)
    if (!update.isSuccess) throwApiFailure(update, t('sys.api.apiRequestFailed'))
    return true
}

async function verifyAndUpdateEmail() {
    const payload = { ...emailModel.value, port: emailModel.value.port ?? 0 }
    const verify = await fetchVerifyEmail(payload)
    if (!verify.isSuccess) throwApiFailure(verify, t('sys.api.apiRequestFailed'))

    if (verify.data?.status !== 200) {
        window.$dialog?.error({
            title: verify.data?.msg || t('sys.api.apiRequestFailed'),
            positiveText: t('common.okText'),
        })
        return false
    }

    const update = await fetchEmailUpdate(payload)
    if (!update.isSuccess) throwApiFailure(update, t('sys.api.apiRequestFailed'))
    return true
}

async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
        const ok =
            props.type === 'docker' ? await verifyAndUpdateDocker() : await verifyAndUpdateEmail()
        if (!ok) return
        window.$message?.success(t('setting.system.update.success'))
        closeModal()
        emit('success')
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        submitting.value = false
    }
}
</script>

<template>
    <n-modal
        :show="show"
        preset="card"
        :style="{ width: '650px' }"
        :mask-closable="false"
        @update:show="emit('update:show', $event)"
    >
        <template #header>
            <n-space align="center" :size="8">
                <SvgIcon v-if="type === 'docker'" name="docker" :size="20" />
                <SvgIcon v-if="type === 'email'" name="mail" :size="18" />
                <span>{{ title }}</span>
            </n-space>
        </template>

        <n-spin :show="loading">
            <n-form
                v-if="type === 'docker'"
                ref="formRef"
                :model="dockerModel"
                :rules="dockerRules"
                label-placement="left"
                label-width="140"
                class="mt-16px"
            >
                <n-form-item path="address">
                    <template #label>
                        <span class="inline-flex items-center gap-4px">
                            {{ t('setting.system.docker.address.label') }}
                            <HelpInfo :message="t('setting.system.docker.address.desc')" />
                        </span>
                    </template>
                    <n-input
                        v-model:value="dockerModel.address"
                        :placeholder="t('setting.system.docker.address.label')"
                    />
                </n-form-item>
                <n-form-item path="namespace">
                    <template #label>
                        <span class="inline-flex items-center gap-4px">
                            {{ t('setting.system.docker.namespace.label') }}
                            <HelpInfo :message="t('setting.system.docker.namespace.desc')" />
                        </span>
                    </template>
                    <n-input
                        v-model:value="dockerModel.namespace"
                        :placeholder="t('setting.system.docker.namespace.label')"
                    />
                </n-form-item>
                <n-form-item path="username">
                    <template #label>
                        <span class="inline-flex items-center gap-4px">
                            {{ t('setting.system.docker.username.label') }}
                            <HelpInfo :message="t('setting.system.docker.username.desc')" />
                        </span>
                    </template>
                    <n-input
                        v-model:value="dockerModel.username"
                        :placeholder="t('setting.system.docker.username.label')"
                    />
                </n-form-item>
                <n-form-item path="password">
                    <template #label>
                        <span class="inline-flex items-center gap-4px">
                            {{ t('setting.system.docker.password.label') }}
                            <HelpInfo :message="t('setting.system.docker.password.desc')" />
                        </span>
                    </template>
                    <n-input
                        v-model:value="dockerModel.password"
                        type="password"
                        show-password-on="click"
                        autocomplete="new-password"
                        :placeholder="t('setting.system.docker.password.label')"
                    />
                </n-form-item>
            </n-form>

            <n-form
                v-else-if="type === 'email'"
                ref="formRef"
                :model="emailModel"
                :rules="emailRules"
                label-placement="left"
                label-width="140"
                class="mt-16px"
            >
                <n-form-item path="host">
                    <template #label>
                        <span class="inline-flex items-center gap-4px">
                            {{ t('setting.system.email.host.label') }}
                            <HelpInfo :message="t('setting.system.email.host.desc')" />
                        </span>
                    </template>
                    <n-input
                        v-model:value="emailModel.host"
                        :placeholder="t('setting.system.email.host.label')"
                    />
                </n-form-item>
                <n-form-item path="port">
                    <template #label>
                        <span class="inline-flex items-center gap-4px">
                            {{ t('setting.system.email.port.label') }}
                            <HelpInfo :message="t('setting.system.email.port.desc')" />
                        </span>
                    </template>
                    <n-input-number
                        v-model:value="emailModel.port"
                        :style="{ width: '100%' }"
                        :min="0"
                        :max="65535"
                        :show-button="false"
                        :placeholder="t('setting.system.email.port.label')"
                    />
                </n-form-item>
                <n-form-item path="from">
                    <template #label>
                        <span class="inline-flex items-center gap-4px">
                            {{ t('setting.system.email.from.label') }}
                            <HelpInfo :message="t('setting.system.email.from.desc')" />
                        </span>
                    </template>
                    <n-input
                        v-model:value="emailModel.from"
                        :placeholder="t('setting.system.email.from.label')"
                    />
                </n-form-item>
                <n-form-item path="userName">
                    <template #label>
                        <span class="inline-flex items-center gap-4px">
                            {{ t('setting.system.email.userName.label') }}
                            <HelpInfo :message="t('setting.system.email.userName.desc')" />
                        </span>
                    </template>
                    <n-input
                        v-model:value="emailModel.userName"
                        :placeholder="t('setting.system.email.userName.label')"
                    />
                </n-form-item>
                <n-form-item path="password">
                    <template #label>
                        <span class="inline-flex items-center gap-4px">
                            {{ t('setting.system.email.password.label') }}
                            <HelpInfo :message="t('setting.system.email.password.desc')" />
                        </span>
                    </template>
                    <n-input
                        v-model:value="emailModel.password"
                        type="password"
                        show-password-on="click"
                        autocomplete="new-password"
                        :placeholder="t('setting.system.email.password.label')"
                    />
                </n-form-item>
                <n-form-item path="ssl">
                    <template #label>
                        <span class="inline-flex items-center gap-4px">
                            {{ t('setting.system.email.ssl.label') }}
                            <HelpInfo :message="t('setting.system.email.ssl.desc')" />
                        </span>
                    </template>
                    <n-switch v-model:value="emailModel.ssl">
                        <template #checked>ON</template>
                        <template #unchecked>OFF</template>
                    </n-switch>
                </n-form-item>
            </n-form>
        </n-spin>

        <template #footer>
            <n-space justify="end">
                <n-button @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button type="primary" :loading="submitting" @click="handleSubmit">
                    {{ t('common.submitText') }}
                </n-button>
            </n-space>
        </template>
    </n-modal>
</template>
