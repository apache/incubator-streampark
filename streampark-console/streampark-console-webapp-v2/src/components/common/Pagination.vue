<script setup lang="ts">
interface Props {
  count?: number
}
const {
  count = 0,
} = defineProps<Props>()

const emit = defineEmits<{
  change: [page: number, pageSize: number] // 具名元组语法
}>()

const page = ref(1)
const pageSize = ref(10)
const displayOrder: Array<'pages' | 'size-picker' | 'quick-jumper'> = ['size-picker', 'pages']

function changePage() {
  emit('change', page.value, pageSize.value)
}
</script>

<template>
  <n-pagination
    v-if="count > 0"
    v-model:page="page"
    v-model:page-size="pageSize"
    :page-sizes="[10, 20, 30, 50]"
    :item-count="count"
    :display-order="displayOrder"
    show-size-picker
    @update-page="changePage"
    @update-page-size="changePage"
  />
</template>

<style scoped></style>
