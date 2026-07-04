<script setup lang="ts">
import { useTabStore } from '@/store'
import { renderIcon, translateMenuTitle } from '@/utils'

const tabStore = useTabStore()

function renderDropTabsLabel(option: any) {
  return translateMenuTitle(String(option.meta?.title || option.name))
}

function renderDropTabsIcon(option: any) {
  return renderIcon(option.meta.icon)!()
}

const router = useRouter()
function handleDropTabs(key: string, option: any) {
  router.push(option.path)
}
</script>

<template>
  <n-dropdown
    :options="tabStore.allTabs"
    :render-label="renderDropTabsLabel"
    :render-icon="renderDropTabsIcon"
    trigger="click"
    size="small"
    key-field="fullPath"
    @select="handleDropTabs"
  >
    <CommonWrapper>
      <n-icon>
        <IonIcon name="AppsOutline" />
      </n-icon>
    </CommonWrapper>
  </n-dropdown>
</template>

<style scoped>

</style>
