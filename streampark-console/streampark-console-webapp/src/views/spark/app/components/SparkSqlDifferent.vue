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
<script setup lang="ts">
  import { nextTick, reactive, ref } from 'vue';
  import { getMonacoOptions } from '/@/views/flink/app/data/index';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { useDiffMonaco } from '/@/views/flink/app/hooks/useDiffMonaco';
  import { Tabs } from 'ant-design-vue';
  import { SvgIcon } from '/@/components/Icon';
  import { useI18n } from '/@/hooks/web/useI18n';

  const TabPane = Tabs.TabPane;

  const { t } = useI18n();

  const items = ref<any[]>([]);
  const renderEleMap = new Map<string, boolean>();
  const version = reactive({
    original: null,
    modified: null,
  });
  const original = ref<Nullable<string>>(null);
  const modified = ref<Nullable<string>>(null);
  const [registerMergelyDrawer, { closeDrawer }] = useDrawerInner((data) => {
    if (data && data.immediate) {
      different(data);
    }
  });
  const { setupEditor, disposeEditor } = useDiffMonaco(
    null,
    'sql',
    () => original.value,
    () => modified.value,
    getMonacoOptions(true) as any,
    false,
  );

  function different(data: { param: Array<any>; original: string; modified: string }) {
    Object.assign(version, { original: data.original, modified: data.modified });
    items.value = data.param;
    handleRenderTab(0);
  }

  function handleRenderTab(index: number) {
    const x = items.value[index];
    original.value = x.original;
    modified.value = x.modified;
    const ele = '#mergely' + index;
    nextTick(() => {
      const elem = document.querySelector(ele);
      handleHeight(elem, 140);
      // no render
      if (!renderEleMap.get(ele)) {
        setupEditor(elem);
        renderEleMap.set(ele, true);
      }
    });
  }
  /* Change editor height */
  function handleHeight(ele: any, h: number) {
    const height = document.documentElement.offsetHeight || document.body.offsetHeight;
    ele.style.height = height - h + 'px';
  }

  async function handleCancel() {
    await disposeEditor();
    renderEleMap.clear();
    closeDrawer();
  }

  defineExpose({ different });
</script>
<template>
  <BasicDrawer
    @register="registerMergelyDrawer"
    width="80%"
    class="drawer-conf"
    keyboard
    :destroyOnClose="true"
    @close="handleCancel"
  >
    <template #title>
      <SvgIcon name="swap" />
      <span class="px-10px">{{ t('spark.app.detail.compareSparkSql') }}</span>
      <span> ( </span>
      <span class="pr-5px">{{ t('spark.app.detail.different.original') }}</span>
      <a-button type="primary" shape="circle" size="small">
        {{ version.original }}
      </a-button>
      <span class="px-10px">VS</span>
      <span class="pr-5px"> {{ t('spark.app.detail.different.target') }} </span>
      <a-button type="primary" shape="circle" size="small">
        {{ version.modified }}
      </a-button>
      <span> ) </span>
    </template>
    <Tabs type="card" @change="handleRenderTab">
      <TabPane v-for="(item, index) in items" :key="index" :tab="item.name">
        <div :id="'mergely'.concat(index.toString())"></div>
      </TabPane>
    </Tabs>
  </BasicDrawer>
</template>
<style scoped>
  .drawer-conf :deep(.ant-drawer-body) {
    padding: 5px !important;
    padding-bottom: 0px !important;
  }

  .drawer-bottom-button {
    position: absolute;
    padding-top: 10px;
    padding-right: 50px;
    width: 100%;
    bottom: 10px;
    z-index: 9;
  }

  .drwaer-button-item {
    margin-right: 20px;
  }
</style>
