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
<script lang="ts" setup>
  import { ref, reactive } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { SyncOutlined } from '@ant-design/icons-vue';
  import { useMonaco } from '/@/hooks/web/useMonaco';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { SparkEnv } from '/@/api/spark/home.type';
  import { fetchSparkEnv, fetchSparkSync } from '/@/api/spark/home';

  const { t } = useI18n();
  const sparkInfo = reactive({} as SparkEnv);
  const conf = ref();
  const syncLoading = ref(false);
  const { Swal } = useMessage();
  const [registerDrawer, { changeLoading }] = useDrawerInner(async (data: SparkEnv) => {
    try {
      changeLoading(true);
      const res = await fetchSparkEnv(data.id);
      Object.assign(sparkInfo, res);
      setContent(res.sparkConf || '');
      const height = document.documentElement.offsetHeight || document.body.offsetHeight;
      conf.value.style.height = height - 210 + 'px';
    } catch (error) {
      console.error(error);
    } finally {
      changeLoading(false);
    }
  });

  const { setContent } = useMonaco(conf, {
    language: 'yaml',
    options: {
      selectOnLineNumbers: false,
      folding: true,
      foldingStrategy: 'indentation', // code fragmentation
      overviewRulerBorder: false, // Don't scroll bar borders
      tabSize: 2, // tab indent length
      readOnly: true,
      scrollBeyondLastLine: false,
      lineNumbersMinChars: 5,
      lineHeight: 24,
      automaticLayout: true,
      cursorStyle: 'line',
      cursorWidth: 3,
      renderFinalNewline: 'on',
      renderLineHighlight: 'all',
      quickSuggestionsDelay: 100, // Code prompt delay
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
  });
  /* Sync configuration */
  async function handleSync() {
    try {
      syncLoading.value = true;
      await fetchSparkSync(sparkInfo.id);
      const res = await fetchSparkEnv(sparkInfo.id);
      Object.assign(sparkInfo, res);
      setContent(res.sparkConf);
      Swal.fire({
        icon: 'success',
        title: res.sparkName.concat(' conf sync successful!'),
        showConfirmButton: false,
        timer: 2000,
      });
    } catch (error) {
      console.error(error);
    } finally {
      syncLoading.value = false;
    }
  }
</script>
<template>
  <BasicDrawer title="Spark Conf" @register="registerDrawer" width="70%" placement="right">
    <div class="py-15px pl-10px">
      {{ t('spark.home.title') }}: &nbsp;&nbsp; {{ sparkInfo.sparkHome }}
    </div>
    <div>
      <div class="pl-10px">{{ t('spark.home.sync') }} :</div>
      <div class="py-15px">
        <div ref="conf" style="height: 120px"></div>
        <a-button
          type="primary"
          class="float-right mt-10px mr-10px"
          @click="handleSync"
          :loading="syncLoading"
        >
          <SyncOutlined />
          {{ t('spark.home.sync') }}
        </a-button>
      </div>
    </div>
  </BasicDrawer>
</template>
