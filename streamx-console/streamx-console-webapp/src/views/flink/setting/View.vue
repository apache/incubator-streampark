<template>
  <div>
    <a-card
      :bordered="false"
      style="margin-top: 24px"
      class="system_setting"
      title="System Setting">
      <a-list>
        <a-list-item v-for="(item,index) in settings" :key="index">
          <a-list-item-meta style="width: 50%">
            <svg-icon class="avatar" name="flink" size="large" slot="avatar" v-if="item.key === 'env.flink.home'"></svg-icon>
            <svg-icon class="avatar" name="workspace" size="large" slot="avatar" v-if="item.key === 'streamx.console.workspace'"></svg-icon>
            <svg-icon class="avatar" name="maven" size="large" slot="avatar" v-if="item.key === 'maven.central.repository'"></svg-icon>
            <svg-icon class="avatar" name="http" size="large" slot="avatar" v-if="item.key === 'streamx.console.webapp.address'"></svg-icon>
            <svg-icon class="avatar" name="host" size="large" slot="avatar" v-if="item.key === 'alert.email.host'"></svg-icon>
            <svg-icon class="avatar" name="port" size="large" slot="avatar" v-if="item.key === 'alert.email.port'"></svg-icon>
            <svg-icon class="avatar" name="mail" size="large" slot="avatar" v-if="item.key === 'alert.email.address'"></svg-icon>
            <svg-icon class="avatar" name="keys" size="large" slot="avatar" v-if="item.key === 'alert.email.password'"></svg-icon>
            <svg-icon class="avatar" name="ssl" size="large" slot="avatar" v-if="item.key === 'alert.email.ssl'"></svg-icon>
            <span slot="title">
              {{ item.title }}
            </span>
            <span slot="description">
              {{ item.description }}
            </span>
          </a-list-item-meta>
          <div class="list-content" style="width: 50%">
            <div class="list-content-item" style="width: 100%">
              <template v-if="item.type === 1">
                <input
                  v-if="item.editable"
                  :value="item.value"
                  :class="item.key.replace(/\./g,'_')"
                  class="ant-input"/>
                <div v-else style="width: 100%;text-align: right">
                  {{ item.value }}
                </div>
              </template>
              <template v-else>
                <a-switch
                  checked-children="ON"
                  un-checked-children="OFF"
                  style="float: right;margin-right: 30px"
                  :default-checked="item.value === 'true'"
                  @change="handleSwitch(item)" />
              </template>
            </div>
          </div>
          <div slot="actions" v-if="item.type === 1">
            <a v-if="!item.submitting" @click="handleEdit(item)">Edit</a>
            <a v-else @click="handleSubmit(item)">Submit</a>
            <a-divider type="vertical" />
            <a v-if="item.key === 'env.flink.home'" @click="handleFlinkConf()">Flink Conf</a>
          </div>
        </a-list-item>
      </a-list>
    </a-card>
    <a-drawer
      :mask-closable="false"
      width="calc(100% - 40%)"
      placement="right"
      :visible="visiable"
      :centered="true"
      :keyboard="false"
      :body-style="{ paddingBottom: '80px' }"
      title="Flink Conf"
      @close="handleClose()">
      <a-col style="font-size: 0.9rem">
        <div style="padding-bottom: 15px">
          Flink Home: &nbsp;&nbsp; {{ flinkHome }}
        </div>
        <div>
          Flink Conf:
          <div style="padding: 15px 0">
            <div id="conf"></div>
            <a-button
              type="primary"
              style="float:right;margin-top: 10px;margin-right: 130px"
              @click="handleSync">
              <a-icon type="sync" />
              Sync Conf
            </a-button>
          </div>
        </div>
      </a-col>
    </a-drawer>
  </div>
</template>

<script>
import {all, sync, update, getFlink } from '@api/setting'
import SvgIcon from '@/components/SvgIcon'
import monaco from '@/views/flink/app/Monaco.yaml'

export default {
  name: 'Setting',
  components: { SvgIcon },
  data() {
    return {
      settings: [],
      flinkHome: null,
      flinkConf: null,
      visiable: false,
      editor: null
    }
  },

  mounted() {
    this.form = this.$form.createForm(this)
    this.handleAll()
  },

  methods: {
    getOption() {
      return {
        theme: this.ideTheme(),
        language: 'yaml',
        selectOnLineNumbers: false,
        foldingStrategy: 'indentation', // 代码分小段折叠
        overviewRulerBorder: false, // 不要滚动条边框
        autoClosingBrackets: true,
        tabSize: 2, // tab 缩进长度
        readOnly: true,
        inherit: true,
        scrollBeyondLastLine: false,
        lineNumbersMinChars: 5,
        lineHeight: 24,
        automaticLayout: true,
        cursorBlinking: 'line',
        cursorStyle: 'line',
        cursorWidth: 3,
        renderFinalNewline: true,
        renderLineHighlight: 'all',
        quickSuggestionsDelay: 100,  //代码提示延时
        scrollbar: {
          useShadows: false,
          vertical: 'visible',
          horizontal: 'visible',
          horizontalSliderSize: 5,
          verticalSliderSize: 5,
          horizontalScrollbarSize: 15,
          verticalScrollbarSize: 15
        }
      }
    },

    handleAll() {
      all({}).then((resp) => {
        this.settings = resp.data
      })
    },

    handleEdit(setting) {
      if (!setting.editable) {
        setting.submitting = true
      }
      setting.editable = !setting.editable
    },

    handleSubmit(setting) {
      setting.submitting = false
      setting.editable = false
      const className = setting.key.replace(/\./g, '_')
      const elem = document.querySelector('.' + className)
      const value = elem.value
      update({
        key: setting.key,
        value: value
      }).then((resp) => {
        this.handleAll()
      })
    },

    handleSync () {
      sync({}).then((resp)=>{
        this.$swal.fire({
            icon: 'success',
            title: 'Flink default conf sync successful!',
            showConfirmButton: false,
            timer: 2000
          })
      })
    },

    handleFlinkConf () {
      this.visiable = true
      getFlink({}).then((resp)=>{
        this.flinkHome = resp.data.flinkHome
        this.flinkConf = resp.data.flinkConf
        this.handleInitEditor()
      })
    },

    handleInitEditor() {
      if (this.editor == null) {
        this.editor = monaco.editor.create(document.querySelector('#conf'), this.getOption())
        this.$nextTick(() => {
          const elem = document.querySelector('#conf')
          this.handleHeight(elem, 210)
        })
      }
      this.$nextTick(()=>{
        this.editor.getModel().setValue(this.flinkConf)
      })
    },

    handleClose() {
      this.visiable = false
    },

    handleHeight(elem, h) {
      const height = document.documentElement.offsetHeight || document.body.offsetHeight
      $(elem).css('height', (height - h) + 'px')
    },

    handleSwitch(setting) {
      update({
        key: setting.key,
        value: setting.value !== 'true'
      }).then((resp) => {
        this.handleAll()
      })
    }
  }

}
</script>

<style lang="less">
@import "View";
</style>
