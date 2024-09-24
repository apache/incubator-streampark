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
<template>
  <div style="height: 340px" :class="fullContentClass">
    <div
      class="full-content-tool flex justify-between px-20px border-solid border-b pb-10px mb-10px"
      v-if="fullScreenStatus"
    >
      <div class="basic-title">
        <Icon icon="material-symbols:energy-program-saving" color="#477de9" />
        Program args
      </div>
      <Tooltip :title="t('component.modal.restore')" placement="bottom">
        <FullscreenExitOutlined role="full" @click="toggle" style="font-size: 18px" />
      </Tooltip>
    </div>
    <div ref="programArgRef" :class="fullEditorClass" class="w-full program-box mt-5px"> </div>
    <ButtonGroup class="sparkSql-tool" v-if="!fullScreenStatus">
      <a-button
        class="sparkSql-tool-item"
        v-if="canReview"
        type="primary"
        @click="emit('preview', value)"
        size="small"
      >
        <Icon icon="ant-design:eye-outlined" />
        {{ t('spark.app.sparkSql.preview') }}
      </a-button>
      <a-button
        class="sparkSql-tool-item"
        size="small"
        :type="canReview ? 'default' : 'primary'"
        @click="toggle"
      >
        <Icon icon="ant-design:fullscreen-outlined" />
        {{ t('layout.header.tooltipEntryFull') }}
      </a-button>
    </ButtonGroup>
    <ButtonGroup v-else class="sparkSql-tool">
      <a-button
        type="primary"
        class="sparkSql-tool-item"
        v-if="canReview"
        @click="emit('preview', value)"
      >
        <Icon icon="ant-design:eye-outlined" />
        {{ t('spark.app.sparkSql.preview') }}
      </a-button>
      <a-button
        class="sparkSql-tool-item"
        size="small"
        :type="canReview ? 'default' : 'primary'"
        @click="toggle"
      >
        <Icon icon="ant-design:fullscreen-exit-outlined" />
        {{ t('layout.header.tooltipExitFull') }}
      </a-button>
    </ButtonGroup>
  </div>
</template>
<script lang="ts" setup>
  import { Button, Tooltip } from 'ant-design-vue';
  import { FullscreenExitOutlined } from '@ant-design/icons-vue';
  import { computed, ref, toRefs, watchEffect } from 'vue';
  import { getMonacoOptions } from '../data';
  import Icon from '/@/components/Icon';
  import { useFullContent } from '/@/hooks/event/useFullscreen';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMonaco } from '/@/hooks/web/useMonaco';
  const { t } = useI18n();
  const ButtonGroup = Button.Group;

  const props = defineProps({
    value: {
      type: String,
      required: true,
    },
    suggestions: {
      type: Array as PropType<Array<{ text: string; description: string; value: string }>>,
      default: () => [],
    },
  });
  const { value, suggestions } = toRefs(props);
  const emit = defineEmits(['update:value', 'preview']);
  const programArgRef = ref();

  const { toggle, fullContentClass, fullEditorClass, fullScreenStatus } = useFullContent();
  const { onChange, setContent, setMonacoSuggest } = useMonaco(programArgRef, {
    language: 'plaintext',
    code: '',
    options: {
      ...(getMonacoOptions(false) as any),
      autoClosingBrackets: 'never',
    },
  });
  watchEffect(() => {
    if (suggestions.value.length > 0) {
      setMonacoSuggest(suggestions.value);
    }
  });
  const canReview = computed(() => {
    return /\${.+}/.test(value.value);
  });

  onChange((data) => {
    emit('update:value', data);
  });
  defineExpose({ setContent });
</script>
<style lang="less">
  .program-box {
    border: 1px solid @border-color-base;
  }
</style>
