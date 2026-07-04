<script setup lang="ts">
import Search from '../header/Search.vue'
import Notices from '../header/Notices.vue'
import UserCenter from '../header/UserCenter.vue'
import Setting from './Setting.vue'

const showDrawer = defineModel<boolean>('show', { default: false })
</script>

<template>
  <n-drawer
    v-model:show="showDrawer"
    :width="280"
    placement="right"
    :mask-closable="true"
    :close-on-esc="true"
  >
    <n-drawer-content :native-scrollbar="false" :body-content-style="{ padding: '0' }">
      <template #header>
        <div class="flex items-center">
          <UserCenter />
          <div class="ml-auto" />
          <Search />
          <Notices />
        </div>
      </template>

      <slot />

      <template #footer>
        <DarkModeSwitch />
        <LangsSwitch />
        <div class="ml-auto" />
        <Setting />
      </template>
    </n-drawer-content>
  </n-drawer>
</template>
