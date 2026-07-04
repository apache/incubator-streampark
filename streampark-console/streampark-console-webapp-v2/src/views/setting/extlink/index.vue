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
import { ionIcon } from '@/utils/ionIcon'
import { SP_ICONS as I } from '@/constants/streamparkIcons'
import type { DataTableColumns } from 'naive-ui'
import type { ExternalLink } from '@/service/api/setting/externalLink'
import { fetchExternalLinkDelete, fetchExternalLinkList } from '@/service'
import ExternalLinkModal from './components/ExternalLinkModal.vue'
defineOptions({ name: 'ExternalLinkSetting' })

const { t } = useI18n()
const loading = ref(false)
const links = ref<ExternalLink[]>([])
const modalVisible = ref(false)
const editingRecord = ref<ExternalLink | null>(null)

const columns = computed<DataTableColumns<ExternalLink>>(() => [
  {
    title: t('setting.externalLink.form.badgePreview'),
    key: 'badgeColor',
    width: 160,
    render(row) {
      return h(
        NTag,
        { color: { color: row.badgeColor, textColor: '#fff' } },
        { default: () => row.badgeLabel || row.badgeName },
      )
    },
  },
  {
    title: t('setting.externalLink.form.linkUrl'),
    key: 'linkUrl',
    ellipsis: { tooltip: true },
  },
  {
    title: t('component.table.operation'),
    key: 'action',
    width: 120,
    align: 'right',
    render(row) {
      return h(NSpace, { justify: 'end', size: 4 }, {
        default: () => [
          h(NButton, {
            quaternary: true,
            size: 'small',
            class: 'e2e-extlink-edit-btn',
            onClick: () => openEdit(row),
          }, { icon: () => ionIcon(I.edit) }),
          h(NPopconfirm, {
            onPositiveClick: () => handleDelete(row.id!),
          }, {
            trigger: () => h(NButton, {
              quaternary: true,
              size: 'small',
              class: 'e2e-extlink-delete-btn',
            }, { icon: () => ionIcon(I.delete) }),
            default: () => t('setting.externalLink.confDeleteTitle'),
          }),
        ],
      })
    },
  },
])

async function loadData() {
  loading.value = true
  try {
    const result = await fetchExternalLinkList()
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    links.value = result.data ?? []
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
    links.value = []
  }
  finally {
    loading.value = false
  }
}

function openCreate() {
  editingRecord.value = null
  modalVisible.value = true
}

function openEdit(record: ExternalLink) {
  editingRecord.value = record
  modalVisible.value = true
}

async function handleDelete(id: string) {
  try {
    const result = await fetchExternalLinkDelete({ id })
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    window.$message?.success(t('common.operationSuccess'))
    loadData()
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
}

onMounted(loadData)
</script>

<template>
  <n-card :bordered="false" class="h-full">
    <div class="mb-16px flex items-center justify-between gap-12px">
      <n-text strong>
        {{ t('setting.externalLink.externalLinkSetting') }}
      </n-text>
      <n-button v-auth="'externalLink:create'" dashed @click="openCreate">
        {{ t('common.add') }}
      </n-button>
    </div>
    <n-data-table
      :loading="loading"
      :columns="columns"
      :data="links"
      :row-key="(row: ExternalLink) => row.id || row.linkUrl"
      :show-header="false"
      flex-height
      class="min-h-480px"
    />
  </n-card>
  <ExternalLinkModal
    v-model:show="modalVisible"
    :record="editingRecord"
    @reload="loadData"
  />
</template>
