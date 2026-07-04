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
import { JobTypeEnum as FlinkJobTypeEnum, ReleaseStateEnum } from '@/enums/flinkEnum'
import { JobTypeEnum as SparkJobTypeEnum } from '@/enums/sparkEnum'

const props = withDefaults(defineProps<{
  name: string
  jobType: number
  description?: string
  release?: number
  engine: 'flink' | 'spark'
  clickable?: boolean
  nameLabel?: string
  jobTypeLabel?: string
}>(), {
  clickable: true,
})

const emit = defineEmits<{ click: [] }>()

const { t } = useI18n()

const typeMeta = computed(() => {
  if (props.engine === 'flink') {
    if (props.jobType === FlinkJobTypeEnum.JAR)
      return { label: 'JAR', className: 'app-type-jar' }
    if (props.jobType === FlinkJobTypeEnum.SQL)
      return { label: 'SQL', className: 'app-type-sql' }
    return { label: 'PY', className: 'app-type-py' }
  }
  if (props.jobType === SparkJobTypeEnum.JAR)
    return { label: 'JAR', className: 'app-type-jar' }
  if (props.jobType === SparkJobTypeEnum.SQL)
    return { label: 'SQL', className: 'app-type-sql' }
  return { label: 'PySpark', className: 'app-type-py' }
})

const jarJobType = computed(() =>
  props.engine === 'flink'
    ? props.jobType === FlinkJobTypeEnum.JAR
    : props.jobType === SparkJobTypeEnum.JAR,
)

const showRecheckBadge = computed(() =>
  jarJobType.value && props.release === ReleaseStateEnum.NEED_CHECK,
)

const showChangedBadge = computed(() =>
  jarJobType.value
  && props.release != null
  && props.release >= ReleaseStateEnum.RELEASING
  && props.release !== ReleaseStateEnum.NEED_CHECK,
)

function handleClick() {
  if (props.clickable)
    emit('click')
}
</script>

<template>
  <div class="app-list-name-cell inline-flex max-w-full items-center gap-4px">
    <span class="app-type shrink-0" :class="typeMeta.className">
      {{ typeMeta.label }}
    </span>
    <n-popover trigger="hover" :title="t('common.detailText')">
      <template #trigger>
        <span
          class="min-w-0 truncate"
          :class="clickable ? 'cursor-pointer text-primary' : ''"
          @click="handleClick"
        >
          {{ name }}
        </span>
      </template>
      <div class="max-w-320px">
        <div class="flex gap-6px">
          <span class="font-bold">{{ nameLabel || t('flink.app.appName') }}:</span>
          <span class="break-words">{{ name }}</span>
        </div>
        <div class="mt-4px flex items-center gap-6px">
          <span class="font-bold">{{ jobTypeLabel || t('flink.app.jobType') }}:</span>
          <span class="app-type" :class="typeMeta.className">{{ typeMeta.label }}</span>
        </div>
        <div v-if="description" class="mt-4px flex gap-6px">
          <span class="shrink-0 font-bold">{{ t('common.description') }}:</span>
          <span class="break-words">{{ description }}</span>
        </div>
      </div>
    </n-popover>
    <n-tooltip v-if="showRecheckBadge" trigger="hover">
      <template #trigger>
        <n-badge value="NEW" type="warning" />
      </template>
      {{ engine === 'flink' ? t('flink.app.view.recheck') : t('spark.app.view.recheck') }}
    </n-tooltip>
    <n-tooltip v-else-if="showChangedBadge" trigger="hover">
      <template #trigger>
        <n-badge dot processing />
      </template>
      {{ engine === 'flink' ? t('flink.app.view.changed') : t('spark.app.view.changed') }}
    </n-tooltip>
  </div>
</template>

<style scoped>
.app-type {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 5px;
  border-radius: 2px;
  transform: scale(0.88);
  transform-origin: center;
}

.app-type-jar {
  border: 1px solid #06f;
  background: #1890ff;
  color: #f5f5f5;
}

.app-type-sql {
  border: 1px solid #0070cc;
  background: rgb(0 112 204 / 20%);
  color: rgb(0 0 0 / 85%);
}

.app-type-py {
  border: 1px solid #fa8c16;
  background: rgb(250 140 22 / 15%);
  color: rgb(0 0 0 / 85%);
}

html.dark .app-type-sql,
html.dark .app-type-py {
  color: rgb(255 255 255 / 85%);
}

.app-list-name-cell :deep(.n-badge) {
  margin-left: 2px;
}
</style>
