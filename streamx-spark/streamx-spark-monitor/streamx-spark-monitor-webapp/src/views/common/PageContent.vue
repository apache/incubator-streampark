<template>
  <div class="page-content">
    <div :class="['page-header-wide', layout]">
      <div class="breadcrumb">
        <a-breadcrumb>
          <a-breadcrumb-item :key="item.path" v-for="(item, index) in breadcrumb">
            <span v-if="index === 0"><router-link to="/">{{item.name}}</router-link></span>
            <span v-else>{{item.name}}</span>
          </a-breadcrumb-item>
        </a-breadcrumb>
      </div>
      <div class="detail">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PageContent',
  props: {
    title: {
      type: String,
      required: false
    },
    breadcrumb: {
      type: Array,
      required: false
    },
    logo: {
      type: String,
      required: false
    }
  },
  computed: {
    layout () {
      return this.$store.state.setting.layout
    }
  }
}
</script>

<style lang="less" scoped>
  .page-content{
    background: #fff;
    padding: 14px 22px;
    border-left: 1px solid #e8e8e8;
    border-right: 1px solid #e8e8e8;
    border-bottom: 1px solid #e8e8e8;
    .page-header-wide{
      .breadcrumb{
        margin-bottom: .6rem;
      }
      .detail{
        display: flex;
        padding: 0 0 1rem 0;
        .row {
          display: flex;
          width: 100%;
        }
        .avatar {
          flex: 0 1 72px;
          margin:0 24px 8px 0;
          & > span {
            border-radius: 72px;
            display: block;
            width: 72px;
            height: 72px;
          }
        }
        .main{
          width: 100%;
          flex: 0 1 auto;
          .title{
            flex: auto;
            font-size: 20px;
            font-weight: 500;
            color: rgba(0,0,0,.85);
            margin-bottom: 16px;
          }
          .logo{
            width: 28px;
            height: 28px;
            border-radius: 4px;
            margin-right: 16px;
          }
          .content{
            margin-bottom: 16px;
            flex: auto;
          }
          .extra{
            flex: 0 1 auto;
            margin-left: 88px;
            min-width: 242px;
            text-align: right;
          }
          .action{
            margin-left: 56px;
            min-width: 266px;
            flex: 0 1 auto;
            text-align: right;
          }
        }
      }
    }
  }
</style>
