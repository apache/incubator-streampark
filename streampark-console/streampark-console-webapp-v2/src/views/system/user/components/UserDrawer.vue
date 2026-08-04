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
import type { FormInst, FormRules } from 'naive-ui'
import type { UserListRecord } from '@/types/api/system/model/userModel'
import { fetchAddUser, fetchCheckUserName, fetchUpdateUser } from '@/service'
import { FormTypeEnum, GenderEnum, StatusEnum, UserTypeEnum } from '../../shared/constants'

const props = defineProps<{
    show: boolean
    formType: FormTypeEnum
    record?: UserListRecord | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    success: []
}>()

const { t } = useI18n()

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)

const defaultForm = () => ({
    userId: '',
    username: '',
    nickName: '',
    password: '',
    email: '',
    userType: UserTypeEnum.USER,
    status: StatusEnum.Effective,
    sex: GenderEnum.Male,
    description: '',
})

type UserFormModel = ReturnType<typeof defaultForm>

const formModel = ref(defaultForm())

const isCreate = computed(() => props.formType === FormTypeEnum.Create)
const isView = computed(() => props.formType === FormTypeEnum.View)

const drawerTitle = computed(
    () =>
        ({
            [FormTypeEnum.Create]: t('system.user.form.create'),
            [FormTypeEnum.Edit]: t('system.user.form.edit'),
            [FormTypeEnum.View]: t('system.user.form.view'),
        })[props.formType],
)

const rules = computed<FormRules>(() => ({
    username: [
        { required: isCreate.value, message: t('system.user.form.required'), trigger: 'blur' },
        { min: 2, max: 20, message: t('system.user.form.min'), trigger: 'blur' },
        {
            asyncValidator: async (_rule, value: string) => {
                if (!isCreate.value || !value || value.length < 2 || value.length > 20) return
                const result = await fetchCheckUserName({ username: value })
                if (!result.isSuccess || !result.data) throw new Error(t('system.user.form.exist'))
            },
            trigger: 'blur',
        },
    ],
    nickName: [
        { required: isCreate.value, message: t('system.user.form.required'), trigger: 'blur' },
    ],
    password: isCreate.value
        ? [
              { required: true, message: t('system.user.form.passwordRequire'), trigger: 'blur' },
              { min: 8, message: t('system.user.form.passwordHelp'), trigger: 'blur' },
          ]
        : [],
    email: [
        { type: 'email', message: t('system.user.form.email'), trigger: 'blur' },
        { max: 50, message: t('system.user.form.maxEmail'), trigger: 'blur' },
    ],
    userType: [{ required: true, message: t('system.user.form.required'), trigger: 'change' }],
    status: [{ required: true, message: t('system.user.form.required'), trigger: 'change' }],
    sex: [{ required: true, message: t('system.user.form.required'), trigger: 'change' }],
}))

watch(
    () => [props.show, props.formType, props.record] as const,
    ([show, formType, record]) => {
        if (!show) return
        formModel.value = defaultForm()
        if (formType !== FormTypeEnum.Create && record)
            formModel.value = { ...defaultForm(), ...record, password: '' } as UserFormModel
        nextTick(() => formRef.value?.restoreValidation())
    },
    { immediate: true },
)

function closeDrawer() {
    emit('update:show', false)
}

async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
        const payload = { ...formModel.value }
        if (!isCreate.value) delete (payload as Recordable).password

        const result = isCreate.value ? await fetchAddUser(payload) : await fetchUpdateUser(payload)

        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))

        closeDrawer()
        emit('success')
    } catch (e: any) {
        if (e?.message) window.$message?.error(e.message)
    } finally {
        submitting.value = false
    }
}
</script>

<template>
    <n-drawer
        :show="show"
        :width="480"
        placement="right"
        @update:show="emit('update:show', $event)"
    >
        <n-drawer-content :title="drawerTitle" closable>
            <n-form
                ref="formRef"
                :model="formModel"
                :rules="rules"
                label-placement="top"
                :disabled="isView"
            >
                <n-form-item :label="t('system.user.form.userName')" path="username">
                    <n-input
                        id="formUserName"
                        v-model:value="formModel.username"
                        :disabled="!isCreate"
                    />
                </n-form-item>

                <n-form-item :label="t('system.user.form.nickName')" path="nickName">
                    <n-input v-model:value="formModel.nickName" :disabled="!isCreate" />
                </n-form-item>

                <n-form-item
                    v-if="isCreate"
                    :label="t('system.user.form.password')"
                    path="password"
                >
                    <n-input
                        v-model:value="formModel.password"
                        type="password"
                        show-password-on="click"
                    />
                    <template #feedback>
                        {{ t('system.user.form.passwordHelp') }}
                    </template>
                </n-form-item>

                <n-form-item label="E-Mail" path="email">
                    <n-input v-model:value="formModel.email" />
                </n-form-item>

                <n-form-item :label="t('system.user.form.userType')" path="userType">
                    <n-select
                        v-model:value="formModel.userType"
                        :options="[
                            { label: 'ADMIN', value: UserTypeEnum.ADMIN },
                            { label: 'USER', value: UserTypeEnum.USER },
                        ]"
                    />
                </n-form-item>

                <n-form-item :label="t('system.user.form.status')" path="status">
                    <n-radio-group v-model:value="formModel.status">
                        <n-space>
                            <n-radio :value="StatusEnum.Locked">
                                {{ t('system.user.locked') }}
                            </n-radio>
                            <n-radio :value="StatusEnum.Effective">
                                {{ t('system.user.effective') }}
                            </n-radio>
                        </n-space>
                    </n-radio-group>
                </n-form-item>

                <n-form-item :label="t('system.user.form.gender')" path="sex">
                    <n-radio-group v-model:value="formModel.sex">
                        <n-space>
                            <n-radio :value="GenderEnum.Male">
                                {{ t('system.user.male') }}
                            </n-radio>
                            <n-radio :value="GenderEnum.Female">
                                {{ t('system.user.female') }}
                            </n-radio>
                            <n-radio :value="GenderEnum.Other">
                                {{ t('system.user.secret') }}
                            </n-radio>
                        </n-space>
                    </n-radio-group>
                </n-form-item>

                <n-form-item v-if="isCreate" :label="t('common.description')" path="description">
                    <n-input v-model:value="formModel.description" type="textarea" :rows="4" />
                </n-form-item>
            </n-form>

            <template v-if="!isView" #footer>
                <n-space justify="end">
                    <n-button class="e2e-user-cancel-btn" @click="closeDrawer">
                        {{ t('common.cancelText') }}
                    </n-button>
                    <n-button
                        class="e2e-user-submit-btn"
                        type="primary"
                        :loading="submitting"
                        @click="handleSubmit"
                    >
                        {{ t('common.submitText') }}
                    </n-button>
                </n-space>
            </template>
        </n-drawer-content>
    </n-drawer>
</template>
