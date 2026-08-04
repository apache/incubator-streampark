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
import { useAuthStore } from '@/store'
import { useUserStoreWithOut } from '@/store/modules/user'

const { t } = useI18n()
const authStore = useAuthStore()
const userStore = useUserStoreWithOut()

const teamId = computed({
    get: () => userStore.teamId || null,
    set: (val: string | null) => {
        if (val) userStore.teamId = val
    },
})

const options = computed(() =>
    userStore.getTeamList.map((item) => ({ label: item.label, value: item.value })),
)
const loading = ref(false)

async function handleTeamChange(value: string) {
    if (!value || value === userStore.teamId) return
    loading.value = true
    try {
        await authStore.switchTeam(value)
        window.$message?.success(t('common.operationSuccess'))
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <n-select
        v-if="options.length > 0"
        v-model:value="teamId"
        size="small"
        class="w-160px"
        :options="options"
        :loading="loading"
        :placeholder="t('sys.login.selectTeam')"
        @update:value="handleTeamChange"
    />
</template>
