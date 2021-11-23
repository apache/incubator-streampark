<template>
  <div
    class="logo">
    <router-link
      style="display: flex;justify-content: center;"
      :to="{path:'/flink/app'}">
      <img
        :src="logoImg"
        style="transform: scale(0.8)"
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
  },
  data () {
    return {
      logoImg: require('@/assets/imgs/logo2.png')
    }
  },
  methods: {
    collapsed(flag) {
      if(flag) {
        setTimeout(()=>{
          this.logoImg = require('@assets/imgs/logo.svg')
        },50)
      } else {
        setTimeout(()=>{
          this.logoImg = require('@/assets/imgs/logo2.png')
        },50)
      }
    }
  }
}
</script>
