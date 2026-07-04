<script setup lang="ts">
import type { RouteLocationNormalized } from 'vue-router'
import { translateMenuTitle } from '@/utils'

const { route, value, closable = false } = defineProps<{
  route: RouteLocationNormalized
  value: string
  closable?: boolean
}>()

const emit = defineEmits<{
  close: [string]
}>()
</script>

<template>
  <n-el
    class="cursor-pointer p-x-4 p-y-2 m-x-2px b b-[--divider-color] b-b-[#0000] rounded-[--border-radius]"
    :class="[
      value === route.fullPath ? 'c-[--primary-color]' : 'c-[--text-color-2]',
      value === route.fullPath ? 'bg-[#0000]' : 'bg-[--tab-color]',
      closable && 'p-r-2',
    ]"
    style="transition: box-shadow .3s var(--n-bezier), color .3s var(--n-bezier), background-color .3s var(--n-bezier), border-color .3s var(--n-bezier);"
  >
    <div class="flex-center gap-2 text-nowrap">
      <nova-icon :icon="route.meta.icon" />
      <span>{{ translateMenuTitle(String(route.meta.title || route.name)) }}</span>
      <button
        v-if="closable"
        type="button"
        class="bg-transparent h-18px w-18px flex-center text-[var(--close-icon-color)] hover:bg-[var(--close-color-hover)] rounded-3px"
        style="transition: background-color .3s var(--n-bezier), color .3s var(--n-bezier);"
        @click.stop="emit('close', route.fullPath)"
      >
        <n-icon size="14">
          <svg viewBox="0 0 12 12" version="1.1" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><g stroke="none" stroke-width="1" fill="none" fill-rule="evenodd"><g fill="currentColor" fill-rule="nonzero"><path d="M2.08859116,2.2156945 L2.14644661,2.14644661 C2.32001296,1.97288026 2.58943736,1.95359511 2.7843055,2.08859116 L2.85355339,2.14644661 L6,5.293 L9.14644661,2.14644661 C9.34170876,1.95118446 9.65829124,1.95118446 9.85355339,2.14644661 C10.0488155,2.34170876 10.0488155,2.65829124 9.85355339,2.85355339 L6.707,6 L9.85355339,9.14644661 C10.0271197,9.32001296 10.0464049,9.58943736 9.91140884,9.7843055 L9.85355339,9.85355339 C9.67998704,10.0271197 9.41056264,10.0464049 9.2156945,9.91140884 L9.14644661,9.85355339 L6,6.707 L2.85355339,9.85355339 C2.65829124,10.0488155 2.34170876,10.0488155 2.14644661,9.85355339 C1.95118446,9.65829124 1.95118446,9.34170876 2.14644661,9.14644661 L5.293,6 L2.14644661,2.85355339 C1.97288026,2.67998704 1.95359511,2.41056264 2.08859116,2.2156945 L2.14644661,2.14644661 L2.08859116,2.2156945 Z" /></g></g></svg>
        </n-icon>
      </button>
    </div>
  </n-el>
</template>
