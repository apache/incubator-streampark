<script setup lang="ts">
import { useAppStore, useRouteStore } from '@/store'
import AppLayout from './AppLayout.vue'
import {
  BackTop,
  Breadcrumb,
  CollapaseButton,
  FullScreen,
  Logo,
  MobileDrawer,
  Notices,
  Search,
  Setting,
  SettingDrawer,
  TabBar,
  TeamSelect,
  UserCenter,
} from './components'
import ContentFullScreen from './components/tab/ContentFullScreen.vue'
import Content from './Content.vue'
import Lock from '@/views/build-in/lock/index.vue'
import LockScreen from './components/header/LockScreen.vue'
import { useLayoutMenu } from './composables/useLayoutMenu'

const route = useRoute()
const appStore = useAppStore()
const routeStore = useRouteStore()

const { layoutMode } = storeToRefs(useAppStore())

const {
  layout,
  activeKey,
// @ts-expect-error naive-ui menu tree depth exceeds TS recursion limit
} = useLayoutMenu({
  mode: layoutMode,
  accordion: true,
  menus: computed(() => routeStore.menus),
})

watch(() => route.path, () => {
  activeKey.value = routeStore.activeMenu
}, { immediate: true })

const showMobileDrawer = ref(false)

const sidebarWidth = ref(240)
const sidebarCollapsedWidth = ref(64)

const hasHorizontalMenu = computed(() => ['horizontal', 'mixed-two-column', 'mixed-sidebar'].includes(layoutMode.value))

const hidenCollapaseButton = computed(() => ['horizontal'].includes(layoutMode.value) || appStore.isMobile)
</script>

<template>
  <SettingDrawer />
  <ContentFullScreen />
  <AppLayout
    v-model:collapsed="appStore.collapsed"
    :mode="layoutMode"
    :is-mobile="appStore.isMobile"
    :show-logo="appStore.showLogo && !appStore.isMobile"
    :show-footer="appStore.showFooter"
    :show-tabbar="appStore.showTabs"
    :sidebar-width="sidebarWidth"
    :sidebar-collapsed-width="sidebarCollapsedWidth"
  >
    <template #logo>
      <Logo />
    </template>

    <template #nav-left>
      <template v-if="appStore.isMobile">
        <Logo />
      </template>

      <template v-else>
        <div v-if="!hasHorizontalMenu || !hidenCollapaseButton" class="h-full flex-y-center gap-1 p-x-sm">
          <CollapaseButton v-if="!hidenCollapaseButton" />
          <Breadcrumb v-if="!hasHorizontalMenu" />
        </div>
      </template>
    </template>

    <template #nav-center>
      <div class="h-full flex-y-center gap-1">
        <n-menu v-if="hasHorizontalMenu" v-bind="layout.horizontalMenuProps" />
      </div>
    </template>

    <template #nav-right>
      <div class="h-full flex-y-center gap-1 p-x-xl">
        <template v-if="appStore.isMobile">
          <n-button quaternary @click="showMobileDrawer = true">
            <template #icon>
              <n-icon size="18">
                <IonIcon name="MenuOutline" />
              </n-icon>
            </template>
          </n-button>
        </template>

        <template v-else>
          <Search />
          <Notices />
          <LockScreen />
          <FullScreen />
          <DarkModeSwitch />
          <LangsSwitch />
          <Setting />
          <TeamSelect />
          <UserCenter />
        </template>
      </div>
    </template>

    <template #sidebar>
      <n-menu v-bind="layout.verticalMenuProps" :collapsed-width="sidebarCollapsedWidth" />
    </template>

    <template #sidebar-extra>
      <n-scrollbar class="h-full">
        <n-menu v-bind="layout.verticalExtraMenuProps" :collapsed-width="sidebarCollapsedWidth" />
      </n-scrollbar>
    </template>

    <template #tabbar>
      <TabBar />
    </template>

    <template #footer>
      <div class="flex-center h-full">
        {{ appStore.footerText }}
      </div>
    </template>

    <Content />
    <BackTop />
    <Lock />

    <MobileDrawer v-model:show="showMobileDrawer">
      <n-menu v-bind="layout.verticalMenuProps" />
    </MobileDrawer>
  </AppLayout>
</template>
