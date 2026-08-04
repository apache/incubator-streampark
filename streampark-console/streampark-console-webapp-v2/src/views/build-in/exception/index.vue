<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PageEnum } from '@/enums/pageEnum'

defineOptions({ name: 'ExceptionPage' })

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const status = computed(() => {
    const code = Number(route.query.status)
    return Number.isFinite(code) ? code : 500
})

const statusMap: Record<number, { title: string; subtitle: string }> = {
    403: { title: '403', subtitle: t('sys.exception.subTitle403') },
    404: { title: '404', subtitle: t('sys.exception.subTitle404') },
    500: { title: '500', subtitle: t('sys.exception.subTitle500') },
}

const current = computed(() => statusMap[status.value] ?? statusMap[500])

function goHome() {
    router.push(PageEnum.BASE_HOME)
}
</script>

<template>
    <div class="flex-col-center h-full">
        <n-result
            :status="status === 403 ? '403' : status === 404 ? '404' : '500'"
            :title="current.title"
            :description="current.subtitle"
        >
            <template #footer>
                <n-button type="primary" @click="goHome">
                    {{ t('sys.exception.backHome') }}
                </n-button>
            </template>
        </n-result>
    </div>
</template>
