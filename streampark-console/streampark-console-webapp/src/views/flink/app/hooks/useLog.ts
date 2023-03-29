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
import { ref, watch, computed } from 'vue';
import { useMonaco, isDark } from '/@/hooks/web/useMonaco';

export const useLog = () => {
  const logRef = ref<HTMLElement>();
  const autoScroll = ref(true);
  let editor: any;
  const { setContent, getInstance, getMonacoInstance } = useMonaco(
    logRef,
    {
      language: 'log',
      options: {
        theme: isDark.value ? 'log-dark' : 'log',
        readOnly: true,
        scrollBeyondLastLine: false,
        overviewRulerBorder: false, // Don't scroll bar borders
        tabSize: 2, // tab indent length
        minimap: { enabled: true },
        smoothScrolling: true,
        scrollbar: {
          useShadows: false,
          vertical: 'visible',
          horizontal: 'visible',
          horizontalSliderSize: 5,
          verticalSliderSize: 5,
          horizontalScrollbarSize: 15,
          verticalScrollbarSize: 15,
        },
      },
    },
    handleLogMonaco,
  );
  watch(
    isDark,
    async () => {
      const monacoInstance = await getMonacoInstance();
      if (monacoInstance) {
        if (isDark.value) monacoInstance.editor.setTheme('log-dark');
        else monacoInstance.editor.setTheme('log');
      }
    },
    { immediate: true },
  );
  watch(
    () => logRef.value,
    async () => {
      if (!logRef.value) return;
      const editorHeight = logRef.value.clientHeight;
      editor = await getInstance();
      editor?.onDidScrollChange((e: any) => {
        if (e.scrollTop > 0) {
          if (editorHeight + e.scrollTop + 15 >= e.scrollHeight) {
            autoScroll.value = true;
          } else {
            autoScroll.value = false;
            console.log('close');
          }
        }
      });
    },
  );
  function setAutoScroll(scroll: boolean) {
    autoScroll.value = scroll;
  }
  const getAutoScroll = computed(() => {
    return autoScroll.value;
  });
  /* registered language */
  async function handleLogMonaco(monaco: any) {
    monaco.languages.register({ id: 'log' });
    monaco.languages.setMonarchTokensProvider('log', {
      tokenizer: {
        root: [
          [/\[?20\d+-\d+-\d+\s+\d+:\d+:\d+\d+|.\d+/, 'custom-date-time'],
          [/\[error.*/, 'custom-error'],
          [/\[notice.*/, 'custom-notice'],
          [/\[info.*/, 'custom-info'],
          [/INFO/, 'custom-info-keyword'],
          [/\[[a-zA-Z 0-9:]+\]/, 'custom-date'],
        ],
      },
    });

    monaco.editor.defineTheme('log-dark', {
      base: 'vs-dark',
      inherit: true,
      colors: {},
      rules: [
        { token: 'custom-info', foreground: '808080' },
        { token: 'custom-info-keyword', foreground: '008800' },
        { token: 'custom-error', foreground: 'ff0000', fontStyle: 'bold' },
        { token: 'custom-notice', foreground: 'FFA500' },
        { token: 'custom-date', foreground: '008800' },
        { token: 'custom-date-time', foreground: '008800' },
      ],
    });
    monaco.editor.defineTheme('log', {
      base: 'vs',
      inherit: true,
      colors: {},
      rules: [
        { token: 'custom-info', foreground: '808080' },
        { token: 'custom-error', foreground: 'ff0000', fontStyle: 'bold' },
        { token: 'custom-notice', foreground: 'FFA500' },
        { token: 'custom-date', foreground: '008800' },
        { token: 'custom-date-time', foreground: '008800' },
      ],
    });
  }

  function handleRevealLine() {
    if (!autoScroll.value) return;
    if (editor) {
      setTimeout(() => {
        editor.revealLine(editor.getModel()?.getLineCount() + 1 || 0);
      }, 500);
    }
  }
  function getLineCount() {
    if (editor) {
      return editor.getModel()?.getLineCount() || 0;
    } else {
      return 0;
    }
  }
  return {
    setAutoScroll,
    getInstance,
    getAutoScroll,
    setContent,
    logRef,
    handleRevealLine,
    getLineCount,
  };
};
