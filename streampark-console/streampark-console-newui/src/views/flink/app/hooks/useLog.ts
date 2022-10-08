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
        overviewRulerBorder: false, // 不要滚动条边框
        tabSize: 2, // tab 缩进长度
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
  /* 注册语言 */
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
