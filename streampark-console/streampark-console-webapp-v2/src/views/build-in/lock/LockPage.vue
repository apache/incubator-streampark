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
import { ionIconComponent } from '@/utils/ionIcon'
import { useNow } from './useNow'
import { useLockStore } from '@/store/modules/lock'
import { useUserStore } from '@/store/modules/user'
import headerImg from '@/assets/svg/default-avator.svg'

const { t } = useI18n()
const lockStore = useLockStore()
const userStore = useUserStore()

const password = ref('')
const loading = ref(false)
const errMsg = ref(false)
const showDate = ref(true)

const { hour, month, minute, meridiem, year, day, week } = useNow(true)

const userinfo = computed(() => userStore.getUserInfo || {})
const avatar = computed(() =>
  userinfo.value.avatar && userinfo.value.avatar !== 'default.jpg'
    ? userinfo.value.avatar
    : headerImg,
)

async function unLock() {
  if (!password.value)
    return
  loading.value = true
  try {
    const ok = await lockStore.unLock(password.value)
    errMsg.value = !ok
  }
  finally {
    loading.value = false
  }
}

function goLogin() {
  userStore.logout(true)
  lockStore.resetLockInfo()
}

function handleShowForm(show = false) {
  showDate.value = show
}
</script>

<template>
  <div class="lock-page">
    <div
      v-show="showDate"
      class="unlock-trigger"
      @click="handleShowForm(false)"
    >
      <n-icon :component="ionIconComponent('LockClosedOutline')" :size="22" />
      <span>{{ t('sys.lock.unlock') }}</span>
    </div>

    <div class="clock-row">
      <div class="clock-block hour-block">
        <span>{{ hour }}</span>
        <span v-show="showDate" class="meridiem">{{ meridiem }}</span>
      </div>
      <div class="clock-block minute-block">
        <span>{{ minute }}</span>
      </div>
    </div>

    <transition name="fade-slide">
      <div v-show="!showDate" class="lock-entry">
        <div class="lock-entry-content">
          <div class="lock-entry-header">
            <img :src="avatar" class="avatar" alt="">
            <p>{{ userinfo.username }}</p>
          </div>
          <n-input
            v-model:value="password"
            type="password"
            show-password-on="click"
            :placeholder="t('sys.lock.placeholder')"
          />
          <n-text v-if="errMsg" type="error" class="err-msg">
            {{ t('sys.lock.alert') }}
          </n-text>
          <div class="lock-entry-footer">
            <n-button text type="primary" size="small" :disabled="loading" @click="handleShowForm(true)">
              {{ t('common.back') }}
            </n-button>
            <n-button text type="primary" size="small" :disabled="loading" @click="goLogin">
              {{ t('sys.lock.backToLogin') }}
            </n-button>
            <n-button text type="primary" size="small" :loading="loading" @click="unLock">
              {{ t('sys.lock.entry') }}
            </n-button>
          </div>
        </div>
      </div>
    </transition>

    <div class="lock-footer">
      <div v-show="!showDate" class="footer-time">
        {{ hour }}:{{ minute }} <span>{{ meridiem }}</span>
      </div>
      <div>{{ year }}/{{ month }}/{{ day }} {{ week }}</div>
    </div>
  </div>
</template>

<style scoped>
.lock-page {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
  color: #bababa;
}

.unlock-trigger {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.clock-row {
  display: flex;
  align-items: center;
  gap: 40px;
}

.clock-block {
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(40vw, 320px);
  height: min(40vh, 320px);
  border-radius: 30px;
  background: #141313;
  font-size: clamp(72px, 16vw, 220px);
  font-weight: 700;
  position: relative;
}

.meridiem {
  position: absolute;
  top: 16px;
  left: 16px;
  font-size: 18px;
}

.lock-entry {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(0 0 0 / 50%);
  backdrop-filter: blur(8px);
}

.lock-entry-content {
  width: 260px;
}

.lock-entry-header {
  text-align: center;
  margin-bottom: 12px;
}

.avatar {
  width: 70px;
  height: 70px;
  border-radius: 50%;
}

.lock-entry-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
}

.err-msg {
  display: block;
  margin-top: 8px;
  font-size: 12px;
}

.lock-footer {
  position: absolute;
  bottom: 24px;
  width: 100%;
  text-align: center;
  color: #d1d5db;
}

.footer-time {
  font-size: 28px;
  margin-bottom: 8px;
}
</style>
