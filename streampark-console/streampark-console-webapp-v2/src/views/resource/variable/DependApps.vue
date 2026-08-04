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
import type { DataTableColumns } from 'naive-ui'
import { fetchDependApps } from '@/service'
import { resolveListData } from '@/views/system/shared/utils'
defineOptions({ name: 'DependApps' })

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const tableData = ref<Recordable[]>([])

const variableCode = computed(() => String(route.query.id ?? ''))

const columns = computed<DataTableColumns<Recordable>>(() => [
    { title: t('flink.variable.depend.jobName'), key: 'jobName', width: 500 },
    { title: t('flink.variable.depend.nickName'), key: 'nickName' },
    { title: t('common.createTime'), key: 'createTime' },
])

async function loadData() {
    if (!variableCode.value) return
    loading.value = true
    try {
        const result = await fetchDependApps({ variableCode: variableCode.value })
        if (!result.isSuccess) throwApiFailure(result, t('sys.api.apiRequestFailed'))
        const { records } = resolveListData(result.data)
        tableData.value = records
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
        tableData.value = []
    } finally {
        loading.value = false
    }
}

watch(() => route.query.id, loadData, { immediate: true })
</script>

<template>
    <n-card :bordered="false" class="h-full">
        <div class="mb-16px flex items-center justify-between gap-12px">
            <n-text strong>
                {{ t('flink.variable.depend.headerTitle', [variableCode]) }}
            </n-text>
            <n-button type="primary" circle @click="router.back()">
                <template #icon>
                    <n-icon><IonIcon name="ArrowBackOutline" /></n-icon>
                </template>
            </n-button>
        </div>
        <n-data-table
            :loading="loading"
            :columns="columns"
            :data="tableData"
            :row-key="(row: Recordable) => String(row.id ?? row.jobName)"
            flex-height
            class="min-h-480px"
        />
    </n-card>
</template>
