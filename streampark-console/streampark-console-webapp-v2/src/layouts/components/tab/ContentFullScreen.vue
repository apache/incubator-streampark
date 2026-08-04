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
import { onKeyStroke } from '@vueuse/core'
import { useAppStore } from '@/store'
import type { LayoutMode } from '@/layouts/types'

const appStore = useAppStore()
const { layoutMode } = storeToRefs(appStore)

const previousLayoutMode = ref<LayoutMode>(appStore.layoutMode)

let stopEscListener: (() => void) | null = null

function enterFullContent() {
    previousLayoutMode.value = appStore.layoutMode
    appStore.layoutMode = 'full-content'
}

function exitFullContent() {
    let mode = previousLayoutMode.value
    if (mode === 'full-content' || !mode) mode = 'vertical'
    appStore.layoutMode = mode
}

watch(
    layoutMode,
    (mode) => {
        stopEscListener?.()
        stopEscListener = null
        if (mode === 'full-content') {
            stopEscListener = onKeyStroke('Escape', () => {
                exitFullContent()
            })
        }
    },
    { immediate: true },
)

onUnmounted(() => stopEscListener?.())
</script>

<template>
    <Teleport
        to="#sp-content-fullscreen-trigger"
        :disabled="layoutMode === 'full-content' || appStore.isMobile"
    >
        <n-tooltip placement="bottom" trigger="hover">
            <template #trigger>
                <CommonWrapper @click="enterFullContent">
                    <n-icon>
                        <IonIcon name="ExpandOutline" />
                    </n-icon>
                </CommonWrapper>
            </template>
            {{ $t('app.togglContentFullScreen') }}
        </n-tooltip>
    </Teleport>

    <Teleport to="body">
        <div v-if="layoutMode === 'full-content'" class="fixed top-4 right-0 z-[9999]">
            <n-tooltip placement="left" trigger="hover">
                <template #trigger>
                    <n-el
                        class="cursor-pointer rounded-l-lg bg-[var(--primary-color)] p-2 shadow-lg c-[var(--base-color)]"
                        @click="exitFullContent"
                    >
                        <n-icon>
                            <IonIcon name="ContractOutline" />
                        </n-icon>
                    </n-el>
                </template>
                {{ $t('app.togglContentFullScreen') }}
            </n-tooltip>
        </div>
    </Teleport>
</template>
