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
import type { AlertSetting } from '@/types/api/setting/types/alert.type'
import { SvgIcon } from '@/components/Icon'
import { alertTypes } from './constants'

const props = defineProps<{
    alertType: string
    alertSource: AlertSetting
}>()

const { t } = useI18n()

const typeMap = computed(() => alertTypes(t))

const emailInfo = computed(() => JSON.parse(props.alertSource.emailParams || '{}'))
const dingTalk = computed(() => JSON.parse(props.alertSource.dingTalkParams || '{}'))
const weChat = computed(() => JSON.parse(props.alertSource.weComParams || '{}'))
const lark = computed(() => JSON.parse(props.alertSource.larkParams || '{}'))

function desensitization(dataString: string) {
    return String(dataString).replace(/^(.{4})(?:.+)(.{4})$/, '$1********$2')
}
</script>

<template>
    <div class="alert-type-info mt-10px flex cursor-pointer items-center text-16px">
        <div class="flex items-center">
            <SvgIcon :name="typeMap[alertType]?.icon" :size="20" class="!align-middle" />
            <span class="pl-10px">
                {{ typeMap[alertType]?.name }}
            </span>
        </div>
    </div>
    <n-descriptions size="small" :column="1" class="mt-10px pl-15px">
        <template v-if="alertType === '1'">
            <n-descriptions-item :label="t('setting.alarm.alertEmail')">
                <span class="text-blue-500">{{ emailInfo.contacts || '' }}</span>
            </n-descriptions-item>
        </template>
        <template v-else-if="alertType === '2'">
            <n-descriptions-item :label="t('setting.alarm.dingTalkUser')">
                {{ dingTalk.contacts || '' }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('setting.alarm.larkIsAtAll')">
                <n-tag :type="dingTalk.isAtAll ? 'success' : 'error'" class="!leading-20px">
                    {{ dingTalk.isAtAll }}
                </n-tag>
            </n-descriptions-item>
        </template>
        <template v-else-if="alertType === '4'">
            <n-descriptions-item :label="t('setting.alarm.weChattoken')">
                {{ desensitization(weChat.token || '') }}
            </n-descriptions-item>
        </template>
        <template v-else-if="alertType === '16'">
            <n-descriptions-item :label="t('setting.alarm.larkIsAtAll')">
                <n-tag :type="lark.isAtAll ? 'success' : 'error'" class="!leading-20px">
                    {{ lark.isAtAll }}
                </n-tag>
            </n-descriptions-item>
        </template>
    </n-descriptions>
</template>

<style scoped>
.alert-type-info::before {
    content: '';
    width: 0;
    height: 20px;
    margin-top: 2px;
    border: 2px solid #24c6dc;
    border-radius: 2px;
    transform: translateX(-10px);
}
</style>
