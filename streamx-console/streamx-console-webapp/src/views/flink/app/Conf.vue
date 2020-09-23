<template>
  <a-drawer
    :maskClosable="false"
    width="calc(100% - 25%)"
    placement="right"
    :closable="true"
    @close="handleCancel"
    :visible="visiable"
    class="drawer-conf">

    <template slot="title">
      <template>
        <a-icon v-if="!visibleDiff" type="setting"/>
        <a-icon v-else type="deployment-unit"/>
        {{ title }}
      </template>
    </template>

    <div v-show="!visibleDiff">
      <a-textarea ref="confEdit" class="confEdit"/>
      <div class="drawer-bootom-button">
        <div style="float: right">
          <a-button class="drwaer-button-item" @click="handleCancel">
            取消
          </a-button>
          <a-button v-if="changed" type="primary" class="drwaer-button-item" @click="handleNext()">
            <a-icon type="right"/>
            下一步
          </a-button>
        </div>
      </div>
    </div>

    <div v-if="visibleDiff">
      <div class="mergely-full-screen">
        <div class="mergely-resizer">
          <div id="mergely"></div>
        </div>
      </div>
      <div class="drawer-bootom-button">
        <div style="float: right">
          <a-button v-if="changed" type="primary" class="drwaer-button-item" @click="handleCloseDiff">
            <a-icon type="left"/>
            上一步
          </a-button>
          <a-button v-if="!compactMode" class="drwaer-button-item" type="primary" icon="cloud" @click="handleOk">确定
          </a-button>
        </div>
      </div>
    </div>

  </a-drawer>
</template>

<script>
import 'codemirror/lib/codemirror.css'
import 'codemirror/mode/yaml/yaml'
import 'codemirror/addon/edit/matchbrackets'
import 'codemirror/addon/selection/active-line'
import 'codemirror/mode/clike/clike'
import 'codemirror/mode/sql/sql'
import 'mergely/lib/mergely.css'
import 'mergely/lib/mergely'

export default {
  name: 'Conf',
  props: {
    visiable: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      compactMode: false,
      title: '配置编辑',
      codeMirror: null,
      confCode: null,
      changed: false,
      value: null,
      visibleDiff: false,
      loading: false
    }
  },

  methods: {

    handleCodeMirror () {
      if (this.codeMirror == null) {
        this.codeMirror = CodeMirror.fromTextArea(document.querySelector('.confEdit'), {
          tabSize: 2,
          styleActiveLine: true,
          lineNumbers: true,
          line: true,
          foldGutter: true,
          styleSelectedText: true,
          matchBrackets: true,
          showCursorWhenSelecting: true,
          extraKeys: { 'Ctrl': 'autocomplete' },
          lint: true,
          readOnly: false,
          autoMatchParens: true,
          mode: 'text/x-yaml',
          theme: 'default',	// 设置主题
          lineWrapping: true, // 代码折叠
          gutters: ['CodeMirror-linenumbers', 'CodeMirror-foldgutter', 'CodeMirror-lint-markers']
        })

        this.codeMirror.on('change', (editor, change) => {
          // 第一次
          if (!this.confCode) {
            this.confCode = change.text
          } else {
            this.changed = true
            this.confCode = this.codeMirror.getValue()
          }
        })
        this.$nextTick(() => {
          this.handleSetValue()
        })
      } else {
        this.handleSetValue()
      }
    },

    handleSetValue () {
      this.codeMirror.setValue(this.value)
      setTimeout(() => {
        this.codeMirror.refresh()
      }, 1)
    },

    handleNext () {
      this.visibleDiff = true
      this.title = '配置对比'
      this.handleMergely(this.value, this.confCode)
    },

    set (value) {
      this.compactMode = false
      this.changed = false
      this.confCode = null
      this.value = value
      this.$nextTick(() => {
        this.handleCodeMirror()
      })
    },

    compact (val1, val2) {
      this.compactMode = true
      this.visibleDiff = true
      this.title = '配置对比'
      setTimeout(() => {
        this.handleMergely(val1, val2)
      }, 100)
    },

    handleMergely (val1, val2) {
      this.$nextTick(() => {
        jQuery('#mergely').mergely({
          cmsettings: { mode: 'text/plain', readOnly: true },
          lhs: (setValue) => {
            setValue(val1)
          },
          rhs: (setValue) => {
            setValue(val2)
          }
        })
        this.$nextTick(() => {
          jQuery('#mergely-splash').remove()
        })
      })
    },

    handleCancel () {
      this.changed = false
      this.confCode = null
      this.value = null
      this.visibleDiff = false
      this.loading = false
      this.$emit('close')
    },

    handleOk () {
      this.$emit('ok', this.confCode)
      this.handleCancel()
    },

    handleCloseDiff () {
      this.title = '配置编辑'
      this.visibleDiff = false
    }
  }

}

</script>

<style scoped>

.drawer-conf {
  z-index: 1055;
}

.drawer-conf >>> .CodeMirror {
  border: 1px solid #eee;
  height: calc(100% - 50px);
}

.drawer-conf >>> .CodeMirror-scroll {
  height: auto;
  overflow-y: hidden;
  overflow-x: auto;
}

.drawer-conf >>> .drawer-bootom-button {
  position: absolute;
  background-color: white;
  padding-top: 30px;
  height: 80px;
  width: 100%;
  padding-right: 50px;
  bottom: 0;
  z-index: 9;
}

.drawer-conf >>> .drwaer-button-item {
  margin-right: 20px;
}

.mergely-full-screen {
  position: absolute;
  top: 80px;
  bottom: 20px;
  left: 20px;
  right: 20px;
  overflow: hidden;
}

</style>
