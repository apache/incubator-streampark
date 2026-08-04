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
import type { CSSProperties } from 'vue'
import { computed } from 'vue'

const props = withDefaults(
    defineProps<{
        prefix?: string
        name: string
        size?: number | string
        spin?: boolean
    }>(),
    {
        prefix: 'icon',
        size: 16,
        spin: false,
    },
)

defineOptions({ name: 'SvgIcon' })

const symbolId = computed(() => `#${props.prefix}-${props.name}`)

const iconStyle = computed((): CSSProperties => {
    const size = `${props.size}`.replace('px', '')
    return {
        width: `${size}px`,
        height: `${size}px`,
    }
})
</script>

<template>
    <svg
        class="streampark-svg-icon"
        :class="[$attrs.class, spin && 'streampark-svg-icon--spin']"
        :style="iconStyle"
        aria-hidden="true"
    >
        <use :xlink:href="symbolId" />
    </svg>
</template>

<style scoped>
.streampark-svg-icon {
    display: inline-block;
    overflow: hidden;
    vertical-align: -0.15em;
    fill: currentColor;
}

.streampark-svg-icon--spin {
    animation: streampark-svg-icon-spin 1s infinite linear;
}

@keyframes streampark-svg-icon-spin {
    from {
        transform: rotate(0deg);
    }

    to {
        transform: rotate(360deg);
    }
}
</style>
