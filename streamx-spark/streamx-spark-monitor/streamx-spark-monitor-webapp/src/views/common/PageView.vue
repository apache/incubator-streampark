<template>
    <page-layout :title="title">
        <keep-alive v-if="multipage">
          <router-view ref="page"/>
        </keep-alive>
        <router-view ref="page" v-else/>
    </page-layout>
</template>

<script>
import PageLayout from './PageLayout'
export default {
  name: 'PageView',
  components: {PageLayout},
  data () {
    return {
      title: ''
    }
  },
  computed: {
    multipage () {
      return this.$store.state.setting.multipage
    }
  },
  mounted () {
    this.getPageHeaderInfo()
  },
  updated () {
    this.getPageHeaderInfo()
  },
  methods: {
    getPageHeaderInfo () {
      this.title = this.$route.name
    }
  }
}
</script>

<style lang="less" scoped>
  .extraImg{
    margin-top: -60px;
    text-align: center;
    width: 195px;
    img{
      width: 100%;
    }
  }
</style>
