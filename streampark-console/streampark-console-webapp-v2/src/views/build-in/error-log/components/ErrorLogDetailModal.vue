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
import type { ErrorLogInfo } from '/#/store'

defineProps<{
    show: boolean
    record: ErrorLogInfo | null
}>()

const emit = defineEmits<{
    'update:show': [value: boolean]
}>()

const { t } = useI18n()
</script>

<template>
    <n-modal
        :show="show"
        preset="card"
        :style="{ width: '800px' }"
        :title="t('sys.errorLog.tableActionDesc')"
        @update:show="emit('update:show', $event)"
    >
        <n-descriptions v-if="record" bordered :column="1" label-placement="left" size="small">
            <n-descriptions-item :label="t('sys.errorLog.tableColumnType')">
                {{ record.type }}
            </n-descriptions-item>
            <n-descriptions-item label="URL">
                {{ record.url }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('sys.errorLog.tableColumnDate')">
                {{ record.time }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('sys.errorLog.tableColumnFile')">
                {{ record.file }}
            </n-descriptions-item>
            <n-descriptions-item label="Name">
                {{ record.name }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('sys.errorLog.tableColumnMsg')">
                {{ record.message }}
            </n-descriptions-item>
            <n-descriptions-item :label="t('sys.errorLog.tableColumnStackMsg')">
                <pre class="stack-pre">{{ record.stack }}</pre>
            </n-descriptions-item>
        </n-descriptions>
    </n-modal>
</template>

<style scoped>
.stack-pre {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
    font-size: 12px;
    max-height: 320px;
    overflow: auto;
}
</style>
