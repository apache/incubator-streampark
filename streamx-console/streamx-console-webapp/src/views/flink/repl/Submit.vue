<template>
  <div>
    <a-card :bordered="false" style="margin-top: 0px;padding: 0px">
      <a-row :gutter="24" type="flex" justify="space-between">
        <a-col :span="22">
          <span style="height: 40px;margin-left: 17px;" class="code-prefix"></span>
          <a-form layout="inline" @submit="handleReplSubmit" :form="form" class="env-select">
            <a-form-item>
              <a-select style="z-index: 5;width: 100%" default-value="flink" @change="handleChangeEnv">
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
          </a-form>
          <div>
            <span style="height: 100%;margin-left: 17px;" class="code-prefix"></span>
            <span style="margin-left: 15px;color: grey">
              {{introduction}}
            </span>
          </div>
        </a-col>
        <a-col :span="2" class="icon-list" style="z-index: 2">
          <a-icon
            type="play-circle"
            twoToneColor="#4a9ff5"
            @click="handleReplSubmit"
            style="padding-left: 10px;"/>
          <a-icon
            type="read"
            twoToneColor="#4a9ff5"
            @click="showTutorial = !showTutorial"
            style="padding-left: 10px;"/>
          <a-icon
            type="fullscreen-exit"
            twoToneColor="#4a9ff5"
            style="padding-left: 10px;"/>
        </a-col>
      </a-row>
      <a-row :gutter="24" style="clear: right;">
        <a-col>
          <textarea ref="code" class="code"></textarea>
        </a-col>
      </a-row>
    </a-card>
    <mavon-editor
      v-if="tutorial && showTutorial"
      v-model="tutorial"
      :toolbarsFlag="false"
      :subfield="false"
      :ishljs="true"
      :preview="true"
      style="margin-top: 10px;z-index: 5"
      defaultOpen="preview">
    </mavon-editor>
  </div>
</template>

<script>
import { mavonEditor } from 'mavon-editor'
import { submit } from '@api/notebook'
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
          introduction: 'Creates ExecutionEnvironment/StreamExecutionEnvironment/BatchTableEnvironment/StreamTableEnvironment and provides a Scala environment as "senv"'
        },
        {
          env: 'pyflink',
          introduction: 'Provides a python environment '
        },
        {
          env: 'ipyflink',
          introduction: ' Provides an ipython environment '
        },
        {
          env: 'ssql',
          introduction: 'Provides a stream sql environment '
        },
        {
          env: 'bsql',
          introduction: ' Provides a batch sql environment'
        }
      ],
      tutorial: null,
      toolbars: false,
      showTutorial: false,
      env: 'flink',
      introduction: null
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
    this.handleIntroduction()
    this.form = this.$form.createForm(this)
  },

  methods: {
    handleReplSubmit () {
      const code = this.editor.getValue()
      console.log(code)
      submit({
        env: 'senv',
        sourceCode: code
      }).then((resp) => {
        console.log(resp.data)
      })
    },
    handleReadmd () {
      get({
        name: 'repl'
      }).then((resp) => {
        this.tutorial = resp.data.content
      })
    },
    handleIntroduction () {
      const env = this.envs.filter((x) => x.env === this.env)[0]
      this.introduction = env.introduction
    },
    handleChangeEnv (env) {
      this.env = env
      this.handleIntroduction()
    }
  }
}
</script>

<style scoped>

.code {
  height: 800px;
  display: block;
  font-size: 11pt;
  position: relative;
  width: 100%;
  font-family: Consolas, Menlo, Monaco, Lucida Console, Liberation Mono, DejaVu Sans Mono, Bitstream Vera Sans Mono, Courier New, monospace, serif;
}

.CodeMirror-line:focus {
  background-color: aliceblue !important;
  left: 0px;
}

>>>.CodeMirror-line::before,.code-prefix {
  content: '';
  background-color: #DDD;
  width: 4px;
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
  top: 0px;
  height: 20px;
}

.CodeMirror {
  position: relative;
  overflow: hidden;
  background: white;
  top: 0;
}
>>>.CodeMirror-lines{
  padding: 0px !important;
}

>>>.ant-select-selection--single {
  height: 24px !important;
  border-radius: 4px !important;
}

>>>.ant-select-selection__rendered{
  line-height: unset;
  margin-left: 5px;
}

>>>.ant-select-arrow {
  right: 5px;
}

.env-select {
  width: 110px;
  margin-left: 14px;
  margin-top: -4px;
  padding: 0px;
}

>>>.ant-card-body {
  padding: 15px !important;
}

>>>.v-note-wrapper {
  z-index: 5;
}

</style>
