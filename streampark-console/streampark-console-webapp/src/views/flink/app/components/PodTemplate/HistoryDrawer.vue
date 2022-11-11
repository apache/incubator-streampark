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
    name: 'HistoryDrawer',
  });
</script>
<script setup lang="ts" name="HistoryDrawer">
  import { Empty, Card } from 'ant-design-vue';
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';

  const dataSource = ref<string[]>([]);
  const visualType = ref<string>('ptVisual');

  const [registerDrawerInner] = useDrawerInner((data) => onReceiveDrawerData(data));
  function onReceiveDrawerData(data: { dataSource: string[]; visualType: string }) {
    if (!data) return;
    if (data.dataSource) {
      dataSource.value = data.dataSource;
    }
    visualType.value = data.visualType;
  }
  const drawerTitleMap = {
    ptVisual: 'Pod Template History',
    jmPtVisual: 'JobManager Pod Template History',
    tmPtVisual: 'TaskManager Pod Template History',
  };

  const cardTitleMap = {
    ptVisual: 'pod-template.yaml',
    jmPtVisual: 'jm-pod-template.yaml',
    tmPtVisual: 'tm-pod-template.yaml',
  };
</script>
<template>
  <BasicDrawer
    @register="registerDrawerInner"
    :title="drawerTitleMap[visualType]"
    item-layout="vertical"
    :width="700"
  >
    <Empty v-if="dataSource.length == 0" />
    <Card
      :title="cardTitleMap[visualType]"
      size="small"
      hoverable
      class="mb-8px"
      v-for="(item, index) in dataSource"
      :key="index"
    >
      <template #extra>
        <!-- <a @click="handleChoicePodTemplate(item)">Choice</a> -->
        <slot name="extra" :data="item"></slot>
      </template>
      <pre style="font-size: 12px">{{ item }}</pre>
    </Card>
  </BasicDrawer>
</template>
