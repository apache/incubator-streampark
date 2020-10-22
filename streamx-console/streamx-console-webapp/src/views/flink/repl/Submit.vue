<template>
  <div class="card-list table-page-search-wrapper" ref="content" style="padding: 10px;background-color: #fff;">
    <a-form layout="inline" @submit="handleReplSubmit" :form="form" style="height: 35px;">
      <a-row :gutter="24" style="height: 35px;">
        <a-col :md="4" :sm="12" style="float: right">
          <a-form-item>
            <a-select>
              <a-select-option
                v-for="(env,index) in envs"
                :value="env.env"
                :key="index">
                <div>
                  {{ env.env }}
                </div>
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :md="4" :sm="12" class="icon-list" style="float: right">
          <a-icon
            type="fullscreen-exit"
            twoToneColor="#4a9ff5"
            style="float: right; padding-left: 10px;"/>
          <a-icon
            type="play-circle"
            twoToneColor="#4a9ff5"
            @click="handleReplSubmit"
            style="float: right;padding-left: 10px;"/>
          <a-icon
            type="project"
            twoToneColor="#4a9ff5"
            style="float: right;padding-left: 10px;"/>
        </a-col>
      </a-row>
    </a-form>
    <a-divider style="margin-bottom: 20px;top: -20px"></a-divider>
    <a-row :gutter="24" style="clear: right;top: -30px">
      <a-col>
        <textarea ref="code" class="code"></textarea>
      </a-col>
    </a-row>
    <mavon-editor
      v-if="tutorial"
      v-model="tutorial"
      :toolbarsFlag="false"
      :subfield="false"
      :ishljs="true"
      :preview="true"
      defaultOpen="preview">
    </mavon-editor>
  </div>
</template>

<script>
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'
import { get } from '@api/tutorial'

import 'codemirror/theme/idea.css'
import 'codemirror/theme/cobalt.css'
import 'codemirror/theme/eclipse.css'
import 'codemirror/lib/codemirror.css'
import 'codemirror/addon/hint/show-hint.css'

import 'codemirror/addon/edit/matchbrackets'
import 'codemirror/addon/selection/active-line'
import 'codemirror/addon/hint/anyword-hint'
import 'codemirror/mode/clike/clike'
import 'codemirror/mode/sql/sql'
const CodeMirror = require('codemirror/lib/codemirror')

export default {
  name: 'CodeMirror',
  components: { mavonEditor },
  data () {
    return {
      editor: null,
      code: '',
      form: null,
      envs: [
        {
          env: 'flink',
          info: 'Creates ExecutionEnvironment/StreamExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment and provides a Scala environment '
        },
        {
          env: 'flink.pyflink',
          info: 'Provides a python environment '
        },
        {
          env: 'flink.ipyflink',
          info: ' Provides an ipython environment '
        },
        {
          env: 'flink.ssql',
          info: 'Provides a stream sql environment '
        },
        {
          env: 'flink.bsql',
          info: ' Provides a batch sql environment'
        }
      ],
      tutorial: null,
      toolbars: false
    }
  },
  mounted () {
    this.editor = CodeMirror.fromTextArea(this.$refs.code, {
      theme: 'eclipse',
      mode: 'text/x-scala',
      lineWrapping: true,	// 代码折叠
      foldGutter: true,
      indentWithTabs: true,
      smartIndent: true,
      lineNumbers: false,
      matchBrackets: true,
      autofocus: true,
      extraKeys: { 'Ctrl': 'autocomplete' }, // 自定义快捷键
      hintOptions: { // 自定义提示选项
        tables: {
          users: ['name', 'score', 'birthDate'],
          countries: ['name', 'population', 'size']
        }
      }
    })
    this.handleReadmd()
    this.form = this.$form.createForm(this)
  },

  methods: {
    handleReplSubmit () {
      const code = this.editor.getValue()
      console.log(code)
    },
    handleReadmd () {
      get({
        name: 'repl'
      }).then((resp) => {
        this.tutorial = resp.data.content
      })
    }
  }
}
</script>

<style scoped>

.code {
  height: 500px;
  display: block;
  font-size: 11pt;
  position: relative;
  width: 100%;
  z-index: 4;
  font-family: Consolas, Menlo, Monaco, Lucida Console, Liberation Mono, DejaVu Sans Mono, Bitstream Vera Sans Mono, Courier New, monospace, serif;
}

.CodeMirror-line:focus {
  background-color: aliceblue !important;
  left: 0px;
}

>>>.CodeMirror-line::before {
  content: '';
  background-color: #1890ff;
  width: 6px;
  height: 21px;
  margin-left: 5px;
  position: absolute;
  margin-right: 5px;
  left: 0;
}

>>>.CodeMirror pre.CodeMirror-line {
  padding: 0 4px 0 14px;
}

.card-list {
  position: relative;
}

.icon-list {
  top: 10px;
  height: 30px;
}

.CodeMirror {
  position: relative;
  overflow: hidden;
  background: white;
  top: 0;
  z-index: 4;
}

</style>
