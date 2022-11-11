<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<script lang="ts">
  import { ref, unref, defineComponent } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  export default defineComponent({
    name: 'ExecOptionModal',
  });
</script>
<script setup lang="ts" name="ExecOptionModal">
  import { SvgIcon } from '/@/components/Icon';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMonaco, isDark } from '/@/hooks/web/useMonaco';
  const startExp = ref<HTMLDivElement>();
  const [registerModal] = useModalInner((data) => {
    data && onReceiveModalData(data);
  });
  const { t } = useI18n();
  const { setContent } = useMonaco(
    startExp,
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
  async function onReceiveModalData(data) {
    setContent(data.content);
  }

  /* registered language */
  async function handleLogMonaco(monaco: any) {
    monaco.languages.register({ id: 'log' });
    monaco.languages.setMonarchTokensProvider('log', {
      tokenizer: {
        root: [
          [/.*\.Exception.*/, 'log-error'],
          [/.*Caused\s+by:.*/, 'log-error'],
          [/\s+at\s+.*/, 'log-info'],
          [/http:\/\/(.*):\d+(.*)\/application_\d+_\d+/, 'yarn-info'],
          [/Container\s+id:\s+container_\d+_\d+_\d+_\d+/, 'yarn-info'],
          [/yarn\s+logs\s+-applicationId\s+application_\d+_\d+/, 'yarn-info'],
          [/\[20\d+-\d+-\d+\s+\d+:\d+:\d+\d+|.\d+]/, 'log-date'],
          [/\[[a-zA-Z 0-9:]+]/, 'log-date'],
        ],
      },
    });

    monaco.editor.defineTheme('log', {
      base: unref(isDark) ? 'vs-dark' : 'vs',
      inherit: true,
      colors: {},
      rules: [
        { token: 'log-info', foreground: '808080' },
        { token: 'log-error', foreground: 'ff0000', fontStyle: 'bold' },
        { token: 'log-notice', foreground: 'FFA500' },
        { token: 'yarn-info', foreground: '0066FF', fontStyle: 'bold' },
        { token: 'log-date', foreground: '008800' },
      ],
    });
  }
</script>
<template>
  <BasicModal
    @register="registerModal"
    :body-style="{ height: '600px', padding: '5px' }"
    :destroy-on-close="true"
    :showOkBtn="false"
    width="80%"
    :height="600"
  >
    <template #title>
      <SvgIcon name="code" style="color: red" />
      {{ t('flink.app.detail.exceptionModal.title') }}
    </template>
    <div class="startExp h-540px" ref="startExp"></div>
  </BasicModal>
</template>
