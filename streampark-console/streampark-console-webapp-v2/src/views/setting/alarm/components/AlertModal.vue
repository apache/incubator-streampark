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
import type { FormInst, FormRules, SelectOption } from 'naive-ui'
import type { AlertCreate } from '@/types/api/setting/types/alert.type'
import { fetchAlertAdd, fetchAlertUpdate, fetchExistsAlert } from '@/service'
import { SvgIcon } from '@/components/Icon'
import { useUserStore } from '@/store/modules/user'
import { alertTypes } from './constants'

export interface AlertFormRecord {
    id?: string
    alertName?: string
    alertType?: string[]
    alertEmail?: string
    alertDingURL?: string
    dingtalkToken?: string
    dingtalkSecretToken?: string
    alertDingUser?: string
    dingtalkIsAtAll?: boolean
    dingtalkSecretEnable?: boolean
    weToken?: string
    alertSms?: string
    alertSmsTemplate?: string
    larkToken?: string
    larkIsAtAll?: boolean
    larkSecretEnable?: boolean
    larkSecretToken?: string
}

const props = defineProps<{
    show: boolean
    record?: AlertFormRecord | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    success: []
}>()

const { t } = useI18n()
const userStore = useUserStore()

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const alertId = ref<string | null>(null)

const defaultModel = (): AlertFormRecord => ({
    alertName: '',
    alertType: [],
    alertEmail: '',
    alertDingURL: '',
    dingtalkToken: '',
    dingtalkSecretToken: '',
    alertDingUser: '',
    dingtalkIsAtAll: false,
    dingtalkSecretEnable: false,
    weToken: '',
    alertSms: '',
    alertSmsTemplate: '',
    larkToken: '',
    larkIsAtAll: false,
    larkSecretEnable: false,
    larkSecretToken: '',
})

const formModel = ref<AlertFormRecord>(defaultModel())

const typeMap = computed(() => alertTypes(t))

const alertTypeOptions = computed(() =>
    Object.entries(typeMap.value).map(([value, item]) => ({
        value,
        label: item.name,
        disabled: item.disabled,
        icon: item.icon,
    })),
)

const isEdit = computed(() => Boolean(alertId.value))

function hasType(type: string) {
    return (formModel.value.alertType ?? []).includes(type)
}

function renderAlertTypeLabel(option: SelectOption) {
    const icon = (option as SelectOption & { icon?: string }).icon
    return h('span', { class: 'inline-flex items-center gap-6px' }, [
        h(SvgIcon, { name: icon ?? 'mail', size: 16 }),
        h('span', option.label as string),
    ])
}

const emailPattern =
    /^([a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)*\.[a-zA-Z0-9]{2,6})(,[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)*\.[a-zA-Z0-9]{2,6})*?$/

