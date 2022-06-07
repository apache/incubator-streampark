<template>
  <a-card
    :bordered="false"
    class="nodebook-submit"
    :style="{ height: '100%' }">
    <br>
    <a-alert
      show-icon
      message="this is Experimental Features,and only supported ExecutionEnvironment/StreamExecutionEnvironment "
      type="info"/><br>
    <a-row
      :gutter="24"
      type="flex"
      justify="space-between">
      <a-col :span="22">
        <span
          style="height: 40px;margin-left: 17px;"
          class="code-prefix"/>
        <a-form
          layout="inline"
          @submit="handleReplSubmit"
          :form="form"
          class="env-select">
          <a-form-item>
            <a-select
              style="z-index: 5;width: 100%"
              default-value="flink"
              @change="handleChangeEnv">
              <a-select-option
                v-for="(e,index) in envs"
                :value="e.env"
                :key="index">
                <div>
                  {{ e.env }}
                </div>
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
        <div>
          <span
            style="height: 100%;margin-left: 17px;"
            class="code-prefix"/>
          <span style="margin-left: 15px;color: grey">
            <slot>{{ introduction }}</slot>
          </span>
        </div>
      </a-col>
      <a-col
        :span="2"
        style="z-index: 2">
        <a-icon
          type="play-circle"
          two-tone-color="#4a9ff5"
          @click="handleReplSubmit"
          style="padding-left: 10px;"/>
        <a-icon
          v-if="1 === 2"
          type="read"
          two-tone-color="#4a9ff5"
          @click="showTutorial = !showTutorial"
          style="padding-left: 10px;"/>
        <a-icon
          type="fullscreen-exit"
          two-tone-color="#4a9ff5"
          style="padding-left: 10px;"/>
      </a-col>
      <a-col :span="24">
        <div class="code-box"></div>
      </a-col>
    </a-row>
  </a-card>
</template>

<script>

import * as monaco from 'monaco-editor'
import {submit} from '@api/notebook'
import {get} from '@api/tutorial'
import storage from '@/utils/storage'

export default {
  name: 'Submit',
  data() {
    return {
      code: '',
      form: null,
      content: '',
      configs: {
        status: false, // disable the status bar at the bottom
        spellChecker: false, // disable spell check
      },
      envs: [
        {
          env: 'flink',
          introduction: function () {
            return 'Creates ExecutionEnvironment/StreamExecutionEnvironment and provides a Scala environment as env'
          }
        },
        {
          env: 'pyflink',
          introduction: function () {
            return 'Provides a python environment '
          }
        },
        {
          env: 'ipyflink',
          introduction: function () {
            return ' Provides an ipython environment '
          }
        },
        {
          env: 'sql',
          introduction: function () {
            return 'Provides a StreamTableEnvironment '
          }
        }
      ],
      tutorial: null,
      toolbars: false,
      showTutorial: false,
      env: 'flink',
      introduction: null,
      woldCount: '\n%flink.repl.out=true\n' +
        '%flink.yarn.queue=default\n' +
        '%flink.execution.mode=yarn\n' +
        '%flink.yarn.appName=Socket Window WordCount with StreamX NoteBook\n\n' +
        '// the host and the port to connect to\n' +
        'val hostname = "localhost"\n' +
        'val port = 9999\n' +
        '\n' +
        '// get the execution environment\n' +
        'val env = StreamExecutionEnvironment.getExecutionEnvironment\n' +
        '\n' +
        'env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)\n' +
        '\n' +
        '// get input data by connecting to the socket\n' +
        'val text = env.socketTextStream(hostname, port, "\\n")\n' +
        '\n' +
        '// parse the data, group it, window it, and aggregate the counts\n' +
        'val windowCounts = text.flatMap(new FlatMapFunction[String,(String,Long)] {\n' +
        '    override def flatMap(value: String, out: Collector[(String, Long)]): Unit = {\n' +
        '       for (word <- value.split("\\\\s")) {\n' +
        '           out.collect(word,1L)\n' +
        '       }\n' +
        '    }\n' +
        '}).keyBy(0).timeWindow(Time.seconds(5)).reduce(new ReduceFunction[(String,Long)]() {\n' +
        '    override def reduce(a: (String, Long), b: (String, Long)): (String, Long) = (a._1,a._2 + b._2)\n' +
        '})\n' +
        '\n' +
        '// print the results with a single thread, rather than in parallel\n' +
        'windowCounts.print.setParallelism(1)\n' +
        'env.execute("Socket Window WordCount with StreamX NoteBook")\n'
    }
  },
  mounted() {
    const option = {
      theme: this.ideTheme(),
      language: 'java',
      value: this.woldCount,
      selectOnLineNumbers: false,
      foldingStrategy: 'indentation', // 代码分小段折叠
      overviewRulerBorder: false, // 不要滚动条边框
      autoClosingBrackets: true,
      tabSize: 2, // tab 缩进长度
      readOnly: false,
      inherit: true,
      scrollBeyondLastLine: false,
      lineNumbersMinChars: 4,
      lineHeight: 24,
      automaticLayout: true,
      cursorBlinking: 'line',
      cursorStyle: 'line',
      cursorWidth: 3,
      renderFinalNewline: true,
      renderLineHighlight: 'line',
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
    this.editor = monaco.editor.create(document.querySelector('.code-box'), option)
    this.handleReadmd()
    this.handleIntroduction()
    this.form = this.$form.createForm(this)
  },

  computed: {
    myTheme() {
      return this.$store.state.app.theme
    }
  },

  watch: {
    myTheme() {
      this.editor.updateOptions({
        theme: this.ideTheme()
      })
    }
  },

  methods: {
    handleReplSubmit() {
      const code = this.editor.getValue()
      this.$swal.fire({
        icon: 'success',
        title: 'this features is experimental\ncurrent job is starting...',
        showConfirmButton: false,
        timer: 2000
      }).then((r)=> {
        submit({
          env: 'senv',
          text: code
        }).then((resp) => {
          console.log(resp.data)
        })
      })
    },

    handleReadmd() {
      get({
        name: 'repl'
      }).then((resp) => {
        this.tutorial = resp.data.content
      })
    },
    handleIntroduction() {
      const env = this.envs.filter((x) => x.env === this.env)[0]
      this.introduction = env.introduction()
    },
    handleChangeEnv(env) {
      this.env = env
      this.handleIntroduction()
    }
  }
}
</script>

<style lang="less">
@import "Submit";
</style>
