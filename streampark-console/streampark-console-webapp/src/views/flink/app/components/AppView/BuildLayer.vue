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
    name: 'BuildLayer',
  });
</script>
<script setup lang="ts" name="BuildLayer">
  import { Space, Tag, Progress } from 'ant-design-vue';
  import { Icon } from '/@/components/Icon';
  defineProps({
    layer: {
      type: Object as PropType<Recordable>,
      required: true,
    },
  });
</script>
<template>
  <div class="mb-5px">
    <Space size="small">
      <Icon icon="ant-design:arrow-right-outlined" />
      <Tag color="blue"> {{ layer.layerId }}</Tag>
      <Tag>{{ layer.status }}</Tag>
      <template v-if="layer.totalMb != null && layer.totalMb !== 0">
        <span style="font-size: 12px; text-align: right">
          {{ layer.currentMb }} / {{ layer.totalMb }} MB
        </span>
      </template>
    </Space>
  </div>
  <template v-if="layer.totalMb != null && layer.totalMb !== 0">
    <div class="ml-20px mr-50px mb-15px">
      <Progress :percent="layer.percent" status="active" />
    </div>
  </template>
</template>
