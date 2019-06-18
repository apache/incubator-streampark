<template>
    <div :class="multipage === true ? 'multi-page':'single-page'">
      <page-content :breadcrumb="breadcrumb" :title="title" :logo="logo">
        <slot></slot>
      </page-content>
    </div>
</template>

<script>
import PageContent from './PageContent'
export default {
  name: 'PageLayout',
  components: {PageContent},
  props: ['logo', 'title'],
  data () {
    return {
      breadcrumb: []
    }
  },
  computed: {
    multipage () {
      return this.$store.state.setting.multipage
    }
  },
  mounted () {
    this.getBreadcrumb()
  },
  updated () {
    this.getBreadcrumb()
  },
  methods: {
    getBreadcrumb () {
      this.breadcrumb = this.$route.matched
    }
  }
}
</script>

<style lang="less" scoped>
  .link{
    margin-top: 16px;
    line-height: 24px;
    a{
      font-size: 14px;
      margin-right: 32px;
      i{
        font-size: 22px;
        margin-right: 8px;
      }
    }
  }
  .page-content{
    &.side{
      margin: 24px 24px 0;
    }
    &.head{
      margin: 24px auto 0;
      max-width: 1400px;
    }
  }
</style>
