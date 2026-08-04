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
const props = defineProps<{
    tags?: string | null
}>()

const tagList = computed(() => {
    if (!props.tags?.trim()) return []
    return props.tags
        .split(',')
        .map((tag) => tag.trim())
        .filter(Boolean)
})
</script>

<template>
    <n-tooltip v-if="tagList.length" trigger="hover">
        <template #trigger>
            <div class="app-tags-cell flex flex-wrap items-center gap-4px">
                <n-tag
                    v-for="(tag, index) in tagList"
                    :key="`${tag}-${index}`"
                    size="small"
                    type="info"
                    :bordered="false"
                >
                    {{ tag }}
                </n-tag>
            </div>
        </template>
        {{ tags }}
    </n-tooltip>
    <span v-else>-</span>
</template>

<style scoped>
.app-tags-cell :deep(.n-tag) {
    max-width: 100%;
}

.app-tags-cell :deep(.n-tag__content) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
</style>
