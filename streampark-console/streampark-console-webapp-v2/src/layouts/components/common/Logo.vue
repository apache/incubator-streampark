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
import { useAppStore } from '@/store'
import { useUserStoreWithOut } from '@/store/modules/user'
import logoIcon from '@/assets/images/logo.png'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStoreWithOut()

const appName = import.meta.env.VITE_APP_NAME

/** Collapsed sidebar shows icon only; expanded shows icon + Apache StreamPark title. */
const showIconOnly = computed(() => {
  if (['sidebar', 'mixed-sidebar', 'horizontal', 'vertical'].includes(appStore.layoutMode))
    return appStore.collapsed
  if (['two-column', 'mixed-two-column'].includes(appStore.layoutMode))
    return true
  return appStore.collapsed
})

function goHome() {
  const home = userStore.getUserInfo?.homePath || import.meta.env.VITE_HOME_PATH || '/flink/app'
  router.push(home)
}
</script>

<template>
  <div
    class="streampark-logo h-60px flex-center cursor-pointer gap-2 px-10px overflow-hidden"
    @click="goHome"
  >
    <img
      :src="logoIcon"
      alt="StreamPark"
      class="streampark-logo__icon shrink-0"
    >
    <div v-show="!showIconOnly" class="streampark-logo__title min-w-0">
      <span class="streampark-logo__apache">APACHE</span>
      <span class="streampark-logo__name">{{ appName.replace(/^Apache\s+/i, '') }}</span>
    </div>
  </div>
</template>

<style scoped>
.streampark-logo {
  border-bottom: 1px solid var(--n-border-color);
}

.streampark-logo__icon {
  width: 40px;
  height: auto;
}

.streampark-logo__title {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
  color: var(--n-text-color);
}

.streampark-logo__apache {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.08em;
  opacity: 0.85;
}

.streampark-logo__name {
  font-size: 18px;
  font-weight: 300;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
