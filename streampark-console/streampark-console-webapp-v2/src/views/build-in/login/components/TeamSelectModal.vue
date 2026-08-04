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

const show = computed({
    get: () => authStore.pendingTeamSelect,
    set: (val: boolean) => {
        authStore.pendingTeamSelect = val
    },
})

const teamId = ref<string | null>(null)
const loading = ref(false)

const options = computed(() =>
    userStore.getTeamList.map((item) => ({ label: item.label, value: item.value })),
)

async function handleConfirm() {
    if (!teamId.value) {
        window.$message?.warning(t('sys.login.selectTeam'))
        return false
    }
    loading.value = true
    try {
        await authStore.confirmTeam(teamId.value)
        window.$message?.success(t('sys.login.loginSuccessTitle'))
        return true
    } catch (e: any) {
        showCatchError(e, t('sys.api.apiRequestFailed'))
        return false
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <n-modal
        v-model:show="show"
        preset="dialog"
        :title="t('sys.login.selectTeam')"
        :positive-text="t('common.okText')"
        :negative-text="t('common.cancelText')"
        :loading="loading"
        :mask-closable="false"
        :close-on-esc="false"
        :on-positive-click="handleConfirm"
    >
        <n-select
            v-model:value="teamId"
            :options="options"
            :placeholder="t('sys.login.selectTeam')"
            class="mt-4"
        />
    </n-modal>
</template>
