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
<template>
  <div>
    <a :href="redirect" @click.prevent="handleClick" :class="{ 'badge-disabled': disabled }">
      <span v-if="hasLabel" class="badge-link">{{ label }}</span>
      <span class="badge-link" :style="{ backgroundColor: color }">
        {{ message }}
      </span>
    </a>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  interface BadgeProps {
    label: String;
    message: String;
    color?: string;
    redirect?: string;
    disabled?: boolean;
    onBadgeClick?: () => void;
  }
  const props = defineProps<BadgeProps>();
  const hasLabel = computed(() => !!props.label);
  const isClickable = computed(() => !!props.redirect && !props.disabled);
  function handleClick() {
    props.onBadgeClick?.() ?? (isClickable.value && window.open(props.redirect, '_blank'));
  }
</script>
<style lang="less">
  .badge-link {
    display: inline-flexbox;
    align-items: center;
    justify-content: center;
    color: #fff;
    background-color: #484f58;
    padding: 4px;
    min-width: 40px;
  }
  .badge-disabled {
    opacity: 0.5;
    &:hover {
      cursor: not-allowed;
    }
  }
</style>
