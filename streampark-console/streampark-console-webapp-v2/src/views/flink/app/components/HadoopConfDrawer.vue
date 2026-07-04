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
import { fetchSysHadoopConf } from '@/service/api/flink/config'

const show = defineModel<boolean>('show', { default: false })

const { t } = useI18n()

const loading = ref(false)
const hadoopConfContent = reactive<{ hadoop?: Recordable, hive?: Recordable }>({})

const isHadoopEmpty = computed(() =>
  !hadoopConfContent.hadoop || Object.keys(hadoopConfContent.hadoop).length === 0,
)
const isHiveEmpty = computed(() =>
  !hadoopConfContent.hive || Object.keys(hadoopConfContent.hive).length === 0,
)

async function loadConf() {
  if (Object.keys(hadoopConfContent).length > 0)
    return
  loading.value = true
  try {
    const result = await fetchSysHadoopConf()
    if (result.isSuccess && result.data)
      Object.assign(hadoopConfContent, result.data)
  }
  finally {
    loading.value = false
  }
}

watch(show, (visible) => {
  if (visible)
    loadConf()
})
</script>

<template>
  <n-drawer v-model:show="show" :width="800" placement="right">
    <n-drawer-content :title="t('flink.app.hadoopConfigTitle')" closable>
      <n-spin :show="loading">
        <n-tabs type="line">
          <n-tab-pane name="hadoop" :tab="t('flink.app.hadoopTab')">
            <n-empty v-if="isHadoopEmpty" />
            <n-tabs v-else type="line" placement="left">
              <n-tab-pane
                v-for="(content, item) in hadoopConfContent.hadoop"
                :key="String(item)"
                :name="String(item)"
                :tab="String(item)"
              >
                <pre class="conf-pre">{{ content }}</pre>
              </n-tab-pane>
            </n-tabs>
          </n-tab-pane>
          <n-tab-pane name="hive" :tab="t('flink.app.hiveTab')">
            <n-empty v-if="isHiveEmpty" />
            <n-tabs v-else type="line" placement="left">
              <n-tab-pane
                v-for="(content, item) in hadoopConfContent.hive"
                :key="String(item)"
                :name="String(item)"
                :tab="String(item)"
              >
                <pre class="conf-pre">{{ content }}</pre>
              </n-tab-pane>
            </n-tabs>
          </n-tab-pane>
        </n-tabs>
      </n-spin>
    </n-drawer-content>
  </n-drawer>
</template>

<style scoped>
.conf-pre {
  margin: 12px 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
