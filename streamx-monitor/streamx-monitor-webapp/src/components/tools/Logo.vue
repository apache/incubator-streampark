<template>
  <div class="logo">
    <router-link :to="{name:'dashboard'}">
      <img src="~@/assets/streamx2.png" alt="logo">
    </router-link>
  </div>
</template>

<script>

export default {
  name: 'Logo',
  props: {
    title: {
      type: String,
      default: 'WebX',
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
