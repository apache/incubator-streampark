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
import type { SystemSetting } from '@/types/api/setting/types/setting.type'
import type { SettingFormType } from './components/SettingFormModal.vue'
import { fetchSystemSettingAll, fetchSystemSettingUpdate } from '@/service'
import SettingFormModal from './components/SettingFormModal.vue'
import SettingList from './components/SettingList.vue'

defineOptions({ name: 'SystemSetting' })

const { t } = useI18n()
const settings = ref<SystemSetting[]>([])
const collapseActive = ref(['1', '2', '3', '4'])

const formModalVisible = ref(false)
const formModalType = ref<SettingFormType | null>(null)

const settingsList = computed(() => {
    const filterValue = (key: string) => settings.value.filter((i) => i.settingKey.includes(key))
    return [
        {
            key: 1,
            title: t('setting.system.systemSettingItems.mavenSetting.name'),
            isPassword: (item: SystemSetting) =>
                item.settingKey === 'streampark.maven.auth.password',
            data: filterValue('streampark.maven'),
        },
        {
            key: 2,
            title: t('setting.system.systemSettingItems.dockerSetting.name'),
            isPassword: (item: SystemSetting) => item.settingKey === 'docker.register.password',
            data: filterValue('docker.register'),
        },
        {
            key: 3,
            title: t('setting.system.systemSettingItems.emailSetting.name'),
            isPassword: (item: SystemSetting) => item.settingKey === 'alert.email.password',
            data: filterValue('alert.email'),
        },
        {
            key: 4,
            title: t('setting.system.systemSettingItems.ingressSetting.name'),
            isPassword: () => false,
            data: filterValue('ingress.mode'),
        },
    ]
})

async function loadSettings() {
    const result = await fetchSystemSettingAll()
    if (result.isSuccess) settings.value = result.data ?? []
}

async function handleSettingUpdate(record: SystemSetting) {
    try {
        const result = await fetchSystemSettingUpdate({
            settingKey: record.settingKey,
            settingValue: record.settingValue !== 'true',
        })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        window.$message?.success(t('setting.system.update.success'))
        loadSettings()
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    }
}

function handleOpenForm(type: SettingFormType) {
    formModalType.value = type
    formModalVisible.value = true
}

onMounted(loadSettings)
</script>

<template>
    <n-card :bordered="false" class="h-full">
        <n-text strong>{{ t('setting.system.systemSetting') }}</n-text>
        <n-collapse v-model:expanded-names="collapseActive" class="mt-16px">
            <n-collapse-item
                v-for="item in settingsList"
                :key="item.key"
                :title="item.title"
                :name="String(item.key)"
            >
                <SettingList
                    :data="item.data"
                    :is-password="item.isPassword"
                    @update-value="handleSettingUpdate"
                    @open-form="handleOpenForm"
                    @reload="loadSettings"
                />
            </n-collapse-item>
        </n-collapse>
        <SettingFormModal
            v-model:show="formModalVisible"
            :type="formModalType"
            @success="loadSettings"
        />
    </n-card>
</template>
