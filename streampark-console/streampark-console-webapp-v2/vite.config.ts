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

import { resolve } from 'node:path'
import { defineConfig, loadEnv } from 'vite'
import { createVitePlugins } from './build/plugins'
import { serviceConfig } from './service.config'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '') as ImportMetaEnv

  return {
    base: env.VITE_BASE_URL || '/',
    plugins: createVitePlugins(env, mode),
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
        '/#/': resolve(__dirname, 'types'),
      },
    },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_PORT) || 10002,
      watch: {
        ignored: [
          '**/dist/**',
          '**/src/typings/auto-imports.d.ts',
          '**/src/typings/components.d.ts',
          '**/src/typings/auto-proxy.d.ts',
        ],
      },
      proxy: {
        '/basic-api': {
          target: 'http://127.0.0.1:10000',
          changeOrigin: true,
          rewrite: path => path.replace(/^\/basic-api/, ''),
        },
      },
    },
    build: {
      target: 'esnext',
      reportCompressedSize: false,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules/monaco-editor'))
              return 'monaco-editor'
            if (id.includes('node_modules/sql-formatter'))
              return 'sql-formatter'
            if (id.includes('node_modules/@vicons'))
              return 'vicons'
            if (id.includes('node_modules/@iconify'))
              return 'iconify'
          },
        },
      },
    },
    define: {
      __SERVICE_CONFIG__: JSON.stringify(serviceConfig),
    },
  }
})
