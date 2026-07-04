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
import type { UploadCustomRequestOptions, UploadFileInfo } from 'naive-ui'
import Icon from '@/components/Icon/src/Icon.vue'

const props = defineProps<{
  customRequest: (item: { file: File, filename: string }) => Promise<void>
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:loading': [value: boolean]
}>()

const { t } = useI18n()

async function handleCustomRequest(options: UploadCustomRequestOptions) {
  const file = options.file.file as File
  try {
    await props.customRequest({ file, filename: options.file.name })
    options.onFinish()
  }
  catch {
    options.onError()
  }
}

function handleUploadJar(options: { file: UploadFileInfo }) {
  if (options.file.status === 'finished' || options.file.status === 'error')
    emit('update:loading', false)
}

function handleBeforeUpload(data: { file: UploadFileInfo }) {
  const file = data.file.file as File
  if (file.type !== 'application/java-archive') {
    if (!/\.(jar|JAR|py)$/.test(file.name)) {
      emit('update:loading', false)
      window.$message?.error(t('flink.app.action.onlyJarFiles'))
      return false
    }
  }
  emit('update:loading', true)
  return true
}
</script>

<template>
  <div>
    <n-upload
      multiple
      :show-file-list="loading"
      :custom-request="handleCustomRequest"
      @before-upload="handleBeforeUpload"
      @change="handleUploadJar"
    >
      <n-upload-dragger>
        <div class="h-200px flex flex-col items-center justify-center">
          <Icon icon="ion:archive-outline" :style="{ fontSize: '56px' }" />
          <p class="mt-12px text-16px">
            {{ t('flink.app.dragUploadTitle') }}
          </p>
          <p class="text-14px text-gray-500">
            {{ t('flink.app.dragUploadTip') }}
          </p>
        </div>
      </n-upload-dragger>
    </n-upload>
    <slot name="uploadInfo" />
  </div>
</template>
