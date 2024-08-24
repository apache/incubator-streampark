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
  import { useDiffMonaco } from '/@/views/flink/app/hooks/useDiffMonaco';
  import { useMonaco } from '/@/hooks/web/useMonaco';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { nextTick, Ref, ref, unref } from 'vue';
  import { getMonacoOptions } from '../data';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { SvgIcon, Icon } from '/@/components/Icon';
  const emit = defineEmits(['ok', 'close', 'register']);
  const props = defineProps({
    readOnly: {
      type: Boolean as PropType<boolean>,
      default: false,
    },
  });

  const { t } = useI18n();
  const title = ref('edit configuration');
  const compareMode = ref(false);
  const visibleDiff = ref(false);
  const changed = ref(false);
  const targetValue = ref<Nullable<string>>(null);
  const originalValue = ref<Nullable<string>>(null);

  const monacoMergely = ref();
  const monacoConfig = ref();
  const loading = ref(false);

  /* Click Next */
  function handleNext() {
    visibleDiff.value = true;
    title.value = 'Compare configuration';
    nextTick(() => {
      handleHeight(monacoMergely, 100);
    });
    // handleDifferent(unref(originalValue), unref(targetValue));
  }
  useDiffMonaco(
    monacoMergely,
    'yaml',
    () => unref(originalValue),
    () => unref(targetValue),
    getMonacoOptions(props.readOnly) as any,
  );

  const { setContent, onChange } = useMonaco(monacoConfig, {
    language: 'yaml',
    options: getMonacoOptions(props.readOnly) as any,
  });
  onChange((value) => {
    // the first time
    if (targetValue.value) {
      changed.value = true;
    }
    targetValue.value = value;
  });

  /* Change editor height */
  function handleHeight(ele: Ref, h: number) {
    const height = document.documentElement.offsetHeight || document.body.offsetHeight;
    unref(ele).style.height = height - h + 'px';
  }

  /* close match */
  function handleCloseDiff() {
    title.value = 'Edit configuration';
    visibleDiff.value = false;
  }

  /* Click OK */
  function handleOk() {
    const value = unref(targetValue);
    if (value == null || !value.replace(/^\s+|\s+$/gm, '')) {
      emit('ok', { isSetConfig: false, configOverride: null });
    } else {
      emit('ok', { isSetConfig: true, configOverride: value });
    }

    handleCancel();
  }

  /* Click to cancel */
  function handleCancel() {
    changed.value = false;
    targetValue.value = null;
    originalValue.value = null;
    visibleDiff.value = false;
    loading.value = false;
    emit('close');
    closeDrawer();
  }
  const [registerMergelyDrawer, { closeDrawer }] = useDrawerInner(
    (data: { configOverride: string }) => {
      data && onReceiveDrawerData(data);
    },
  );
  /* data reception */
  function onReceiveDrawerData(data) {
    compareMode.value = false;
    changed.value = false;
    targetValue.value = null;
    originalValue.value = data.configOverride;
    if (props.readOnly) {
      title.value = 'Configuration detail';
    }
    nextTick(() => {
      handleHeight(unref(monacoConfig), 130);
      visibleDiff.value = false;
      setContent(unref(originalValue) || '');
    });
  }
</script>

<template>
  <BasicDrawer
    @register="registerMergelyDrawer"
    :keyboard="false"
    :closable="false"
    :mask-closable="false"
    width="80%"
    class="drawer-conf"
  >
    <template #title>
      <SvgIcon v-if="props.readOnly" name="see" />
      <SvgIcon v-else name="edit" />
      {{ title }}
    </template>
    <div v-show="!visibleDiff">
      <div ref="monacoConfig"></div>
      <div class="drawer-bottom-button">
        <div style="float: right">
          <a-button type="primary" class="drwaer-button-item" @click="handleCancel">
            {{ t('common.cancelText') }}
          </a-button>
          <a-button v-if="changed" type="primary" class="drwaer-button-item" @click="handleNext()">
            <Icon icon="clarity:note-edit-line" />

            {{ t('common.next') }}
          </a-button>
        </div>
      </div>
    </div>

    <div v-if="visibleDiff">
      <div ref="monacoMergely"></div>
      <div class="drawer-bottom-button" style="position: absolute">
        <div style="float: right">
          <a-button
            v-if="changed"
            type="primary"
            class="drwaer-button-item"
            @click="handleCloseDiff"
          >
            <Icon icon="ant-design:left-outlined" />

            {{ t('common.previous') }}
          </a-button>
          <a-button v-if="!compareMode" class="drwaer-button-item" type="primary" @click="handleOk">
            <Icon icon="ant-design:cloud-outlined" />

            {{ t('common.apply') }}
          </a-button>
        </div>
      </div>
    </div>
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
