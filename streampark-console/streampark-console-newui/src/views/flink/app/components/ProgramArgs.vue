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
  <div ref="programArgRef" class="w-full border mt-5px" style="height: 120px"> </div>
  <div class="relative flinksql-tool">
    <a-button
      class="flinksql-tool-item"
      v-if="canReview"
      @click="emit('preview', value)"
      size="small"
    >
      <Icon icon="ant-design:eye-outlined" />
      preview
    </a-button>
  </div>
</template>
<script lang="ts">
  export default {
    name: 'ProgramArg',
  };
</script>
<script lang="ts" setup>
  import { computed, ref, toRefs, watchEffect } from 'vue';
  import { getMonacoOptions } from '../data';
  import Icon from '/@/components/Icon';
  import { useMonaco } from '/@/hooks/web/useMonaco';
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
