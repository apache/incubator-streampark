<script setup lang="ts">
interface Props {
    message: string
}

const { message } = defineProps<Props>()
</script>

<template>
    <n-tooltip :show-arrow="false" trigger="hover">
        <template #trigger>
            <n-icon class="op-50 cursor-help">
                <IonIcon name="HelpCircleOutline" />
            </n-icon>
        </template>
        {{ message }}
    </n-tooltip>
</template>
