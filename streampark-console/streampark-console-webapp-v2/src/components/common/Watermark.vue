<script setup lang="ts">
interface Props {
  showWatermark: boolean
  text?: string
}
const {
  text = 'Watermark',
} = defineProps<Props>()
</script>

<template>
  <n-watermark
    v-if="showWatermark"
    :content="text"
    cross
    fullscreen
    :font-size="16"
    :line-height="16"
    :width="384"
    :height="384"
    :x-offset="12"
    :y-offset="60"
    :rotate="-15"
  />
</template>