const dingTalkUrlPattern =
    /^((https?):\/\/)?([^!@#$%^&*?.\s-]([^!@#$%^&*?.\s]{0,63}[^!@#$%^&*?.\s])?\.)+[a-z]{2,6}\/?/

const rules = computed<FormRules>(() => {
    const base: FormRules = {
        alertName: [
            {
                required: true,
                trigger: 'blur',
                asyncValidator: async (_rule, value: string) => {
                    if (!value)
                        throw new Error(
                            t('setting.alarm.alertNameErrorMessage.alertNameIsRequired'),
                        )
                    if (!alertId.value) {
                        const result = await fetchExistsAlert({ alertName: value })
                        if (!result.isSuccess)
                            throwApiFailure(
                                result,
                                t('setting.alarm.alertNameErrorMessage.alertConfigFailed'),
                            )
                        if (result.data)
                            throw new Error(
                                t('setting.alarm.alertNameErrorMessage.alertNameAlreadyExists'),
                            )
                    }
                },
            },
        ],
        alertType: [
            {
                required: true,
                type: 'array',
                min: 1,
                message: t('setting.alarm.faultAlertTypeIsRequired'),
                trigger: 'change',
            },
        ],
    }

    if (hasType('1')) {
        base.alertEmail = [
            {
                required: true,
                message: t('setting.alarm.alertEmailAddressIsRequired'),
                trigger: 'blur',
            },
            {
                pattern: emailPattern,
                message: t('setting.alarm.alertEmailFormatIsInvalid'),
                trigger: 'blur',
            },
        ]
    }

    if (hasType('2')) {
        base.alertDingURL = [
            {
                pattern: dingTalkUrlPattern,
                message: t('setting.alarm.dingTalkUrlFormatIsInvalid'),
                trigger: 'blur',
            },
        ]
        base.dingtalkToken = [
            { required: true, message: 'Access token is required', trigger: 'blur' },
        ]
        if (formModel.value.dingtalkSecretEnable) {
            base.dingtalkSecretToken = [
                {
                    required: true,
                    message: t('setting.alarm.dingTalkSecretTokenIsRequired'),
                    trigger: 'blur',
                },
            ]
        }
    }

    if (hasType('4')) {
        base.weToken = [
            { required: true, message: t('setting.alarm.weChattokenIsRequired'), trigger: 'blur' },
        ]
    }

    if (hasType('8')) {
        base.alertSms = [
            { required: true, message: t('setting.alarm.mobileNumberIsRequired'), trigger: 'blur' },
        ]
        base.alertSmsTemplate = [
            { required: true, message: t('setting.alarm.smsTemplateIsRequired'), trigger: 'blur' },
        ]
    }

    if (hasType('16')) {
        base.larkToken = [{ required: true, message: 'Lark token is required', trigger: 'blur' }]
        if (formModel.value.larkSecretEnable) {
            base.larkSecretToken = [
                {
                    required: true,
                    message: t('setting.alarm.larkSecretTokenIsRequired'),
                    trigger: 'blur',
                },
            ]
        }
    }

    return base
})

watch(
    () => [props.show, props.record] as const,
    ([show, record]) => {
        if (!show) return
        alertId.value = record?.id ?? null
        formModel.value = {
            ...defaultModel(),
            ...record,
            alertType: record?.alertType ? [...record.alertType] : [],
        }
        nextTick(() => formRef.value?.restoreValidation())
    },
    { immediate: true },
)

watch(
    () => props.show,
    (show) => {
        if (!show) {
            alertId.value = null
            formModel.value = defaultModel()
        }
    },
)

function closeModal() {
    emit('update:show', false)
}

function stripStreamParkMessage(message?: string) {
    return (message ?? '').replaceAll(/\[StreamPark]/g, '')
}

function buildPayload(): AlertCreate {
    const types = formModel.value.alertType ?? []
    return {
        id: alertId.value ?? undefined,
        alertName: formModel.value.alertName ?? '',
        userId: userStore.getUserInfo?.userId ?? '',
        alertType: types.reduce((sum, value) => sum | Number(value), 0),
        emailParams: { contacts: formModel.value.alertEmail ?? '' },
        dingTalkParams: {
            token: formModel.value.dingtalkToken,
            contacts: formModel.value.alertDingUser,
            isAtAll: formModel.value.dingtalkIsAtAll ?? false,
            alertDingURL: formModel.value.alertDingURL,
            secretEnable: formModel.value.dingtalkSecretEnable,
            secretToken: formModel.value.dingtalkSecretToken,
        },
        weComParams: { token: formModel.value.weToken ?? '' },
        larkParams: {
            token: formModel.value.larkToken ?? '',
            isAtAll: formModel.value.larkIsAtAll ?? false,
            secretEnable: formModel.value.larkSecretEnable ?? false,
            secretToken: formModel.value.larkSecretToken ?? '',
        },
        isJsonType: true,
    }
}

async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
        const param = buildPayload()

        if (!isEdit.value) {
            const exists = await fetchExistsAlert({ alertName: param.alertName })
            if (!exists.isSuccess) throwApiFailure(exists, t('sys.api.apiRequestFailed'))
            if (exists.data) {
                window.$dialog?.error({
                    title: t('setting.alarm.fail.title'),
                    content: t('setting.alarm.fail.subTitle', [param.alertName]),
                    positiveText: t('common.okText'),
                })
                return
            }
        }

        const result = isEdit.value ? await fetchAlertUpdate(param) : await fetchAlertAdd(param)

        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))

        const payload = result.data as { data?: boolean; message?: string } | boolean | undefined
        const ok = typeof payload === 'boolean' ? payload : payload?.data
        if (!ok) {
            const message =
                typeof payload === 'object' && payload?.message
                    ? stripStreamParkMessage(payload.message)
                    : t('sys.api.apiRequestFailed')
            window.$dialog?.error({
                title: isEdit.value
                    ? t('setting.alarm.fail.update')
                    : t('setting.alarm.fail.title'),
                content: message,
                positiveText: t('common.okText'),
            })
            return
        }

        window.$message?.success(
            isEdit.value ? t('setting.alarm.success.update') : t('setting.alarm.success.title'),
        )
        closeModal()
        emit('success')
    } catch (e: any) {
        if (e?.message) window.$message?.error(e.message)
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
                <SvgIcon name="alarm" :size="25" />
                <span>{{ t('setting.alarm.alertSetting') }}</span>
            </n-space>
        </template>

        <n-form
            ref="formRef"
            :model="formModel"
            :rules="rules"
            label-placement="top"
            class="mt-16px"
        >
            <n-form-item :label="t('setting.alarm.alertName')" path="alertName">
                <n-input
                    v-model:value="formModel.alertName"
                    clearable
                    :placeholder="t('setting.alarm.alertNamePlaceHolder')"
                />
                <template #feedback>
                    {{ t('setting.alarm.alertNameTips') }}
                </template>
            </n-form-item>

            <n-form-item :label="t('setting.alarm.faultAlertType')" path="alertType">
                <n-select
                    v-model:value="formModel.alertType"
                    :placeholder="t('setting.alarm.faultAlertType')"
                    clearable
                    multiple
                    :options="alertTypeOptions"
                    :render-label="renderAlertTypeLabel"
                />
            </n-form-item>

            <template v-if="hasType('1')">
                <n-divider>
                    <n-space align="center" :size="8">
                        <SvgIcon name="mail" :size="20" />
                        <span>{{ t('setting.alarm.email') }}</span>
                    </n-space>
                </n-divider>
                <n-form-item :label="t('setting.alarm.alertEmail')" path="alertEmail">
                    <n-input
                        v-model:value="formModel.alertEmail"
                        :placeholder="t('setting.alarm.alertEmailPlaceholder')"
                    />
                </n-form-item>
            </template>

            <template v-if="hasType('2')">
                <n-divider>
                    <n-space align="center" :size="8">
                        <SvgIcon name="dingtalk" :size="20" />
                        <span>{{ t('setting.alarm.dingTalk') }}</span>
                    </n-space>
                </n-divider>
                <n-form-item :label="t('setting.alarm.dingTalkUrl')" path="alertDingURL">
                    <n-input
                        v-model:value="formModel.alertDingURL"
                        :placeholder="t('setting.alarm.dingTalkPlaceholder')"
                        clearable
                    />
                </n-form-item>
                <n-form-item :label="t('setting.alarm.dingtalkAccessToken')" path="dingtalkToken">
                    <n-input
                        v-model:value="formModel.dingtalkToken"
                        :placeholder="t('setting.alarm.dingtalkAccessTokenPlaceholder')"
                    />
                </n-form-item>
                <n-form-item :label="t('setting.alarm.secretEnable')" path="dingtalkSecretEnable">
                    <n-switch v-model:value="formModel.dingtalkSecretEnable">
                        <template #checked>ON</template>
                        <template #unchecked>OFF</template>
                    </n-switch>
                    <template #feedback>
                        {{ t('setting.alarm.secretTokenEnableHelpMessage') }}
                    </template>
                </n-form-item>
                <n-form-item
                    v-if="formModel.dingtalkSecretEnable"
                    :label="t('setting.alarm.secretToken')"
                    path="dingtalkSecretToken"
                >
                    <n-input
                        v-model:value="formModel.dingtalkSecretToken"
                        :placeholder="t('setting.alarm.secretTokenPlaceholder')"
                    />
                </n-form-item>
                <n-form-item :label="t('setting.alarm.dingTalkUser')" path="alertDingUser">
                    <n-input
                        v-model:value="formModel.alertDingUser"
                        :placeholder="t('setting.alarm.dingTalkUserPlaceholder')"
                    />
                </n-form-item>
                <n-form-item :label="t('setting.alarm.dingtalkIsAtAll')" path="dingtalkIsAtAll">
                    <n-switch v-model:value="formModel.dingtalkIsAtAll">
                        <template #checked>ON</template>
                        <template #unchecked>OFF</template>
                    </n-switch>
                    <template #feedback>
                        {{ t('setting.alarm.whetherNotifyAll') }}
                    </template>
                </n-form-item>
            </template>

            <template v-if="hasType('4')">
                <n-divider>
                    <n-space align="center" :size="8">
                        <SvgIcon name="wecom" :size="20" />
                        <span>{{ t('setting.alarm.weChat') }}</span>
                    </n-space>
                </n-divider>
                <n-form-item :label="t('setting.alarm.weChattoken')" path="weToken">
                    <n-input
                        v-model:value="formModel.weToken"
                        type="textarea"
                        :rows="4"
                        :placeholder="t('setting.alarm.weChattokenPlaceholder')"
                    />
                </n-form-item>
            </template>

            <template v-if="hasType('8')">
                <n-divider>
                    <n-space align="center" :size="8">
                        <SvgIcon name="message" :size="20" />
                        <span>{{ t('setting.alarm.sms') }}</span>
                    </n-space>
                </n-divider>
                <n-form-item :label="t('setting.alarm.sms')" path="alertSms">
                    <n-input
                        v-model:value="formModel.alertSms"
                        :placeholder="t('setting.alarm.smsPlaceholder')"
                        clearable
                    />
                </n-form-item>
                <n-form-item :label="t('setting.alarm.smsTemplate')" path="alertSmsTemplate">
                    <n-input
                        v-model:value="formModel.alertSmsTemplate"
                        type="textarea"
                        :rows="4"
                        :placeholder="t('setting.alarm.smsTemplateIsRequired')"
                    />
                </n-form-item>
            </template>

            <template v-if="hasType('16')">
                <n-divider>
                    <n-space align="center" :size="8">
                        <SvgIcon name="lark" :size="20" />
                        <span>{{ t('setting.alarm.lark') }}</span>
                    </n-space>
                </n-divider>
                <n-form-item :label="t('setting.alarm.larkToken')" path="larkToken">
                    <n-input
                        v-model:value="formModel.larkToken"
                        :placeholder="t('setting.alarm.larkTokenPlaceholder')"
                        clearable
                    />
                </n-form-item>
                <n-form-item :label="t('setting.alarm.larkIsAtAll')" path="larkIsAtAll">
                    <n-switch v-model:value="formModel.larkIsAtAll">
                        <template #checked>ON</template>
                        <template #unchecked>OFF</template>
                    </n-switch>
                    <template #feedback>
                        {{ t('setting.alarm.whetherNotifyAll') }}
                    </template>
                </n-form-item>
                <n-form-item :label="t('setting.alarm.larkSecretEnable')" path="larkSecretEnable">
                    <n-switch v-model:value="formModel.larkSecretEnable">
                        <template #checked>ON</template>
                        <template #unchecked>OFF</template>
                    </n-switch>
                    <template #feedback>
                        {{ t('setting.alarm.larkTokenEnableHelpMessage') }}
                    </template>
                </n-form-item>
                <n-form-item
                    v-if="formModel.larkSecretEnable"
                    :label="t('setting.alarm.larkSecretToken')"
                    path="larkSecretToken"
                >
                    <n-input
                        v-model:value="formModel.larkSecretToken"
                        :placeholder="t('setting.alarm.larkSecretTokenPlaceholder')"
                    />
                </n-form-item>
            </template>
        </n-form>

        <template #footer>
            <n-space justify="end">
                <n-button class="e2e-alert-cancel-btn" @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button
                    class="e2e-alert-submit-btn"
                    type="primary"
                    :loading="submitting"
                    @click="handleSubmit"
                >
                    {{ t('common.submitText') }}
                </n-button>
            </n-space>
        </template>
    </n-modal>
</template>
