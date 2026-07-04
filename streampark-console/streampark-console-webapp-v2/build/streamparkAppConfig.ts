/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { Plugin } from 'vite'
import { loadEnv } from 'vite'

const GLOBAL_CONFIG_FILE_NAME = '_app.config.js'

function getVariableName(title: string) {
  return `__PRODUCTION__${title.replace(/\s/g, '_').replace(/-/g, '_') || '__APP'}__CONF__`
    .toUpperCase()
}

export function createStreamParkAppConfigPlugin(mode: string): Plugin {
  let source = ''
  let publicPath = '/'

  return {
    name: 'streampark-app-config',
    apply: 'build',
    configResolved(config) {
      publicPath = config.base
      const env = loadEnv(mode, process.cwd(), '')
      const appTitle = env.VITE_GLOB_APP_TITLE || env.VITE_APP_NAME || 'StreamPark'
      const variableName = getVariableName(appTitle)
      const globConfig = {
        VITE_GLOB_APP_TITLE: env.VITE_GLOB_APP_TITLE || appTitle,
        VITE_GLOB_APP_SHORT_NAME: env.VITE_GLOB_APP_SHORT_NAME || 'streampark',
        VITE_GLOB_API_URL: env.VITE_GLOB_API_URL ?? '',
        VITE_GLOB_API_URL_PREFIX: env.VITE_GLOB_API_URL_PREFIX ?? '',
        VITE_GLOB_UPLOAD_URL: env.VITE_GLOB_UPLOAD_URL ?? '/upload',
      }
      source = `${`window.${variableName}`}=${JSON.stringify(globConfig)};`
      source += `Object.freeze(window.${variableName});`
      source += `Object.defineProperty(window,"${variableName}",{configurable:false,writable:false});`
    },
    transformIndexHtml(html) {
      const base = publicPath.endsWith('/') ? publicPath : `${publicPath}/`
      const src = `${base}${GLOBAL_CONFIG_FILE_NAME}?v=${Date.now()}`
      return {
        html,
        tags: [{ tag: 'script', attrs: { src } }],
      }
    },
    generateBundle() {
      this.emitFile({
        type: 'asset',
        fileName: GLOBAL_CONFIG_FILE_NAME,
        source,
      })
    },
  }
}
