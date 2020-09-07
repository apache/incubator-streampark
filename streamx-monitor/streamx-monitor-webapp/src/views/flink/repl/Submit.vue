<template>
  <div class="card-list" ref="content" style="padding: 10px;background-color: #fff;">
    <div class="icon-list" style="height: 30px;width: 100%;z-index: 5">
      <a-icon type="fullscreen-exit"
              twoToneColor="#4a9ff5"
              style="float: right; padding-left: 10px;"/>
      <a-icon type="play-circle"
              twoToneColor="#4a9ff5"
              style="float: right;padding-left: 10px;"/>
      <a-icon type="project"
              twoToneColor="#4a9ff5"
              style="float: right;padding-left: 10px;"/>
    </div>
    <textarea ref="code" class="code" v-model="code"></textarea>
  </div>
</template>
s
<script>
import "codemirror/theme/idea.css"
import 'codemirror/theme/eclipse.css'
import "codemirror/lib/codemirror.css"
import "codemirror/addon/hint/show-hint.css"
require("codemirror/addon/edit/matchbrackets")
require("codemirror/addon/selection/active-line")
require("codemirror/addon/hint/anyword-hint")
require("codemirror/mode/clike/clike")
let CodeMirror = require("codemirror/lib/codemirror")

export default {
  name: "codeMirror",
  data () {
    return {
      code: ''
    }
  },
  mounted () {
    let editor = CodeMirror.fromTextArea(this.$refs.code, {
      theme: 'idea',
      mode: "text/x-scala",
      lineWrapping: true,	//代码折叠
      foldGutter: true,
      indentWithTabs: true,
      smartIndent: true,
      lineNumbers: false,
      matchBrackets: true,
      autofocus: true,
      extraKeys: {'Ctrl': 'autocomplete'},//自定义快捷键
      hintOptions: {//自定义提示选项
        tables: {
          users: ['name', 'score', 'birthDate'],
          countries: ['name', 'population', 'size']
        }
      }
    })
    //代码自动提示功能，记住使用cursorActivity事件不要使用change事件，这是一个坑，那样页面直接会卡死
    editor.on('cursorActivity', function () {
      editor.showHint()
    })
  }
}
</script>

<style>
.code {
  height:500px;
  font-size: 11pt;
  position: absolute;
  z-index: 4;
  font-family: Consolas, Menlo, Monaco, Lucida Console, Liberation Mono, DejaVu Sans Mono, Bitstream Vera Sans Mono, Courier New, monospace, serif;
}

.CodeMirror-line:hover {
  background-color: aliceblue !important;
  left: 0px;
}

.CodeMirror-line::before {
  content: '';
  background-color: #d9d9d9;
  width: 4px;
  height: 21px;
  margin-left: 5px;
  position: absolute;
  margin-right: 5px;
  left: 0;
}

.CodeMirror pre.CodeMirror-line{
  padding: 0 4px 0 14px;
}
.code{
  display: block;
  position: relative;
  top: -31px;
}
.card-list{
  position: relative;
}
.icon-list{
  height: 30px;
  width: 200px;
  z-index: 5;
  position: absolute;
  right: 20px;
  top: 18px;
}
 .CodeMirror {
   position: relative;
   overflow: hidden;
   background: white;
   top: 0;
   z-index: 4;
 }
</style>
