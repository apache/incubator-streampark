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
import { useAuthStore } from '@/store'
import { useUserStoreWithOut } from '@/store/modules/user'
import { ionIcon } from '@/utils/ionIcon'
import { SP_ICONS as I } from '@/constants/streamparkIcons'
import PasswordModal from './PasswordModal.vue'

const { t } = useI18n()

const authStore = useAuthStore()
const userStore = useUserStoreWithOut()
const passwordModalVisible = ref(false)

const userInfo = computed(() => userStore.getUserInfo)

const options = computed(() => {
    return [
        {
            label: userInfo.value?.nickName || userInfo.value?.username || t('app.userCenter'),
            key: 'userInfo',
            disabled: true,
        },
        {
            type: 'divider',
            key: 'd1',
        },
        {
            label: t('sys.modifyPassword.title'),
            key: 'password',
            icon: () => ionIcon(I.password),
        },
        {
            label: 'GitHub',
            key: 'github',
            icon: () => ionIcon('ant-design:github-outlined'),
        },
        {
            label: 'Docs',
            key: 'docs',
            icon: () => ionIcon(I.docs),
        },
        {
            type: 'divider',
            key: 'd2',
        },
        {
            label: t('app.loginOut'),
            key: 'loginOut',
            icon: () => ionIcon(I.logout),
        },
    ]
})

function handleSelect(key: string | number) {
    if (key === 'loginOut') {
        window.$dialog?.info({
            title: t('app.loginOutTitle'),
            content: t('app.loginOutContent'),
            positiveText: t('common.confirm'),
            negativeText: t('common.cancel'),
            onPositiveClick: () => {
                authStore.logout()
            },
        })
    }

    if (key === 'password') passwordModalVisible.value = true

    if (key === 'github') window.open('https://github.com/apache/streampark')

    if (key === 'docs') window.open('https://streampark.apache.org/docs/intro')
}
</script>

<template>
    <n-dropdown trigger="click" :options="options" @select="handleSelect">
        <n-avatar round class="cursor-pointer">
            <template #fallback>
                <div class="wh-full flex-center">
                    <n-icon>
                        <IonIcon name="PersonOutline" />
                    </n-icon>
                </div>
            </template>
        </n-avatar>
    </n-dropdown>
    <PasswordModal v-model:show="passwordModalVisible" />
</template>

<style scoped></style>
