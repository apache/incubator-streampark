<script setup lang="ts">
import type { LayoutMode } from '@/layouts/types'

const value = defineModel<LayoutMode>('value', { required: true })
</script>

<template>
    <div class="selector-wapper gap-4">
        <n-tooltip placement="top" trigger="hover">
            <template #trigger>
                <n-el
                    :class="{
                        'outline outline-2': value === 'vertical',
                    }"
                    class="grid grid-cols-[20%_1fr] outline-[var(--primary-color)] hover:(outline outline-2) cursor-pointer"
                    @click="value = 'vertical'"
                >
                    <div class="bg-[var(--primary-color)]" />
                    <div class="bg-[var(--divider-color)]" />
                </n-el>
            </template>
            <span> {{ $t('app.verticalLayout') }} </span>
        </n-tooltip>

        <n-tooltip placement="top" trigger="hover">
            <template #trigger>
                <n-el
                    :class="{
                        'outline outline-2': value === 'horizontal',
                    }"
                    class="grid grid-rows-[30%_1fr] outline-[var(--primary-color)] hover:(outline outline-2) cursor-pointer"
                    @click="value = 'horizontal'"
                >
                    <div class="bg-[var(--primary-color)]" />
                    <div class="bg-[var(--divider-color)]" />
                </n-el>
            </template>
            <span> {{ $t('app.horizontalLayout') }} </span>
        </n-tooltip>

        <n-tooltip placement="top" trigger="hover">
            <template #trigger>
                <n-el
                    :class="{
                        'outline outline-2': value === 'two-column',
                    }"
                    class="grid grid-cols-[10%_15%_1fr] outline-[var(--primary-color)] hover:(outline outline-2) cursor-pointer"
                    @click="value = 'two-column'"
                >
                    <div class="bg-[var(--primary-color)]" />
                    <div class="bg-[var(--primary-color-suppl)]" />
                    <div class="bg-[var(--divider-color)]" />
                </n-el>
            </template>
            <span> {{ $t('app.twoColumnLayout') }} </span>
        </n-tooltip>

        <n-tooltip placement="bottom" trigger="hover">
            <template #trigger>
                <n-el
                    :class="{
                        'outline outline-2': value === 'mixed-two-column',
                    }"
                    class="grid grid-cols-[10%_15%_1fr] grid-rows-[20%_1fr] outline-[var(--primary-color)] hover:(outline outline-2) cursor-pointer"
                    @click="value = 'mixed-two-column'"
                >
                    <div class="bg-[var(--primary-color-suppl)] row-span-2" />
                    <div class="bg-[var(--primary-color-suppl)] row-span-2" />
                    <div class="bg-[var(--primary-color)]" />
                    <div class="bg-[var(--divider-color)]" />
                </n-el>
            </template>
            <span> {{ $t('app.mixedTwoColumnLayout') }} </span>
        </n-tooltip>

        <n-tooltip placement="bottom" trigger="hover">
            <template #trigger>
                <n-el
                    :class="{
                        'outline outline-2': value === 'sidebar',
                    }"
                    class="grid grid-cols-[20%_1fr] grid-rows-[20%_1fr] outline-[var(--primary-color)] hover:(outline outline-2) cursor-pointer"
                    @click="value = 'sidebar'"
                >
                    <div class="bg-[var(--divider-color)] col-span-2" />
                    <div class="bg-[var(--primary-color)]" />
                    <div class="bg-[var(--divider-color)]" />
                </n-el>
            </template>
            <span> {{ $t('app.sidebarLayout') }} </span>
        </n-tooltip>

        <n-tooltip placement="bottom" trigger="hover">
            <template #trigger>
                <n-el
                    :class="{
                        'outline outline-2': value === 'mixed-sidebar',
                    }"
                    class="grid grid-cols-[20%_1fr] grid-rows-[20%_1fr] outline-[var(--primary-color)] hover:(outline outline-2) cursor-pointer"
                    @click="value = 'mixed-sidebar'"
                >
                    <div class="bg-[var(--primary-color)] col-span-2" />
                    <div class="bg-[var(--primary-color-suppl)]" />
                    <div class="bg-[var(--divider-color)]" />
                </n-el>
            </template>
            <span> {{ $t('app.mixedSidebarLayout') }} </span>
        </n-tooltip>
    </div>
</template>

<style lang="scss" scoped>
.selector-wapper {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
}

.grid {
    height: 60px;
    width: 86px;
    gap: 0.4em;
    padding: 0.4em;
    box-shadow: var(--box-shadow-1);
    border-radius: var(--border-radius);
}
.grid > div {
    border-radius: var(--border-radius);
}
</style>
