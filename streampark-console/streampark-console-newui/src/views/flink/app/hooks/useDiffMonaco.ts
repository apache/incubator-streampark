import { watch, Ref, unref, ref } from 'vue';
import { until, createEventHook, tryOnUnmounted } from '@vueuse/core';

import type { editor as Editor } from 'monaco-editor';
import setupMonaco from '/@/monaco';
import { useDark } from '@vueuse/core';
export const isDark = useDark();

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
