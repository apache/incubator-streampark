<template>
  <a-layout-sider class="sider" width="273">
    <setting-item title="导航栏颜色">
      <img-checkbox-group @change="setTheme">
        <img-checkbox img="static/img/side-bar-dark.svg" :checked="dark"
                      value="dark"/>
        <img-checkbox img="static/img/side-bar-light.svg" :checked="!dark"
                      value="light"/>
      </img-checkbox-group>
    </setting-item>
    <setting-item title="主题颜色">
      <color-checkbox-group @change="onColorChange" :defaultValues="defaultValues" :multiple="false">
        <template v-for="(color, index) in colorList">
          <color-checkbox :color="color" :value="index + 1" :key="index"/>
        </template>
      </color-checkbox-group>
    </setting-item>
    <a-divider/>
    <setting-item title="导航栏位置">
      <img-checkbox-group @change="setLayout">
        <img-checkbox img="static/img/side-bar-left.svg" :checked="side" value="side"/>
        <img-checkbox img="static/img/side-bar-top.svg" :checked="!side" value="head"/>
      </img-checkbox-group>
    </setting-item>
    <setting-item>
      <a-list :split="false">
        <a-list-item>
          固定顶栏
          <a-switch :checked="fixedHeader" slot="actions" size="small" @change="fixHeader"/>
        </a-list-item>
        <a-list-item>
          固定侧边栏
          <a-switch :checked="fixedSiderbar" slot="actions" size="small" @change="fixSiderbar"/>
        </a-list-item>
        <a-list-item>
          多页签模式
          <a-switch :checked="multipage" slot="actions" size="small" @change="setMultipage"/>
        </a-list-item>
      </a-list>
    </setting-item>
    <a-button style="width: 100%" icon="save" @click="updateUserConfig">保存设置</a-button>
  </a-layout-sider>
</template>

<script>
import SettingItem from './SettingItem'
import StyleItem from './StyleItem'
import ColorCheckbox from '../checkbox/ColorCheckbox'
import ImgCheckbox from '../checkbox/ImgCheckbox'
import { updateTheme } from 'utils/color'
import {mapState, mapMutations} from 'vuex'

const ColorCheckboxGroup = ColorCheckbox.Group
const ImgCheckboxGroup = ImgCheckbox.Group

export default {
  name: 'Setting',
  components: {ImgCheckboxGroup, ImgCheckbox, ColorCheckboxGroup, ColorCheckbox, StyleItem, SettingItem},
  computed: {
    ...mapState({
      multipage: state => state.setting.multipage,
      theme: state => state.setting.theme,
      colorList: state => state.setting.colorList,
      fixedSiderbar: state => state.setting.fixSiderbar,
      fixedHeader: state => state.setting.fixHeader,
      layout: state => state.setting.layout,
      color: state => state.setting.color,
      user: state => state.account.user
    }),
    dark () {
      return this.theme === 'dark'
    },
    side () {
      return this.layout === 'side'
    },
    defaultValues () {
      let currentColor = this.$store.state.setting.color
      if (Array.isArray(currentColor)) {
        currentColor = currentColor[0]
      }
      let index = this.colorList.indexOf(currentColor) + 1
      return `[${index}]`
    }
  },
  methods: {
    ...mapMutations({setSettingBar: 'setting/setSettingBar'}),
    onColorChange (values, colors) {
      if (colors.length > 0) {
        updateTheme(colors)
        this.$store.commit('setting/setColor', colors)
      }
    },
    setTheme (values) {
      this.$store.commit('setting/setTheme', values[0])
    },
    setLayout (values) {
      this.$store.commit('setting/setLayout', values[0])
    },
    setMultipage (checked) {
      this.$store.commit('setting/setMultipage', checked)
    },
    fixSiderbar (checked) {
      this.$store.commit('setting/fixSiderbar', checked)
    },
    fixHeader (checked) {
      this.$store.commit('setting/fixHeader', checked)
    },
    updateUserConfig () {
      this.$put('user/userconfig', {
        multiPage: this.multipage ? '1' : '0',
        theme: this.theme,
        fixedSiderbar: this.fixedSiderbar ? '1' : '0',
        fixedHeader: this.fixedHeader ? '1' : '0',
        layout: this.layout,
        color: this.color,
        userId: this.user.userId
      }).then(() => {
        this.$message.success('保存成功')
        this.setSettingBar(false)
      })
    }
  }
}
</script>

<style lang="less" scoped>
  .sider {
    background-color: #fff;
    height: 100%;
    padding: 24px;
    font-size: 14px;
    line-height: 1.5;
    word-wrap: break-word;
    position: relative;
    .flex {
      display: flex;
    }
  }
</style>
