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
import LockModal from './LockModal.vue'

const { t } = useI18n()

const lockModalVisible = ref(false)
</script>

<template>
  <n-tooltip placement="bottom" trigger="hover">
    <template #trigger>
      <CommonWrapper @click="lockModalVisible = true">
        <n-icon>
          <IonIcon name="LockClosedOutline" />
        </n-icon>
      </CommonWrapper>
    </template>
    <span>{{ t('layout.header.lockScreen') }}</span>
  </n-tooltip>
  <LockModal v-model:show="lockModalVisible" />
</template>
