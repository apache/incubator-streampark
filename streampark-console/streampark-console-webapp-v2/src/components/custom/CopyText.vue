<script setup lang="ts">
interface Props {
    maxLength?: string
}
const { maxLength } = defineProps<Props>()
const modelValue = defineModel<string>('value')
</script>

<template>
    <div v-if="modelValue" class="inline-flex items-center gap-0.5em">
        <n-ellipsis :style="{ 'max-width': maxLength || '12em' }">
            {{ modelValue }}
        </n-ellipsis>
        <n-tooltip trigger="hover">
            <template #trigger>
                <span v-copy="modelValue" class="cursor-pointer">
                    <n-icon>
                        <IonIcon name="CopyOutline" />
                    </n-icon>
                </span>
            </template>
            {{ $t('components.copyText.tooltip') }}
        </n-tooltip>
    </div>
</template>
