/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import monaco from '@/views/flink/app/Monaco.xml'
import { verify } from '@/api/flinksql'

const globalOption = {
  theme: 'vs', //vs, hc-black, or vs-dark
  language: 'sql',
  selectOnLineNumbers: false,
  foldingStrategy: 'indentation', // 代码分小段折叠
  overviewRulerBorder: false, // 不要滚动条边框
  autoClosingBrackets: true,
  tabSize: 2, // tab 缩进长度
  readOnly: false,
  inherit: true,
  scrollBeyondLastLine: false,
  lineNumbersMinChars: 5,
  lineHeight: 24,
  automaticLayout: true,
  cursorBlinking: 'line',
  cursorStyle:'line',
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

export function initEditor(vue) {
  const controller = vue.controller
  controller.flinkSQL.value = arguments[1] || controller.flinkSQL.defaultValue
  const option = Object.assign({},globalOption)
  option.value = controller.flinkSQL.value
  option.minimap = { enabled: false }
  controller.editor.flinkSQL = monaco.editor.create(document.querySelector('#flink-sql'), option)
  vue.$nextTick(() => {
    const bigScreen = document.querySelector('.big-screen')
    document.querySelector('#flink-sql>.monaco-editor').appendChild(bigScreen)
  })

  //输入事件触发...
  controller.editor.flinkSQL.onDidChangeModelContent(() => {
    controller.flinkSQL.value = controller.editor.flinkSQL.getValue()
    verifySQL(vue)
  })

  //pom
  const pomOption = Object.assign({},globalOption)
  pomOption.language = 'xml'
  pomOption.value = controller.pom.defaultValue
  pomOption.minimap = { enabled: false }
  controller.editor.pom = monaco.editor.create(document.querySelector('.pom-box'), pomOption)
  vue.$nextTick(() => {
    const applyPom = document.querySelector('.apply-pom')
    document.querySelector('.pom-box>.monaco-editor').appendChild(applyPom)
  })

  controller.editor.pom.onDidChangeModelContent(() => {
    controller.pom.value = controller.editor.pom.getValue()
  })
}

export function verifySQL(vue) {
  const controller = vue.controller
  const callback = arguments[1] || function(r) {
  }
  verify({ 'sql': controller.flinkSQL.value }).then((resp) => {
    const success = resp.data == true || resp.data == 'true'
    if (success) {
      controller.flinkSQL.success = true
      controller.flinkSQL.errorMsg = null
      controller.flinkSQL.errorSQL = null
      controller.flinkSQL.errorStart = null
      controller.flinkSQL.errorEnd = null
      syntaxError(vue)
    } else {
      controller.flinkSQL.success = false
      controller.flinkSQL.errorLine = resp.line
      controller.flinkSQL.errorColumn = resp.column
      controller.flinkSQL.errorSQL = resp.sql
      controller.flinkSQL.errorStart = resp.start
      controller.flinkSQL.errorEnd = resp.end
      switch (resp.type) {
        case 4:
          controller.flinkSQL.errorMsg = 'Unsupported sql'
          break
        case 5:
          controller.flinkSQL.errorMsg = 'SQL is not endWith \';\''
          break
        default:
          controller.flinkSQL.errorMsg = resp.message
          break
      }
      syntaxError(vue)
    }
    callback(success)
  }).catch((error) => {
    //vue.$message.error(error.message)
  })

}

export function syntaxError(vue) {
  const controller = vue.controller
  const editor = controller.visiable.bigScreen
    ? controller.editor.bigScreen
    : controller.editor.flinkSQL

  const model = editor.getModel()
  monaco.editor.setModelMarkers(model, 'sql', [])
  if (!controller.flinkSQL.success) {
    try {
      const startFind = model.findMatches(controller.flinkSQL.errorStart)
      const endFind = model.findMatches(controller.flinkSQL.errorEnd)
      const startLineNumber = startFind[0].range.startLineNumber
      let endLineNumber = startLineNumber
      for (let i = 0; i < endFind.length; i++) {
        const find = endFind[i]
        if (find.range.endLineNumber >= startLineNumber) {
          endLineNumber = find.range.endLineNumber
          break
        }
      }
      //清空
      monaco.editor.setModelMarkers(model, 'sql', [{
          startLineNumber: startLineNumber,
          endLineNumber: endLineNumber + 1,
          severity: monaco.MarkerSeverity.Error,
          message: controller.flinkSQL.errorMsg
        }]
      )
    } catch (e) {
    }
  }
}

export function bigScreenOpen(vue) {
  const controller = vue.controller
  controller.visiable.bigScreen = true
  const option = Object.assign({},globalOption)
  vue.$nextTick(() => {
    option.value = controller.flinkSQL.value
    option.minimap = { enabled: true }
    option.language = 'sql'
    const elem = document.querySelector('#big-sql')
    const height = document.documentElement.offsetHeight || document.body.offsetHeight
    $(elem).css('height', (height - 120) + 'px')

    controller.editor.bigScreen = monaco.editor.create(elem, option)
    controller.editor.bigScreen.onDidChangeModelContent((event) => {
      const value = controller.editor.bigScreen.getValue()
      if (value != '') {
        controller.flinkSQL.value = value
        controller.editor.flinkSQL.getModel().setValue(value)
        verifySQL(vue)
      }
    })
    if (controller.flinkSQL.value != null && controller.flinkSQL.value.trim() != '') {
      verifySQL(vue)
    }
  })
}

export function bigScreenOk(vue,callback) {
  const controller = vue.controller
  verifySQL(vue, (success) => {
    if (success) {
      //销毁
      controller.editor.bigScreen.dispose()
      controller.visiable.bigScreen = false
      if (callback) {
        callback()
      }
    }
  })
}

export function bigScreenClose(vue) {
  verifySQL(vue)
}

export function applyPom(vue) {
  const controller = vue.controller
  const pom = controller.pom.value
  if (pom == null || pom.replace(/\\s+/, '') === '') {
    return
  }
  const groupExp = /<groupId>([\s\S]*?)<\/groupId>/
  const artifactExp = /<artifactId>([\s\S]*?)<\/artifactId>/
  const versionExp = /<version>([\s\S]*?)<\/version>/
  const exclusionsExp = /<exclusions>([\s\S]*?)<\/exclusions>/

  pom.split('</dependency>').filter(x => x.replace(/\\s+/, '') !== '').forEach(dep => {
    const groupId = dep.match(groupExp) ? (groupExp.exec(dep)[1]).trim() : null
    const artifactId = dep.match(artifactExp) ? (artifactExp.exec(dep)[1]).trim() : null
    const version = dep.match(versionExp) ? (versionExp.exec(dep)[1]).trim() : null
    const exclusion = dep.match(exclusionsExp) ? (exclusionsExp.exec(dep)[1]).trim() : null
    if (groupId != null && artifactId != null && version != null) {
      const id = groupId + '_' + artifactId
      const mvnPom = {
        'groupId': groupId,
        'artifactId': artifactId,
        'version': version
      }
      const pomExclusion = new Map()
      if (exclusion != null) {
        const exclusions = exclusion.split('<exclusion>')
        exclusions.forEach(e => {
          if (e != null && e.length > 0) {
            const e_group = e.match(groupExp) ? (groupExp.exec(e)[1]).trim() : null
            const e_artifact = e.match(artifactExp) ? (artifactExp.exec(e)[1]).trim() : null
            const id = e_group + '_' + e_artifact
            pomExclusion.set(id, {
              'groupId': e_group,
              'artifactId': e_artifact
            })
          }
        })
      }
      mvnPom.exclusions = pomExclusion
      controller.dependency.pom.set(id, mvnPom)
    } else {
      console.error('dependency error...')
    }
  })
  updateDependency(vue)
  controller.editor.pom.getModel().setValue(controller.pom.defaultValue)
}

export function updateDependency(vue) {
  const controller = vue.controller
  const deps = []
  const jars = []
  controller.dependency.pom.forEach((v, k, item) => {
    if (v.exclusions) {
      const exclusions = []
      v.exclusions.forEach((e) => exclusions.push(e))
      v.exclusions = exclusions
    }
    deps.push(v)
  })
  controller.dependency.jar.forEach((v, k, item) => {
    jars.push(v)
  })
  vue.dependency = deps
  vue.uploadJars = jars
}