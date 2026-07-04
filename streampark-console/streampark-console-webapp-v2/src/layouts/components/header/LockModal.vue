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
import { useLockStore } from '@/store/modules/lock'
import { useUserStore } from '@/store/modules/user'
import headerImg from '@/assets/svg/default-avator.svg'

const show = defineModel<boolean>('show', { default: false })

const { t } = useI18n()
const lockStore = useLockStore()
const userStore = useUserStore()

const password = ref('')

const username = computed(() => userStore.getUserInfo?.username ?? '')
const avatar = computed(() => {
  const value = userStore.getUserInfo?.avatar
  return value && value !== 'default.jpg' ? value : headerImg
})

function handleClose() {
  password.value = ''
}

async function handleLock() {
  if (!password.value)
    return
  lockStore.setLockInfo({
    isLock: true,
    pwd: password.value,
  })
  password.value = ''
  show.value = false
}
</script>

<template>
  <n-modal
    v-model:show="show"
    preset="card"
    :style="{ width: '360px' }"
    :title="t('layout.header.lockScreen')"
    @after-leave="handleClose"
  >
    <div class="lock-modal-body">
      <div class="lock-modal-header">
        <img :src="avatar" class="avatar" alt="">
        <p>{{ username }}</p>
      </div>
      <n-form label-placement="top">
        <n-form-item :label="t('layout.header.lockScreenPassword')" required>
          <n-input
            v-model:value="password"
            type="password"
            show-password-on="click"
            @keyup.enter="handleLock"
          />
        </n-form-item>
      </n-form>
    </div>
    <template #footer>
      <n-space justify="end">
        <n-button @click="show = false">
          {{ t('common.cancelText') }}
        </n-button>
        <n-button type="primary" @click="handleLock">
          {{ t('layout.header.lockScreenBtn') }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<style scoped>
.lock-modal-body {
  padding-top: 8px;
}

.lock-modal-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.avatar {
  width: 70px;
  height: 70px;
  border-radius: 50%;
}
</style>
