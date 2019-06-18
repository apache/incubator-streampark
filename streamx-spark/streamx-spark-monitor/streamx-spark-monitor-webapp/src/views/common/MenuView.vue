<template>
  <global-layout>
    <contextmenu :itemList="menuItemList" :visible.sync="menuVisible" @select="onMenuSelect"/>
    <a-tabs
      class="page-tabs"
      @contextmenu.native="e => onContextmenu(e)"
      v-if="multipage"
      :active-key="activePage"
      style="margin-top: -8px; margin-bottom: 8px"
      :hide-add="true"
      type="editable-card"
      @change="changePage"
      @edit="editPage">
      <a-tab-pane :id="page.fullPath" :key="page.fullPath" v-for="page in pageList" forceRender>
        <span slot="tab" :pagekey="page.fullPath">{{page.name}}</span>
      </a-tab-pane>
    </a-tabs>
      <keep-alive v-if="multipage">
        <router-view/>
      </keep-alive>
      <router-view v-else/>
  </global-layout>
</template>

<script>
import GlobalLayout from './GlobalLayout'
import Contextmenu from '~/menu/Contextmenu'

export default {
  name: 'MenuView',
  components: {Contextmenu, GlobalLayout},
  data () {
    return {
      pageList: [],
      linkList: [],
      activePage: '',
      menuVisible: false,
      menuItemList: [
        {key: '1', icon: 'arrow-left', text: '关闭左侧'},
        {key: '2', icon: 'arrow-right', text: '关闭右侧'},
        {key: '3', icon: 'close', text: '关闭其它'}
      ]
    }
  },
  computed: {
    multipage () {
      return this.$store.state.setting.multipage
    }
  },
  created () {
    this.pageList.push(this.$route)
    this.linkList.push(this.$route.fullPath)
    this.activePage = this.$route.fullPath
  },
  watch: {
    '$route': function (newRoute, oldRoute) {
      this.activePage = newRoute.fullPath
      if (!this.multipage) {
        this.linkList = [newRoute.fullPath]
        this.pageList = [newRoute]
      } else if (this.linkList.indexOf(newRoute.fullPath) < 0) {
        this.linkList.push(newRoute.fullPath)
        this.pageList.push(newRoute)
      }
    },
    'activePage': function (key) {
      this.$router.push(key)
    },
    'multipage': function (newVal, oldVal) {
      if (!newVal) {
        this.linkList = [this.$route.fullPath]
        this.pageList = [this.$route]
      }
    }
  },
  methods: {
    changePage (key) {
      this.activePage = key
    },
    editPage (key, action) {
      this[action](key)
    },
    remove (key) {
      if (this.pageList.length === 1) {
        this.$router.push('/')
        if (!this.pageList[0].meta.closeable) {
          return
        }
      }
      this.pageList = this.pageList.filter(item => item.fullPath !== key)
      let index = this.linkList.indexOf(key)
      this.linkList = this.linkList.filter(item => item !== key)
      index = index >= this.linkList.length ? this.linkList.length - 1 : index
      this.activePage = this.linkList[index]
    },
    onContextmenu (e) {
      const pagekey = this.getPageKey(e.target)
      if (pagekey !== null) {
        e.preventDefault()
        this.menuVisible = true
      }
    },
    getPageKey (target, depth) {
      depth = depth || 0
      if (depth > 2) {
        return null
      }
      let pageKey = target.getAttribute('pagekey')
      pageKey = pageKey || (target.previousElementSibling ? target.previousElementSibling.getAttribute('pagekey') : null)
      return pageKey || (target.firstElementChild ? this.getPageKey(target.firstElementChild, ++depth) : null)
    },
    onMenuSelect (key, target) {
      let pageKey = this.getPageKey(target)
      switch (key) {
        case '1':
          this.closeLeft(pageKey)
          break
        case '2':
          this.closeRight(pageKey)
          break
        case '3':
          this.closeOthers(pageKey)
          break
        case '4':
          this.closeAll(pageKey)
          break
        default:
          break
      }
    },
    closeOthers (pageKey) {
      let index = this.linkList.indexOf(pageKey)
      this.linkList = this.linkList.slice(index, index + 1)
      this.pageList = this.pageList.slice(index, index + 1)
      this.activePage = this.linkList[0]
    },
    closeLeft (pageKey) {
      let index = this.linkList.indexOf(pageKey)
      this.linkList = this.linkList.slice(index)
      this.pageList = this.pageList.slice(index)
      if (this.linkList.indexOf(this.activePage) < 0) {
        this.activePage = this.linkList[0]
      }
    },
    closeRight (pageKey) {
      let index = this.linkList.indexOf(pageKey)
      this.linkList = this.linkList.slice(0, index + 1)
      this.pageList = this.pageList.slice(0, index + 1)
      if (this.linkList.indexOf(this.activePage < 0)) {
        this.activePage = this.linkList[this.linkList.length - 1]
      }
    }
  }
}
</script>

<style scoped>
  >>>.ant-tabs-tab{
    margin-right: 1px !important;
  }
</style>
