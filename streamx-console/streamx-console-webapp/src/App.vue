<template>
  <a-locale-provider
    :locale="locale">
    <div
      id="app">
      <router-view />
    </div>
  </a-locale-provider>
</template>

<script>
import zhCN from 'ant-design-vue/lib/locale-provider/zh_CN'
import { AppDeviceEnquire } from '@/utils/mixin'

export default {
  mixins: [AppDeviceEnquire],
  data () {
    return {
      locale: zhCN
    }
  },
  mounted () {
    console.log(111111111111111111111111111111111111)
  }
}
</script>

<style lang="less">
@import "App";
</style>