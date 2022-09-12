<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

<template>
  <a-layout-sider
    :class="['sider', isDesktop() ? null : 'shadow', theme, fixSiderbar ? 'ant-fixed-sidemenu' : null ]"
    width="240px"
    :collapsible="collapsible"
    v-model="collapsed"
    :trigger="null">
    <logo ref="logo"/>
    <s-menu
      :collapsed="collapsed"
      :menu="menu"
      theme="dark"
      :mode="mode"
      @select="onSelect"
      style="padding: 16px 0;" />
  </a-layout-sider>
</template>

<script>
import Logo from '@/components/tools/Logo'
import SMenu from './index'
import { mixin, mixinDevice } from '@/utils/mixin'

export default {
  name: 'SideMenu',
  components: { Logo, SMenu },
  mixins: [mixin, mixinDevice],
  data () {
    return {
      menu: []
    }
  },
  props: {
    mode: {
      type: String,
      required: false,
      default: 'inline'
    },
    theme: {
      type: String,
      required: false,
      default: 'dark'
    },
    collapsible: {
      type: Boolean,
      required: false,
      default: false
    },
    collapsed: {
      type: Boolean,
      required: false,
      default: false
    },
    menus: {
      type: Array,
      required: true
    }
  },
  methods: {
    onSelect (obj) {
      this.$emit('menuSelect', obj)
    },
    logoCollapsed () {
      this.$emit('menuSelect', obj)
    },
    // 处理菜单隐藏的。。。
    handleHideMenu (array, source) {
      source.filter((x) => { return !x.meta.hidden }).forEach((x) => {
        if (x.children && x.children.length > 0) {
          const children = []
          this.handleHideMenu(children, x.children)
          x.children = children
          array.push(x)
        } else {
          array.push(x)
        }
      })
    }
  },
  mounted () {
    const array = []
    this.handleHideMenu(array, this.menus)
    this.menu = array
  },
  watch: {
    collapsed(curr) {
      this.$refs.logo.collapsed(curr)
    }
  }
}
</script>
