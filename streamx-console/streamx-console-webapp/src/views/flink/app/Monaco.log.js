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
import storage from '@/utils/storage'
import {DEFAULT_THEME} from '@/store/mutation-types'

// Register a new language
monaco.languages.register({ id: 'log' })
monaco.languages.setMonarchTokensProvider('log', {
  tokenizer: {
    root: [
      [/.*\.Exception.*/,'log-error'],
      [/.*Caused\s+by:.*/,'log-error'],
      [/\s+at\s+.*/, 'log-info'],
      [/http:\/\/(.*):\d+(.*)\/application_\d+_\d+/, 'yarn-info'],
      [/Container\s+id:\s+container_\d+_\d+_\d+_\d+/, 'yarn-info'],
      [/yarn\s+logs\s+-applicationId\s+application_\d+_\d+/ , 'yarn-info'],
      [/\[20\d+-\d+-\d+\s+\d+:\d+:\d+\d+|.\d+]/, 'log-date'],
      [/\[[a-zA-Z 0-9:]+]/, 'log-date'],
    ]
  }
})

monaco.editor.defineTheme('log', {
  base: storage.get(DEFAULT_THEME) === 'dark' ? 'vs-dark' : 'vs',
  inherit: true,
  rules: [
    { token: 'log-info', foreground: '808080' },
    { token: 'log-error', foreground: 'ff0000', fontStyle: 'bold' },
    { token: 'log-notice', foreground: 'FFA500' },
    { token: 'yarn-info', foreground: '0066FF', fontStyle: 'bold'},
    { token: 'log-date', foreground: '008800' },
  ]
})

export default monaco
