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
import type { LayoutMode } from './types'

const props = withDefaults(defineProps<{
  mode?: LayoutMode
  isMobile?: boolean
  showLogo?: boolean
  showFooter?: boolean
  showTabbar?: boolean
  showNav?: boolean
  showSidebar?: boolean
  navHeight?: number
  tabbarHeight?: number
  footerHeight?: number
  sidebarWidth?: number
  sidebarCollapsedWidth?: number
}>(), {
  mode: 'vertical',
  isMobile: false,
  showLogo: true,
  showFooter: true,
  showTabbar: true,
  showNav: true,
  showSidebar: true,
  navHeight: 60,
  tabbarHeight: 45,
  footerHeight: 40,
  sidebarWidth: 240,
  sidebarCollapsedWidth: 64,
})

const collapsed = defineModel<boolean>('collapsed', { default: false })

const layoutStyle = computed(() => ({
  '--sp-nav-height': `${props.navHeight}px`,
  '--sp-tabbar-height': `${props.tabbarHeight}px`,
  '--sp-footer-height': `${props.footerHeight}px`,
  '--sp-sidebar-width': `${props.sidebarWidth}px`,
  '--sp-sidebar-collapsed-width': `${props.sidebarCollapsedWidth}px`,
}))

const showAside = computed(() => {
  if (props.isMobile || props.mode === 'full-content' || props.mode === 'horizontal')
    return false
  return props.showSidebar
})

const showSidebarExtra = computed(() => {
  return showAside.value && ['two-column', 'mixed-two-column'].includes(props.mode)
})

const showHeader = computed(() => props.mode !== 'full-content' && props.showNav)
const showTabs = computed(() => props.mode !== 'full-content' && props.showTabbar)
const showFooterBlock = computed(() => props.mode !== 'full-content' && props.showFooter)
</script>

<template>
  <div
    class="sp-layout wh-full"
    :class="[
      `sp-layout--${mode}`,
      { 'sp-layout--mobile': isMobile, 'sp-layout--collapsed': collapsed },
    ]"
    :style="layoutStyle"
  >
    <aside
      v-if="showAside"
      class="sp-layout__aside"
      :class="{ 'sp-layout__aside--collapsed': collapsed }"
    >
      <div v-if="showLogo && mode !== 'mixed-two-column'" class="sp-layout__logo">
        <slot name="logo" />
      </div>
      <div class="sp-layout__aside-body" :class="{ 'sp-layout__aside-body--split': showSidebarExtra }">
        <div class="sp-layout__sidebar">
          <slot name="sidebar" />
        </div>
        <div v-if="showSidebarExtra" class="sp-layout__sidebar-extra">
          <slot name="sidebar-extra" />
        </div>
      </div>
    </aside>

    <div class="sp-layout__main">
      <header v-if="showHeader" class="sp-layout__header">
        <nav class="sp-layout__nav">
          <div class="sp-layout__nav-left">
            <slot name="nav-left" />
          </div>
          <div class="sp-layout__nav-center">
            <slot name="nav-center" />
          </div>
          <div class="sp-layout__nav-right">
            <slot name="nav-right" />
          </div>
        </nav>
        <div v-if="showTabs" class="sp-layout__tabbar">
          <slot name="tabbar" />
        </div>
      </header>

      <main class="sp-layout__content">
        <slot />
      </main>

      <footer v-if="showFooterBlock" class="sp-layout__footer">
        <slot name="footer" />
      </footer>
    </div>
  </div>
</template>

<style scoped>
.sp-layout {
  display: flex;
  width: 100%;
  height: 100%;
  color: var(--n-text-color);
  background-color: var(--n-color);
}

.sp-layout__aside {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: var(--sp-sidebar-width);
  height: 100%;
  border-right: 1px solid var(--n-border-color);
  background: var(--n-color);
  transition: width 0.3s var(--n-bezier);
  overflow: hidden;
}

.sp-layout__aside--collapsed {
  width: var(--sp-sidebar-collapsed-width);
}

.sp-layout__logo {
  flex-shrink: 0;
  height: var(--sp-nav-height);
}

.sp-layout__aside-body {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.sp-layout__aside-body--split {
  flex-direction: row;
}

.sp-layout__sidebar,
.sp-layout__sidebar-extra {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.sp-layout__sidebar-extra {
  border-left: 1px solid var(--n-border-color);
}

.sp-layout__main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.sp-layout__header {
  flex-shrink: 0;
  background: var(--n-color);
}

.sp-layout__nav {
  display: flex;
  align-items: center;
  height: var(--sp-nav-height);
  border-bottom: 1px solid var(--n-border-color);
}

.sp-layout__nav-left,
.sp-layout__nav-right {
  display: flex;
  align-items: center;
  height: 100%;
  flex-shrink: 0;
}

.sp-layout__nav-center {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

.sp-layout__tabbar {
  height: var(--sp-tabbar-height);
  border-bottom: 1px solid var(--n-border-color);
}

.sp-layout__content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: var(--n-body-color, var(--n-color));
}

.sp-layout__footer {
  flex-shrink: 0;
  height: var(--sp-footer-height);
  border-top: 1px solid var(--n-border-color);
  background: var(--n-color);
}

.sp-layout--full-content .sp-layout__content {
  padding: 0;
}
</style>
