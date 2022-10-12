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
import { ref, unref } from 'vue';
import { useMonaco, isDark } from '/@/hooks/web/useMonaco';

export const useLog = () => {
  const logRef = ref();
  const { setContent } = useMonaco(
    logRef,
    {
      language: 'log',
      options: {
        theme: 'log',
        readOnly: true,
        scrollBeyondLastLine: false,
        overviewRulerBorder: false, // Don't scroll bar borders
        tabSize: 2, // tab indent length
        minimap: { enabled: true },
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
  /* registered language */
  async function handleLogMonaco(monaco: any) {
    monaco.languages.register({ id: 'log' });
    monaco.languages.setMonarchTokensProvider('log', {
      tokenizer: {
        root: [
          [/\[20\d+-\d+-\d+\s+\d+:\d+:\d+\d+|.\d+]/, 'log-date'],
          [/\[[a-zA-Z 0-9:]+]/, 'log-date'],
        ],
      },
    });

    monaco.editor.defineTheme('log', {
      base: unref(isDark) ? 'vs-dark' : 'vs',
      inherit: true,
      colors: {},
      rules: [{ token: 'log-date', foreground: '008800', fontStyle: 'bold' }],
    });
  }
  return { setContent, logRef };
};
