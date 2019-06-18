<template>
  <div class="head-info" :class="center && 'center'">
    <span>{{ title }}</span>
    <p><a>{{ content }}</a></p>
  </div>
</template>

<script>
export default {
  name: 'HeadInfo',
  props: {
    title: {
      default: ''
    },
    content: {
      default: ''
    },
    bordered: {
      default: false
    },
    center: {
      default: true
    }
  }
}
</script>

<style lang="less" scoped>
  .head-info {
    position: relative;
    text-align: left;
    padding: 0 32px 0 0;
    min-width: 125px;

    &.center {
      text-align: center;
      padding: 0 32px;
    }

    span {
      color: rgba(0, 0, 0, .45);
      display: inline-block;
      font-size: .95rem;
      line-height: 32px;
      margin-bottom: 4px;
    }
    p {
      line-height: 32px;
      margin: 0;
      a {
        font-weight: 600;
        font-size: 1rem;
      }
    }
  }
</style>
