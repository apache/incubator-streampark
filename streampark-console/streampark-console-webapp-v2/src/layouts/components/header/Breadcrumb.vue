<script setup lang="ts">
import { useAppStore } from '@/store'
import { translateMenuTitle } from '@/utils'

const router = useRouter()
const route = useRoute()
const routes = computed(() => {
    return route.matched
})
const appStore = useAppStore()
</script>

<template>
    <TransitionGroup
        v-if="appStore.showBreadcrumb"
        name="list"
        tag="ul"
        style="display: flex; gap: 1em"
    >
        <n-el
            v-for="item in routes"
            :key="item.path"
            tag="li"
            style="color: var(--text-color-2); transition: 0.3s var(--cubic-bezier-ease-in-out)"
            class="flex-center gap-2 cursor-pointer split"
            @click="router.push(item.path)"
        >
            <nova-icon v-if="appStore.showBreadcrumbIcon" :icon="item.meta.icon" />
            <span class="whitespace-nowrap">{{
                translateMenuTitle(String(item.meta.title || item.name))
            }}</span>
        </n-el>
    </TransitionGroup>
</template>

<style lang="scss">
.split:not(:first-child)::before {
    content: '/';
    padding-right: 0.6em;
}

.list-move,
.list-enter-active,
.list-leave-active {
    transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
    opacity: 0;
    transform: translateX(-30px);
}

.list-leave-active {
    position: absolute;
}
</style>
