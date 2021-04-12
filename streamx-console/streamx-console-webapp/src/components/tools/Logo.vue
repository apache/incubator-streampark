<template>
  <div
    class="logo">
    <router-link
      :to="{path:'/flink/app'}">
      <img
        src="~@/assets/streamx.png"
        alt="logo">
    </router-link>
  </div>
</template>

<script>

export default {
  name: 'Logo',
  props: {
    title: {
      type: String,
      default: 'StreamX',
      required: false
    },
    showTitle: {
      type: Boolean,
      default: true,
      required: false
    }
  }
}
</script>
