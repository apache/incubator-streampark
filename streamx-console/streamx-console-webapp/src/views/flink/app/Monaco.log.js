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
      [/.*Exception.*/,'log-error'],
      [/.*Caused\s+by:.*/,'log-error'],
      [/\s+at\s+.*/, 'log-info'],
      [/\[[a-zA-Z 0-9:]+]/, 'log-date'],
    ]
  }
})


monaco.editor.defineTheme('log', {
  base: storage.get(DEFAULT_THEME) === 'dark' ? 'vs-dark' : 'vs',
  inherit: false,
  rules: [
    { token: 'log-info', foreground: '808080' },
    { token: 'log-error', foreground: 'ff0000', fontStyle: 'bold' },
    { token: 'log-notice', foreground: 'FFA500' },
    { token: 'log-date', foreground: '008800' },
  ]
})


export default monaco
