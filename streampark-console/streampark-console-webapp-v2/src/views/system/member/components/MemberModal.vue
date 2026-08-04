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
import type { MemberListRecord } from '@/types/api/system/model/memberModel'
import type { RoleListItem } from '@/types/api/base/model/systemModel'
import { fetchAddMember, fetchCandidateUsers, fetchUpdateMember } from '@/service'
import { useUserStoreWithOut } from '@/store/modules/user'

const props = defineProps<{
    show: boolean
    isUpdate: boolean
    record?: MemberListRecord | null
    roleOptions: Array<Partial<RoleListItem>>
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    success: [isUpdate: boolean]
}>()

const { t } = useI18n()
const userStore = useUserStoreWithOut()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const userOptions = ref<SelectOption[]>([])

const editParams = reactive({
    id: '',
    userId: '',
    teamId: '',
})

const formModel = ref({
    userName: null as string | null,
    roleId: null as string | null,
})

const title = computed(() =>
    props.isUpdate ? t('system.member.modifyMember') : t('system.member.addMember'),
)

const roleSelectOptions = computed<SelectOption[]>(() =>
    props.roleOptions.map((item) => {
        const roleId = (item as Recordable).roleId ?? item.id
        return {
            label: item.roleName ?? '',
            value: String(roleId ?? ''),
        }
    }),
)

const rules = computed<FormRules>(() => ({
    userName: props.isUpdate
        ? []
        : [{ required: true, message: t('system.member.userNameRequire'), trigger: 'change' }],
    roleId: [{ required: true, message: t('system.member.roleRequire'), trigger: 'change' }],
}))

async function loadCandidateUsers() {
    const teamId = userStore.getTeamId
    const result = await fetchCandidateUsers(teamId ? { teamId } : undefined)
    if (result.isSuccess && result.data) {
        userOptions.value = result.data.map((item) => ({
            label: item.username,
            value: item.username,
        }))
    }
}

watch(
    () => [props.show, props.isUpdate, props.record] as const,
    async ([show, isUpdate, record]) => {
        if (!show) return
        Object.assign(editParams, { id: '', userId: '', teamId: userStore.getTeamId ?? '' })
        formModel.value = { userName: null, roleId: null }
        if (isUpdate && record) {
            Object.assign(editParams, {
                id: record.id,
                userId: record.userId,
                teamId: userStore.getTeamId ?? '',
            })
            formModel.value = {
                userName: record.userName,
                roleId: String(record.roleId),
            }
        } else {
            await loadCandidateUsers()
        }
        nextTick(() => formRef.value?.restoreValidation())
    },
    { immediate: true },
)

function closeModal() {
    emit('update:show', false)
}

async function handleSubmit() {
    await formRef.value?.validate()
    submitting.value = true
    try {
        const payload = {
            userName: formModel.value.userName!,
            roleId: Number(formModel.value.roleId),
        }
        const result = props.isUpdate
            ? await fetchUpdateMember({ ...editParams, ...payload })
            : await fetchAddMember(payload)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        closeModal()
        emit('success', props.isUpdate)
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
        :title="title"
        :style="{ width: '600px' }"
        @update:show="emit('update:show', $event)"
    >
        <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
            <n-form-item :label="t('system.member.table.userName')" path="userName">
                <n-select
                    v-model:value="formModel.userName"
                    :disabled="isUpdate"
                    :options="userOptions"
                    filterable
                    :placeholder="t('system.member.userNameRequire')"
                />
            </n-form-item>
            <n-form-item :label="t('system.member.table.roleName')" path="roleId">
                <n-select
                    v-model:value="formModel.roleId"
                    :options="roleSelectOptions"
                    :placeholder="t('system.member.roleRequire')"
                />
            </n-form-item>
        </n-form>
        <template #footer>
            <n-space justify="end">
                <n-button class="e2e-member-cancel-btn" @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button
                    class="e2e-member-submit-btn"
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
