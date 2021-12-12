<template>
  <div>
    <route-view />
  </div>
  
</template>

<script>
import { mapState, mapActions } from 'vuex'
import { mixin, mixinDevice } from '@/utils/mixin'
import config from '@/config/defaultSettings'

import RouteView from './RouteView'
import MultiTab from '@/components/MultiTab'
import SideMenu from '@/components/Menu/SideMenu'
import GlobalHeader from '@/components/GlobalHeader'
import GlobalFooter from '@/components/GlobalFooter'

export default {
  name: 'BasicView',
  mixins: [mixin, mixinDevice],
  components: {
    RouteView,
    MultiTab,
    SideMenu,
    GlobalHeader,
    GlobalFooter
  },
  data () {
    return {
      production: config.production,
      collapsed: false,
      menus: []
    }
  },
  computed: {
    ...mapState({
      // 动态主路由
      mainMenu: state => state.user.routers
    }),
    contentPaddingLeft () {
      if (!this.fixSidebar || this.isMobile()) {
        return '0'
      }
      if (this.sidebarOpened) {
        return '228px'
      }
      return '80px'
    }
  },
  watch: {
    sidebarOpened (val) {
      this.collapsed = !val
    }
  },
  created () {
    this.menus = this.mainMenu.find(item => item.path === '/').children
    this.collapsed = !this.sidebarOpened
  },
  mounted () {
    const userAgent = navigator.userAgent
    if (userAgent.indexOf('Edge') > -1) {
      this.$nextTick(() => {
        this.collapsed = !this.collapsed
        setTimeout(() => {
          this.collapsed = !this.collapsed
        }, 16)
      })
    }
  },
  methods: {
    ...mapActions(['setSidebar']),
    toggle () {
      this.collapsed = !this.collapsed
      this.setSidebar(!this.collapsed)
      this.triggerWindowResize()
    },
    paddingCalc () {
      let left = ''
      if (this.sidebarOpened) {
        left = this.isDesktop() ? '228px' : '80px'
      } else {
        left = (this.isMobile() && '0') || ((this.fixSidebar && '80px') || '0')
      }
      return left
    },
    menuSelect () {
      if (!this.isDesktop()) {
        this.collapsed = false
      }
    },
    drawerClose () {
      this.collapsed = false
    }
  }
}
</script>

<style lang="less">
@import url('../components/global.less');

/*
 * The following styles are auto-applied to elements with
 * transition="page-transition" when their visibility is toggled
 * by Vue.js.
 *
 * You can easily play with the page transition by editing
 * these styles.
 */

.page-transition-enter {
  opacity: 0;
}

.page-transition-leave-active {
  opacity: 0;
}

.page-transition-enter .page-transition-container,
.page-transition-leave-active .page-transition-container {
  -webkit-transform: scale(1.1);
  transform: scale(1.1);
}
</style>
