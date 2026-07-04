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
import HadoopConfDrawer from './HadoopConfDrawer.vue'

const model = defineModel<boolean>({ required: true })

const { t } = useI18n()

const drawerVisible = ref(false)
</script>

<template>
  <div class="flex items-center gap-8px">
    <n-switch v-model:value="model">
      <template #checked>{{ t('common.switchOn') }}</template>
      <template #unchecked>{{ t('common.switchOff') }}</template>
    </n-switch>
    <n-popover trigger="hover">
      <template #trigger>
        <n-button quaternary circle size="tiny">
          ?
        </n-button>
      </template>
      <div class="max-w-280px text-13px">
        <p>{{ t('flink.app.useSysHadoopConfTip1') }}</p>
        <p>{{ t('flink.app.useSysHadoopConfTip2') }}</p>
      </div>
    </n-popover>
    <n-button v-if="model" size="small" @click="drawerVisible = true">
      {{ t('common.view') }}
    </n-button>
    <HadoopConfDrawer v-model:show="drawerVisible" />
  </div>
</template>
