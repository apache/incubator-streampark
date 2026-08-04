<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed } from 'vue'
import SvgIcon from '@/components/Icon/src/SvgIcon.vue'
import { resolveIconifyRef, resolveViconComponent } from '@/components/Icon/xicons'

const props = defineProps<{
    name: string
}>()

defineOptions({ name: 'IonIcon' })

const isSvgIcon = computed(() => props.name.endsWith('|svg'))
const svgName = computed(() => props.name.replace('|svg', ''))
const iconifyName = computed(() => {
    if (isSvgIcon.value) return ''
    if (props.name.includes(':')) return props.name
    return resolveIconifyRef(props.name)
})
const iconComponent = computed(() => {
    if (isSvgIcon.value || iconifyName.value) return undefined
    return resolveViconComponent(props.name)
})
</script>

<template>
    <SvgIcon v-if="isSvgIcon" :name="svgName" :size="18" />
    <component :is="iconComponent" v-else-if="iconComponent" />
    <Icon v-else-if="iconifyName" :icon="iconifyName" />
</template>
