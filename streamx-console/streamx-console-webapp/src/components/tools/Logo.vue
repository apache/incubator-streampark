<template>
  <div
    class="logo">
    <router-link
      :to="{path:'/flink/app'}">
      <img
        :src="logoImg"

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
      logoImg: require('@assets/imgs/logo2.svg')
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
          this.logoImg = require('@assets/imgs/logo2.svg')
        },50)
      }
    }
  }
}
</script>
