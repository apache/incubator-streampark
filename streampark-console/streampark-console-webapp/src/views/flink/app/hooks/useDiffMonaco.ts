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
import { watch, Ref, unref, ref, computed } from 'vue';
import { until, createEventHook, tryOnUnmounted } from '@vueuse/core';

import type { editor as Editor } from 'monaco-editor';
import setupMonaco from '/@/monaco';
import { ThemeEnum } from '/@/enums/appEnum';
import { useRootSetting } from '/@/hooks/setting/useRootSetting';
const { getDarkMode } = useRootSetting();
export const isDark = computed(() => getDarkMode.value === ThemeEnum.DARK);

export function useDiffMonaco(
  target: Nullable<Ref>,
  language: string,
  getOriginal: Fn,
  getModified: Fn,
  options: Editor.IStandaloneDiffEditorConstructionOptions,
  immediate = true,
) {
  const changeEventHook = createEventHook<string>();
  const isSetup = ref(false);
  let diffEditor: Editor.IStandaloneDiffEditor;

  const getEditor = async (): Promise<Editor.IStandaloneDiffEditor> => {
    await until(isSetup).toBeTruthy();
    if (diffEditor) {
      return diffEditor;
    } else {
      return Promise.reject(null);
    }
  };

  const disposeEditor = async () => {
    diffEditor?.dispose();
  };

  const setupEditor = async (el, monaco?: any) => {
    if (!el) {
      console.warn('No editor found');
      return;
    }
    if (!monaco) {
      const monacoEditor = await setupMonaco();
      monaco = monacoEditor.monaco;
    }
    const originalModel = monaco.editor.createModel(getOriginal(), language);
    const modifiedModel = monaco.editor.createModel(getModified(), language);
    const defaultOptions = {
      tabSize: 2,
      insertSpaces: true,
      autoClosingQuotes: 'always',
      detectIndentation: false,
      folding: false,
      automaticLayout: true,
      theme: 'vs',
      minimap: {
        enabled: false,
      },
    };
    diffEditor = monaco.editor.createDiffEditor(el, Object.assign(defaultOptions, options || {}));
    diffEditor.setModel({
      original: originalModel,
      modified: modifiedModel,
    });
    isSetup.value = true;
    watch(
      isDark,
      () => {
        if (isDark.value) monaco.editor.setTheme('vs-dark');
        else monaco.editor.setTheme('vs');
      },
      { immediate: true },
    );
  };
  const init = async () => {
    const { monaco } = await setupMonaco();
    if (target != null) {
      watch(
        target,
        () => {
          const el = unref(target);
          if (!el) {
            return;
          }
          setupEditor(el, monaco);
        },
        {
          flush: 'post',
          immediate: true,
        },
      );
    }
  };

  immediate && init();

  tryOnUnmounted(() => stop());

  return {
    onChange: changeEventHook.on,
    getEditor,
    setupEditor,
    disposeEditor,
  };
}
