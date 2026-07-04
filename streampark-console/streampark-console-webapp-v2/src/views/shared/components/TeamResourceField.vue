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
import { fetchTeamResource } from '@/service'
import { ResourceTypeEnum } from '@/views/resource/upload/shared/constants'

const model = defineModel<string[]>({ default: () => [] })

const { t } = useI18n()

const teamResources = ref<Recordable[]>([])

const options = computed(() =>
  teamResources.value
    .filter(item => item.resourceType !== ResourceTypeEnum.APP)
    .map(resource => ({
      value: resource.id,
      label: `${resource.resourceType}-${resource.resourceName}`,
    })),
)

onMounted(async () => {
  const result = await fetchTeamResource({})
  if (result.isSuccess)
    teamResources.value = result.data ?? []
})
</script>

<template>
  <n-select
    v-model:value="model"
    multiple
    filterable
    clearable
    :max-tag-count="3"
    :options="options"
    :placeholder="t('flink.app.resourcePlaceHolder')"
  />
</template>
