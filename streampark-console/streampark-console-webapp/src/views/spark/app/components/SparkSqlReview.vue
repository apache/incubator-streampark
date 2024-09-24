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
  <BasicDrawer
    @register="registerDrawer"
    :width="800"
    placement="right"
    :showOkBtn="false"
    showFooter
  >
    <template #title>
      <EyeOutlined style="color: green" />
      Spark SQL preview
    </template>
    <div ref="sparkReviewRef" class="h-[calc(100vh-150px)] spark-preview"></div>
  </BasicDrawer>
</template>

<script lang="ts" setup>
  import { EyeOutlined } from '@ant-design/icons-vue';
  import { ref } from 'vue';
  import { getMonacoOptions } from '../data';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { useMonaco } from '/@/hooks/web/useMonaco';
  const sparkReviewRef = ref();
  const { setContent } = useMonaco(sparkReviewRef, {
    language: 'sql',
    code: '',
    options: getMonacoOptions(true) as any,
  });
  const [registerDrawer] = useDrawerInner((data) => {
    if (data) {
      setContent(data.sql || '');
    }
  });
</script>
<style lang="less">
  .spark-preview {
    border: 1px solid @border-color-base;
  }
</style>
