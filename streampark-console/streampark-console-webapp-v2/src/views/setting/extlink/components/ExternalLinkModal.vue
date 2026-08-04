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
import type { ExternalLink } from '@/service/api/setting/externalLink'
import { fetchExternalLinkCreate, fetchExternalLinkUpdate } from '@/service'

const props = defineProps<{
    show: boolean
    record?: ExternalLink | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
    reload: []
}>()

const { t } = useI18n()
const DEFAULT_COLOR = '#eb8c34'
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const linkId = ref('')

const formModel = ref({
    badgeLabel: '',
    badgeName: '',
    linkUrl: '',
    badgeColor: DEFAULT_COLOR,
})

const rules: FormRules = {
    badgeName: [
        {
            required: true,
            message: t('setting.externalLink.form.badgeNameIsRequired'),
            trigger: 'blur',
        },
    ],
    linkUrl: [
        {
            required: true,
            message: t('setting.externalLink.form.linkUrlIsRequired'),
            trigger: 'blur',
        },
    ],
    badgeColor: [
        {
            required: true,
            message: t('setting.externalLink.form.badgeColorIsRequired'),
            trigger: 'change',
        },
    ],
}

watch(
    () => [props.show, props.record] as const,
    ([show, record]) => {
        if (!show) return
        linkId.value = record?.id ?? ''
        formModel.value = {
            badgeLabel: record?.badgeLabel ?? '',
            badgeName: record?.badgeName ?? '',
            linkUrl: record?.linkUrl ?? '',
            badgeColor: record?.badgeColor ?? DEFAULT_COLOR,
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
        const payload = { ...formModel.value, id: linkId.value || undefined }
        const result = linkId.value
            ? await fetchExternalLinkUpdate(payload as ExternalLink)
            : await fetchExternalLinkCreate(payload as ExternalLink)
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        window.$message?.success(
            linkId.value
                ? t('setting.externalLink.operateMessage.updateLinkSuccessful')
                : t('setting.externalLink.operateMessage.createLinkSuccessful'),
        )
        closeModal()
        emit('reload')
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
        :title="t('setting.externalLink.externalLinkSetting')"
        :style="{ width: '600px' }"
        @update:show="emit('update:show', $event)"
    >
        <n-form ref="formRef" :model="formModel" :rules="rules" label-placement="top">
            <n-form-item :label="t('setting.externalLink.form.badgeLabel')" path="badgeLabel">
                <n-input
                    v-model:value="formModel.badgeLabel"
                    :placeholder="t('setting.externalLink.form.badgeLabelPlaceholder')"
                />
            </n-form-item>
            <n-form-item :label="t('setting.externalLink.form.badgeName')" path="badgeName">
                <n-input
                    v-model:value="formModel.badgeName"
                    :placeholder="t('setting.externalLink.form.badgeNamePlaceholder')"
                />
            </n-form-item>
            <n-form-item :label="t('setting.externalLink.form.linkUrl')" path="linkUrl">
                <n-input
                    v-model:value="formModel.linkUrl"
                    :placeholder="t('setting.externalLink.form.linkUrlPlaceholder')"
                />
                <template #feedback>
                    Supported variables: {job_id}, {yarn_id}, {job_name}
                </template>
            </n-form-item>
            <n-form-item :label="t('setting.externalLink.form.badgeColor')" path="badgeColor">
                <n-color-picker v-model:value="formModel.badgeColor" :show-alpha="false" />
            </n-form-item>
            <n-form-item :label="t('setting.externalLink.form.badgePreview')">
                <n-tag :color="{ color: formModel.badgeColor, textColor: '#fff' }">
                    {{ formModel.badgeLabel || formModel.badgeName || 'Preview' }}
                </n-tag>
            </n-form-item>
        </n-form>
        <template #footer>
            <n-space justify="end">
                <n-button class="e2e-extlink-cancel-btn" @click="closeModal">
                    {{ t('common.cancelText') }}
                </n-button>
                <n-button
                    class="e2e-extlink-submit-btn"
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
