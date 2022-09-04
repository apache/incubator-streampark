<template>
  <a-drawer
    :mask-closable="false"
    width="calc(100% - 20%)"
    placement="right"
    :closable="true"
    @close="handleCancel"
    :visible="visiable"
    class="drawer-conf">
    <template slot="title">
      <template>
        <svg-icon
          v-if="readOnly"
          name="see"/>
        <svg-icon
          v-else
          name="edit"/>
        {{ title }}
      </template>
    </template>

    <div v-show="!visibleDiff">
      <div id="monaco_config"></div>
      <div class="drawer-bottom-button">
        <div
          style="float: right">
          <a-button
            type="primary"
            class="drwaer-button-item"
            @click="handleCancel">
            Cancel
          </a-button>
          <a-button
            v-if="changed"
            type="primary"
            class="drwaer-button-item"
            @click="handleNext()">
            <a-icon type="right"/>
            Next
          </a-button>
        </div>
      </div>
    </div>

    <div v-if="visibleDiff">
      <div id="monaco_mergely"></div>
      <div class="drawer-bottom-button" style="position: absolute">
        <div
          style="float: right">
          <a-button
            v-if="changed"
            type="primary"
            class="drwaer-button-item"
            @click="handleCloseDiff">
            <a-icon type="left"/>
            Previous
          </a-button>
          <a-button
            v-if="!compareMode"
            class="drwaer-button-item"
            type="primary"
            icon="cloud"
            @click="handleOk">
            Apply
          </a-button>
        </div>
      </div>
    </div>
  </a-drawer>
</template>

<script>
import monaco from './Monaco.yaml'
import SvgIcon from '@/components/SvgIcon'
export default {
  name: 'Mergely',
  components: {SvgIcon},
  props: {
    visiable: {
      type: Boolean,
      default: false
    },
    readOnly: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      compareMode: false,
      title: 'edit configuration',
      editor: null,
      changed: false,
      originalValue: null,
      targetValue: null,
      visibleDiff: false,
      loading: false
    }
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
          readOnly: this.readOnly,
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

    handleNext() {
      this.visibleDiff = true
      this.title = 'Compare configuration'
      this.handleDifferent(this.originalValue, this.targetValue)
    },

    set(value) {
      this.compareMode = false
      this.changed = false
      this.targetValue = null
      this.originalValue = value
      if (this.readOnly) {
        this.title = 'Configuration detail'
      }
      this.$nextTick(() => {
        if (this.editor == null) {
          this.editor = monaco.editor.create(document.querySelector('#monaco_config'), this.getOption())
          this.editor.onDidChangeModelContent((event) => {
            // 第一次
            if (this.targetValue) {
              this.changed = true
            }
            this.targetValue = this.editor.getValue()
          })
          this.$nextTick(() => {
            const elem = document.querySelector('#monaco_config')
            this.handleHeight(elem, 130)
          })
        }
        this.$nextTick(()=>{
          this.visibleDiff = false
          this.editor.getModel().setValue(this.originalValue)
        })
      })
    },

    theme () {
      if(this.editor != null) {
        this.editor.updateOptions({
          theme: this.ideTheme()
        })
      }
    },

    compare(original, modified, title) {
      this.compareMode = true
      this.visibleDiff = true
      this.title = title
      setTimeout(() => {
        this.handleDifferent(original, modified)
      }, 100)
    },

    handleDifferent(original, modified) {
      this.$nextTick(() => {
        const elem = document.querySelector('#monaco_mergely')
        this.handleHeight(elem, 100)
        const originalModel = monaco.editor.createModel(original, 'yaml')
        const modifiedModel = monaco.editor.createModel(modified, 'yaml')
        const diffEditor = monaco.editor.createDiffEditor(elem, this.getOption())
        diffEditor.setModel({
          original: originalModel,
          modified: modifiedModel
        })
      })
    },

    handleCancel() {
      this.changed = false
      this.targetValue = null
      this.originalValue = null
      this.visibleDiff = false
      this.loading = false
      this.$emit('close')
    },

    handleOk() {
      this.$emit('ok', this.targetValue)
      this.handleCancel()
    },

    handleCloseDiff() {
      this.title = 'Edit configuration'
      this.visibleDiff = false
    },

    handleHeight(elem, h) {
      const height = document.documentElement.offsetHeight || document.body.offsetHeight
      $(elem).css('height', (height - h) + 'px')
    }
  }

}

</script>

<style scoped>
.drawer-conf >>> .ant-drawer-body {
  padding: 5px !important;
  padding-bottom: 0px !important;
}

.drawer-bottom-button {
  position: absolute;
  padding-top: 10px;
  padding-right: 50px;
  width: 100%;
  bottom: 10px;
  z-index: 9;
}

.drwaer-button-item {
  margin-right: 20px;
}
</style>
