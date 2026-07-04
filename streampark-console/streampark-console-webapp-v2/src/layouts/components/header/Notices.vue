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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
-->
<script setup lang="ts">
import { useWebSocket } from '@vueuse/core'
import type { NotifyItem } from '@/types/api/system/model/notifyModel'
import { fetchNotify, fetchNotifyDelete } from '@/service'
import { useUserStoreWithOut } from '@/store/modules/user'
import { isObject } from '@/utils/is'
import { resolveListData } from '@/views/system/shared/utils'

const { t } = useI18n()
const userStore = useUserStoreWithOut()

const currentTab = ref(1)
const loading = ref(false)
const exceptionList = ref<NotifyItem[]>([])
const messageList = ref<NotifyItem[]>([])

const unreadCount = computed(() =>
  [...exceptionList.value, ...messageList.value].filter(item => item.readed === 0).length,
)

const activeList = computed(() => currentTab.value === 1 ? exceptionList.value : messageList.value)

function buildWebSocketUrl(userId: string) {
  const { path, rawPath } = __URL_MAP__.api
  const httpBase = rawPath.startsWith('http') ? rawPath : `${window.location.origin}${path}`
  return `${httpBase.replace(/^http/, 'ws').replace(/\/$/, '')}/websocket/${userId}`
}

const wsUrl = computed(() => {
  const userId = userStore.getUserInfo?.userId
  if (!userStore.getToken || !userId)
    return ''
  return buildWebSocketUrl(String(userId))
})

const { data: wsData } = useWebSocket(wsUrl, {
  autoReconnect: {
    retries: 3,
    delay: 1000,
    onFailed() {
      window.$message?.warning(t('sys.api.apiRequestFailed'))
    },
  },
})

async function loadNotify(type: number) {
  loading.value = true
  try {
    const result = await fetchNotify({ type, pageNum: 1, pageSize: 40 })
    if (!result.isSuccess)
      return
    const records = resolveListData(result.data as any).records as NotifyItem[]
    if (type === 1)
      exceptionList.value = records
    else
      messageList.value = records
  }
  finally {
    loading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadNotify(1), loadNotify(2)])
}

function prependNotify(type: number, item: NotifyItem) {
  if (type === 1)
    exceptionList.value = [item, ...exceptionList.value]
  else
    messageList.value = [item, ...messageList.value]
}

async function handleDelete(id: string) {
  loading.value = true
  try {
    const result = await fetchNotifyDelete({ id })
    if (!result.isSuccess)
      throwApiFailure(result, t('sys.api.apiRequestFailed'))
    await loadNotify(currentTab.value)
  }
  catch (e: any) {
    showCatchError(e, t('sys.api.apiRequestFailed'))
  }
  finally {
    loading.value = false
  }
}

function handleItemClick(item: NotifyItem) {
  window.$dialog?.[item.type === 1 ? 'error' : 'info']({
    title: item.title,
    content: item.context,
    positiveText: t('common.delText'),
    negativeText: t('common.cancelText'),
    onPositiveClick: () => handleDelete(item.id),
  })
}

watch(wsData, (payload) => {
  if (!payload)
    return
  let item: NotifyItem
  if (typeof payload === 'string') {
    try {
      item = JSON.parse(payload) as NotifyItem
    }
    catch {
      return
    }
  }
  else if (isObject(payload)) {
    item = payload as NotifyItem
  }
  else {
    return
  }
  prependNotify(item.type, item)
  handleItemClick(item)
})

watch(currentTab, (type) => {
  if (type === 1 && !exceptionList.value.length)
    loadNotify(1)
  if (type === 2 && !messageList.value.length)
    loadNotify(2)
})

onMounted(() => {
  loadAll()
})
</script>

<template>
  <n-popover placement="bottom" trigger="click" arrow-point-to-center class="!p-0">
    <template #trigger>
      <n-tooltip placement="bottom" trigger="hover">
        <template #trigger>
          <CommonWrapper>
            <n-badge :value="unreadCount" :max="99" style="color: unset">
              <n-icon>
                <IonIcon name="NotificationsOutline" />
              </n-icon>
            </n-badge>
          </CommonWrapper>
        </template>
        <span>{{ t('app.notificationsTips') }}</span>
      </n-tooltip>
    </template>
    <n-spin :show="loading">
      <n-tabs v-model:value="currentTab" type="line" animated justify-content="space-evenly" class="w-390px">
        <n-tab-pane :name="1">
          <template #tab>
            <n-space class="w-180px" justify="center">
              {{ t('routes.basic.notice.exception') }}
              <n-badge type="error" :value="exceptionList.filter(i => i.readed === 0).length" :max="99" />
            </n-space>
          </template>
          <n-scrollbar style="max-height: 400px">
            <n-list hoverable clickable>
              <n-list-item
                v-for="item in activeList"
                :key="item.id"
                :class="{ 'opacity-50': item.readed !== 0 }"
                @click="handleItemClick(item)"
              >
                <n-thing content-indented>
                  <template #header>
                    <n-ellipsis :line-clamp="1">
                      {{ item.title }}
                    </n-ellipsis>
                  </template>
                  <template #avatar>
                    <n-icon size="24" :color="item.type === 1 ? '#d03050' : '#2080f0'">
                      <IonIcon :name="item.type === 1 ? 'WarningOutline' : 'MailOutline'" />
                    </n-icon>
                  </template>
                  <template #description>
                    <n-ellipsis :line-clamp="2">
                      {{ item.context }}
                    </n-ellipsis>
                  </template>
                  <template #footer>
                    {{ item.createTime }}
                  </template>
                </n-thing>
              </n-list-item>
              <n-empty v-if="!activeList.length" class="py-24px" />
            </n-list>
          </n-scrollbar>
        </n-tab-pane>
        <n-tab-pane :name="2">
          <template #tab>
            <n-space class="w-180px" justify="center">
              {{ t('routes.basic.notice.message') }}
              <n-badge type="info" :value="messageList.filter(i => i.readed === 0).length" :max="99" />
            </n-space>
          </template>
          <n-scrollbar style="max-height: 400px">
            <n-list hoverable clickable>
              <n-list-item
                v-for="item in activeList"
                :key="item.id"
                :class="{ 'opacity-50': item.readed !== 0 }"
                @click="handleItemClick(item)"
              >
                <n-thing content-indented>
                  <template #header>
                    <n-ellipsis :line-clamp="1">
                      {{ item.title }}
                    </n-ellipsis>
                  </template>
                  <template #avatar>
                    <n-icon size="24" color="#2080f0">
                      <IonIcon name="MailOutline" />
                    </n-icon>
                  </template>
                  <template #description>
                    <n-ellipsis :line-clamp="2">
                      {{ item.context }}
                    </n-ellipsis>
                  </template>
                  <template #footer>
                    {{ item.createTime }}
                  </template>
                </n-thing>
              </n-list-item>
              <n-empty v-if="!activeList.length" class="py-24px" />
            </n-list>
          </n-scrollbar>
        </n-tab-pane>
      </n-tabs>
    </n-spin>
  </n-popover>
</template>
