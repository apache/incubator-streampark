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
defineProps<{
  layer: Recordable
}>()
</script>

<template>
  <div class="mb-5px">
    <n-space size="small">
      <n-icon><IonIcon name="ArrowForwardOutline" /></n-icon>
      <n-tag type="info">
        {{ layer.layerId }}
      </n-tag>
      <n-tag>{{ layer.status }}</n-tag>
      <template v-if="layer.totalMb != null && layer.totalMb !== 0">
        <span class="text-12px">
          {{ layer.currentMb }} / {{ layer.totalMb }} MB
        </span>
      </template>
    </n-space>
  </div>
  <template v-if="layer.totalMb != null && layer.totalMb !== 0">
    <div class="mb-15px ml-20px mr-50px">
      <n-progress :percentage="layer.percent" processing />
    </div>
  </template>
</template>
