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
  export default {
    name: 'BuildLogModal',
  };
</script>
<script setup lang="ts" name="BuildLogModal">
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useLog } from '/@/hooks/web/useLog';
  import { buildLog } from '/@/api/resource/project';
  import { reactive, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon';
  import { formatToDateTime } from '/@/utils/dateUtil';
  import { useTimeoutFn } from '@vueuse/shared';
  import {ResultEnum} from "/@/enums/httpEnum";

  const { t } = useI18n();

  let startOffset: Nullable<number> = null;
  const logTime = ref<string>('');
  const showRefresh = ref<boolean>(false);
  const project = reactive<Recordable>({});

  const [registerModal, { changeLoading, closeModal }] = useModalInner((data) => {
    data && onReceiveModalData(data);
  });
  const { setContent, logRef, handleRevealLine } = useLog();

  async function onReceiveModalData(data: Recordable) {
    showRefresh.value = true;
    Object.assign(project, unref(data.project));
    changeLoading(true);
    await refreshLog();
    start();
  }
  const { start, stop } = useTimeoutFn(
    () => {
      refreshLog();
    },
    2000,
    { immediate: false },
  );

  async function refreshLog() {
    try {
      const { data } = await buildLog({
        id: project.id,
        startOffset,
      });
      if (data.code == ResultEnum.SUCCESS) {
        if (data.readFinished === false) {
          showRefresh.value = true;
          start();
        } else {
          showRefresh.value = false;
        }
        logTime.value = formatToDateTime(new Date());
        await setContent(data.data.fileContent);
        handleRevealLine();
      }
    } catch (error) {
      closeModal();
      console.error('logModal error', error);
    } finally {
      changeLoading(false);
    }
  }
  async function handleClose() {
    stop();
  }
</script>
<template>
  <BasicModal
    canFullscreen
    :scrollTop="false"
    @register="registerModal"
    width="80%"
    :after-close="handleClose"
  >
    <template #title>
      <Icon icon="ant-design:code-outlined" style="color: #477de9" />&nbsp;
      <span>{{ project.projectName }} Build Log </span>
    </template>
    <div ref="logRef" class="h-full min-h-500px"></div>
    <template #footer>
      <div class="flex align-items-center">
        <div class="flex-1 text-left">{{ t('flink.app.view.refreshTime') }}:{{ logTime }}</div>
        <div class="button-group">
          <a-button key="stop" type="primary" @click="closeModal()">
            {{ t('common.closeText') }}
          </a-button>
        </div>
      </div>
    </template>
  </BasicModal>
</template>
<style scoped></style>
