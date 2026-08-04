<script setup lang="ts">
import { useAppStore, useRouteStore } from '@/store'

const appStore = useAppStore()
const routeStore = useRouteStore()

const transitionName = computed(() => appStore.transitionAnimation || undefined)
</script>

<template>
    <n-el
        class="h-full"
        :class="[appStore.layoutMode === 'full-content' ? 'p-0' : 'p-16px']"
        style="background-color: var(--action-color)"
    >
        <router-view v-if="appStore.loadFlag" v-slot="{ Component, route }">
            <template v-if="Component">
                <transition v-if="transitionName" :name="transitionName">
                    <keep-alive :include="routeStore.cacheRoutes">
                        <component
                            :is="Component"
                            :key="
                                Object.keys(route.query).length
                                    ? route.fullPath
                                    : (route.meta.componentName as string) || route.path
                            "
                        />
                    </keep-alive>
                </transition>
                <keep-alive v-else :include="routeStore.cacheRoutes">
                    <component
                        :is="Component"
                        :key="
                            Object.keys(route.query).length
                                ? route.fullPath
                                : (route.meta.componentName as string) || route.path
                        "
                    />
                </keep-alive>
            </template>
        </router-view>
    </n-el>
</template>
