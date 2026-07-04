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

import type { Ref } from 'vue'
import type { editor as Editor } from 'monaco-editor'
import { computed, ref, unref, watch } from 'vue'
import { createEventHook, tryOnUnmounted, until } from '@vueuse/core'
import { loadMonaco } from '@/monaco'
import { isFunction } from '@/utils/is'
import { useAppStore } from '@/store/app'

const appStore = useAppStore()
export const isDark = computed(() => appStore.colorMode === 'dark')

export interface MonacoEditorOption {
  code?: any
  language: string
  suggestions?: any[]
  options?: Editor.IStandaloneEditorConstructionOptions
}

export interface TextRange {
  startLineNumber: number
  endLineNumber: number
  startColumn: number
  endColumn: number
}

export function useMonaco(
  target: Ref,
  options: MonacoEditorOption,
  beforeMount?: (monoao: any) => Promise<void>,
) {
  const valueUpdateHook = createEventHook<string>()
  const isSetup = ref(false)
  let editor: Editor.IStandaloneCodeEditor
  let monacoInstance: any
  const registerCompletionMap = new Map()
  let disposable: { dispose: () => void } | undefined
  let stopThemeWatch: (() => void) | undefined

  const setContent = async (content: string) => {
    await until(isSetup).toBeTruthy()
    if (editor)
      editor.setValue(content)
  }

  const getContent = async () => {
    await until(isSetup).toBeTruthy()
    return editor ? editor.getValue() : ''
  }

  const getInstance = async () => {
    await until(isSetup).toBeTruthy()
    return editor ?? null
  }

  const getMonacoInstance = async () => {
    await until(isSetup).toBeTruthy()
    return monacoInstance ?? null
  }

  let suggestLabels: Array<Recordable> = []

  const createSuggestions = (
    monaco: any,
    range: TextRange,
    preWord: string,
    currentWord: string,
  ) => {
    const suggestions: Array<Recordable> = []
    for (let i = 0; i < suggestLabels.length; i++) {
      const id = suggestLabels[i].text
      const desc = suggestLabels[i].description
      suggestions.push({
        label: `${id}${desc ? `:${desc}` : ''}`,
        insertText: `${preWord !== '{' && currentWord !== '{' ? '{' : ''}${id}}`,
        detail: 'Variable Code',
        kind: monaco.languages.CompletionItemKind.Variable,
        range,
      })
    }
    return suggestions
  }

  const registerCompletion = async (monaco: any, languageId: string, callbackIds: any[]) => {
    if (callbackIds && callbackIds.length > 0) {
      suggestLabels = callbackIds
      if (registerCompletionMap.has(languageId))
        return
      registerCompletionMap.set(languageId, 1)
      disposable?.dispose()
      disposable = monaco.languages.registerCompletionItemProvider(languageId, {
        triggerCharacters: ['$', '{'],
        provideCompletionItems: async (model: any, position: Recordable) => {
          const word = model.getWordUntilPosition(position)
          const content = model.getLineContent(position.lineNumber)
          const currentWord = content[position.column - 2]
          const preWord = content[position.column - 3]
          const lastWord = content[position.column - 4]
          if (
            currentWord === '$'
            || (currentWord === '{' && preWord === '$')
            || (preWord === '{' && lastWord === '$')
          ) {
            const range: TextRange = {
              startLineNumber: position.lineNumber,
              endLineNumber: position.lineNumber,
              startColumn: word.startColumn,
              endColumn: word.endColumn,
            }
            return { suggestions: createSuggestions(monaco, range, preWord, currentWord) }
          }
          return { suggestions: [] }
        },
        resolveCompletionItem: () => [{ label: 'sss' }],
      })
    }
  }

  const setMonacoSuggest = async (suggestions: any[]) => {
    await until(isSetup).toBeTruthy()
    if (monacoInstance)
      registerCompletion(monacoInstance, options.language, suggestions)
  }

  const disposeInstance = async () => {
    stopThemeWatch?.()
    editor?.dispose()
  }

  const init = async () => {
    const { monaco } = await loadMonaco()
    monacoInstance = monaco
    if (isFunction(beforeMount))
      await beforeMount(monaco)

    watch(
      target,
      () => {
        const el = unref(target)
        if (!el)
          return
        const model = monaco.editor.createModel(options.code, options.language)
        const defaultOptions = {
          model,
          language: options.language,
          tabSize: 2,
          insertSpaces: true,
          autoClosingQuotes: 'always' as const,
          detectIndentation: false,
          folding: true,
          foldingStrategy: 'indentation' as const,
          automaticLayout: true,
          theme: 'vs',
          minimap: { enabled: false },
        }
        editor = monaco.editor.create(el, Object.assign(defaultOptions, options.options || {}))
        isSetup.value = true
        if (!options.options?.theme) {
          stopThemeWatch = watch(
            isDark,
            () => {
              monaco.editor.setTheme(isDark.value ? 'vs-dark' : 'vs')
            },
            { immediate: true },
          )
        }
        editor.getModel()?.onDidChangeContent(() => {
          valueUpdateHook.trigger(editor.getValue())
        })
      },
      { flush: 'post', immediate: true },
    )
  }

  init()

  tryOnUnmounted(() => {
    disposable?.dispose()
    disposeInstance()
  })

  return {
    onUpdateValue: valueUpdateHook.on,
    setContent,
    setMonacoSuggest,
    getContent,
    getInstance,
    getMonacoInstance,
    disposeInstance,
  }
}
