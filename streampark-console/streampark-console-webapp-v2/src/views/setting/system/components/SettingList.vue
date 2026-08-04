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
import { fetchSystemSettingUpdate } from '@/service'
import { SvgIcon } from '@/components/Icon'

const props = defineProps<{
    data: SystemSetting[]
    isPassword: (item: SystemSetting) => boolean
}>()

const emit = defineEmits<{
    'update-value': [record: SystemSetting]
    reload: []
    'open-form': [type: 'docker' | 'email']
}>()

const { t } = useI18n()

const avatarMap: Record<string, string> = {
    'streampark.maven.settings': 'settings2',
    'streampark.maven.central.repository': 'maven',
    'streampark.maven.auth.user': 'user',
    'streampark.maven.auth.password': 'mvnpass',
    'docker.register.address': 'docker',
    'alert.email.from': 'mail',
    'ingress.mode.default': 'nginx',
}

const settingTitles: Record<string, string> = {
    'streampark.maven.settings': t('setting.system.title.mavenSettings'),
    'streampark.maven.central.repository': t('setting.system.title.mavenRepository'),
    'streampark.maven.auth.user': t('setting.system.title.mavenUser'),
    'streampark.maven.auth.password': t('setting.system.title.mavenPassword'),
    'docker.register.address': t('setting.system.title.docker'),
    'alert.email.from': t('setting.system.title.email'),
    'ingress.mode.default': t('setting.system.title.ingress'),
}

const settingDesc: Record<string, string> = {
    'streampark.maven.settings': t('setting.system.desc.mavenSettings'),
    'streampark.maven.central.repository': t('setting.system.desc.mavenRepository'),
    'streampark.maven.auth.user': t('setting.system.desc.mavenUser'),
    'streampark.maven.auth.password': t('setting.system.desc.mavenPassword'),
    'docker.register.address': t('setting.system.desc.docker'),
    'alert.email.from': t('setting.system.desc.email'),
    'ingress.mode.default': t('setting.system.desc.ingress'),
}

function handleSwitch(record: SystemSetting) {
    emit('update-value', record)
}

function handleEdit(record: SystemSetting) {
    if (record.settingKey.startsWith('docker.register')) emit('open-form', 'docker')
    else if (record.settingKey.startsWith('alert.email')) emit('open-form', 'email')
    else record.editable = !record.editable
}

async function handleSubmit(record: SystemSetting) {
    record.submitting = false
    record.editable = false
    await fetchSystemSettingUpdate({
        settingKey: record.settingKey,
        settingValue: record.settingValue,
    })
    window.$message?.success(t('setting.system.update.success'))
    emit('reload')
}

const visibleItems = computed(() => props.data.filter((item) => avatarMap[item.settingKey]))
</script>

<template>
    <n-list>
        <n-list-item v-for="item in visibleItems" :key="item.settingKey">
            <div class="setting-row">
                <n-thing
                    :title="settingTitles[item.settingKey]"
                    :description="settingDesc[item.settingKey]"
                >
                    <template #avatar>
                        <div class="avatar">
                            <SvgIcon :name="avatarMap[item.settingKey]" />
                        </div>
                    </template>
                </n-thing>
                <div class="setting-value">
                    <template v-if="item.type === 1">
                        <n-input
                            v-if="item.editable"
                            v-model:value="item.settingValue"
                            :type="isPassword(item) ? 'password' : 'text'"
                            :placeholder="t('common.inputText')"
                        />
                        <span v-else>
                            <template v-if="isPassword(item) && item.settingValue !== null"
                                >********</template
                            >
                            <template v-else>{{ item.settingValue }}</template>
                        </span>
                    </template>
                    <n-switch
                        v-else
                        :value="item.settingValue === 'true'"
                        @update:value="handleSwitch(item)"
                    />
                </div>
                <div v-if="item.type === 1" v-auth="'setting:update'">
                    <n-button
                        v-if="!item.submitting"
                        circle
                        type="primary"
                        @click="handleEdit(item)"
                    >
                        <SvgIcon name="edit" />
                    </n-button>
                    <n-button v-else circle type="primary" @click="handleSubmit(item)">
                        <SvgIcon name="save" />
                    </n-button>
                </div>
            </div>
        </n-list-item>
    </n-list>
</template>

<style scoped>
.setting-row {
    display: flex;
    align-items: center;
    gap: 16px;
    width: 100%;
}

.setting-value {
    flex: 1;
    min-width: 180px;
}

.avatar {
    width: 70px;
    height: 70px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: var(--action-color);
}
</style>
