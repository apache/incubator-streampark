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
  import { defineComponent } from 'vue';
  export default defineComponent({
    name: 'TemplateButtonGroup',
  });
</script>
<script setup lang="ts" name="TemplateButtonGroup">
  import { Icon } from '/@/components/Icon';
  import { Button } from 'ant-design-vue';
  import { useI18n } from '/@/hooks/web/useI18n';

  const ButtonGroup = Button.Group;
  const { t } = useI18n();
  const emit = defineEmits(['clickHistory', 'clickInit', 'clickHostAlias']);
  defineProps({
    visualType: {
      type: String,
      required: true,
    },
  });
</script>
<template>
  <ButtonGroup class="pod-template-tool">
    <a-button
      type="primary"
      size="small"
      class="pod-template-tool-item"
      @click="emit('clickHistory', visualType)"
    >
      <div class="flex items-center">
        <Icon icon="ant-design:history-outlined" class="pr-5px" />
        {{ t('common.history') }}
      </div>
    </a-button>
    <a-button
      type="default"
      size="small"
      class="pod-template-tool-item"
      @click="emit('clickInit', visualType)"
    >
      <div class="flex items-center">
        <Icon icon="ant-design:copy-outlined" class="pr-5px" />
        {{ t('flink.app.pod.init') }}
      </div>
    </a-button>
    <a-button
      type="default"
      size="small"
      class="pod-template-tool-item"
      @click="emit('clickHostAlias', visualType)"
    >
      <div class="flex items-center">
        <Icon icon="ant-design:share-alt-outlined" class="pr-5px" />

        {{ t('flink.app.pod.host') }}
      </div>
    </a-button>
    <a-button type="default" disabled size="small" class="pod-template-tool-item">
      <div class="flex items-center">
        <Icon icon="ant-design:hdd-outlined" class="pr-5px" />
        PVC
      </div>
    </a-button>
  </ButtonGroup>
</template>
