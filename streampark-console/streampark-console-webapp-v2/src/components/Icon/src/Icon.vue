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
import { Icon as IconifyIcon } from '@iconify/vue'
import { NIcon } from 'naive-ui'
import { computed, useAttrs } from 'vue'
import { resolveIconifyRef, resolveViconComponent } from '../xicons'
import SvgIcon from './SvgIcon.vue'

const SVG_END_WITH_FLAG = '|svg'

const props = withDefaults(
    defineProps<{
        icon?: string
        color?: string
        depth?: string | number
        size?: string | number
        spin?: boolean
        prefix?: string
    }>(),
    {
        size: 18,
        spin: false,
        prefix: '',
    },
)

defineOptions({ name: 'Icon', inheritAttrs: false })

const attrs = useAttrs()

const isSvgIcon = computed(() => props.icon?.endsWith(SVG_END_WITH_FLAG))
const svgName = computed(() => props.icon?.replace(SVG_END_WITH_FLAG, '') ?? '')

const iconRef = computed(() => {
    const raw = props.prefix ? `${props.prefix}:${props.icon}` : props.icon
    return raw
})

const iconifyName = computed(() => {
    const raw = iconRef.value
    if (!raw || raw.endsWith(SVG_END_WITH_FLAG)) return ''
    if (raw.includes(':')) return raw
    return resolveIconifyRef(raw)
})

const viconComponent = computed(() => {
    if (!iconRef.value || isSvgIcon.value || iconifyName.value) return undefined
    return resolveViconComponent(iconRef.value)
})

const iconSize = computed(() => {
    const style = attrs.style as Record<string, string> | undefined
    const fontSize = style?.fontSize
    if (fontSize) {
        const parsed = Number.parseInt(String(fontSize), 10)
        if (!Number.isNaN(parsed)) return parsed
    }
    if (typeof props.size === 'string') return Number.parseInt(props.size, 10) || 18
    return props.size ?? 18
})

const iconDepth = computed(() => {
    const d = props.depth
    if (d === 1 || d === 2 || d === 3 || d === 5) return d
    return undefined
})

const forwardAttrs = computed(() => {
    const { class: _class, style: _style, ...rest } = attrs
    return rest
})
</script>

<template>
    <SvgIcon
        v-if="isSvgIcon"
        :size="size"
        :name="svgName"
        :spin="spin"
        :class="[$attrs.class, 'streampark-icon']"
    />
    <NIcon
        v-else-if="iconifyName"
        :size="iconSize"
        :color="color"
        :depth="iconDepth"
        :style="$attrs.style"
        :class="[$attrs.class, 'streampark-icon', spin && 'streampark-icon--spin']"
        v-bind="forwardAttrs"
    >
        <IconifyIcon :icon="iconifyName" />
    </NIcon>
    <NIcon
        v-else-if="viconComponent"
        :size="iconSize"
        :color="color"
        :depth="iconDepth"
        :style="$attrs.style"
        :class="[$attrs.class, 'streampark-icon', spin && 'streampark-icon--spin']"
        v-bind="forwardAttrs"
    >
        <component :is="viconComponent" />
    </NIcon>
</template>

<style scoped>
.streampark-icon {
    display: inline-flex;
    vertical-align: -0.125em;
}

.streampark-icon--spin {
    animation: streampark-icon-spin 1s infinite linear;
}

@keyframes streampark-icon-spin {
    from {
        transform: rotate(0deg);
    }

    to {
        transform: rotate(360deg);
    }
}
</style>
