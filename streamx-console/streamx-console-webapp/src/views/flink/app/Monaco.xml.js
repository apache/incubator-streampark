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
import * as monaco from 'monaco-editor'

monaco.languages.registerCompletionItemProvider('xml', {
  provideCompletionItems: function(model, position) {
    const textUntilPosition = model.getValueInRange({startLineNumber: 1, startColumn: 1, endLineNumber: position.lineNumber, endColumn: position.column})
    //dependency...
    if (textUntilPosition.match(/\s*<dep(.*)\s*\n*(.*\n*)*(<\/dependency>|)?$/)) {
      const word = model.getWordUntilPosition(position)
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn
      }
      const suggestions = [{
        label: '"dependency"',
        insertText: 'dependency>\n' +
          '    <groupId></groupId>\n' +
          '    <artifactId></artifactId>\n' +
          '    <version></version>\n' +
          '</dependency'
      },
        { label: '"group"', insertText: 'groupId></groupId' },
        { label: '"artifactId"', insertText: 'artifactId></artifactId' },
        { label: '"version"', insertText: 'version></version' }
      ]

      if ( textUntilPosition.indexOf('<exclusions>') >0 ) {
        suggestions.push({
          label: '"exclusion"',
          insertText: 'exclusion>\n' +
            '  <artifactId></artifactId>\n' +
            '  <groupId></groupId>\n' +
            '</exclusion'
        })
      } else {
        suggestions.push({
          label: '"exclusions"',
          insertText: 'exclusions>\n' +
            '  <exclusion>\n' +
            '    <artifactId></artifactId>\n' +
            '    <groupId></groupId>\n' +
            '  </exclusion>\n' +
            '</exclusions'
        })
      }
      suggestions.forEach(x=>{
        x.kind = monaco.languages.CompletionItemKind.Function
        x.range = range
      })
      return { suggestions: suggestions }
    }
  }
})

export default monaco