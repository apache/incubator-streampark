<script setup lang="ts">
const router = useRouter()
</script>

<template>
  <div class="flex-col-center h-full">
    <img
      src="@/assets/svg/error-not-found.svg"
      alt=""
      class="w-1/3"
    >
    <n-button
      type="primary"
      @click="router.push('/')"
    >
      {{ $t('sys.exception.backHome') }}
    </n-button>
  </div>
</template>
