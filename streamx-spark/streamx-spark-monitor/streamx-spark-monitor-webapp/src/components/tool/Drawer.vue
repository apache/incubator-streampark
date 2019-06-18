<template>
  <div >
    <div :class="['mask', openDrawer ? 'open' : 'close']" @click="close"></div>
    <div :class="['drawer', placement, openDrawer ? 'open' : 'close']">
      <div ref="drawer" style="position: relative; height: 100%;">
        <slot></slot>
      </div>
      <div v-if="showHandler" :class="['handler-container', placement]" ref="handler">
      </div>
    </div>
  </div>
</template>

<script>
import { mapMutations } from 'vuex'

export default {
  name: 'Drawer',
  data () {
    return {
      drawerWidth: 0
    }
  },
  props: {
    openDrawer: {
      type: Boolean,
      required: false,
      default: false
    },
    placement: {
      type: String,
      required: false,
      default: 'left'
    },
    showHandler: {
      type: Boolean,
      required: false,
      default: true
    }
  },
  mounted () {
    this.drawerWidth = this.getDrawerWidth()
  },
  watch: {
    'drawerWidth': function (val) {
      if (this.placement === 'left') {
        this.$refs.handler.style.left = val + 'px'
      } else {
        this.$refs.handler.style.right = val + 'px'
      }
    }
  },
  methods: {
    close () {
      this.setSettingBar(false)
    },
    getDrawerWidth () {
      return this.$refs.drawer.clientWidth
    },
    ...mapMutations({setSettingBar: 'setting/setSettingBar'})
  }
}
</script>

<style lang="less" scoped>
  .mask{
    position: fixed;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.2);
    transition: all 0.5s;
    z-index: 100;
    &.open{
      display: inline-block;
    }
    &.close{
      display: none;
    }
  }
  .drawer{
    position: fixed;
    height: 100%;
    transition: all 0.5s;
    z-index: 100;
    &.left{
      left: 0px;
      &.open{
        box-shadow: 2px 0 8px rgba(0,0,0,.15);
      }
      &.close{
        transform: translateX(-100%);
      }
    }
    &.right{
      right: 0px;
      &.open{
        box-shadow: -2px 0 8px rgba(0,0,0,.15);
      }
      &.close{
        transform: translateX(100%);
      }
    }
    .sider{
      height: 100%;
    }
  }
  .handler-container{
    position: fixed;
    top: 200px;
    text-align: center;
    transition: all 0.5s;
    cursor: pointer;
    .handler {
      height: 40px;
      width: 40px;
      background-color: #fff;
      z-index: 100;
      font-size: 26px;
      box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
      line-height: 40px;
    }
    &.left{
      .handler{
        border-radius: 0 5px 5px 0;
      }
    }
    &.right{
      .handler{
        border-radius: 5px 0 0 5px;
      }
    }
  }
</style>
