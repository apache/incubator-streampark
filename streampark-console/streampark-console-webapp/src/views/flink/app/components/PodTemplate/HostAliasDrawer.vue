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
  import { useI18n } from '/@/hooks/web/useI18n';
  export default defineComponent({
    name: 'HostAliasDrawer',
  });
</script>
<script setup lang="ts" name="HostAliasDrawer">
  import { computed, ref, unref } from 'vue';
  import { Select, Card, Tag } from 'ant-design-vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import {
    fetchCompleteHostAliasToPodTemplate,
    fetchPreviewHostAlias,
    fetchSysHosts,
  } from '/@/api/flink/flinkPodtmpl';

  const SelectOption = Select.Option;
  const emit = defineEmits(['complete', 'register']);

  const { t } = useI18n();
  const selectValue = ref<string[]>([]);
  const sysHostsAlias = ref<string[]>([]);
  const hostAliasPreview = ref('');
  const visualType = ref('');
  const podTemplate = ref('');
  const [registerDrawerInner, { closeDrawer }] = useDrawerInner((data) =>
    onReceiveDrawerData(data),
  );

  async function onReceiveDrawerData(data: {
    selectValue: string[];
    visualType: string;
    podTemplate: string;
  }) {
    selectValue.value = data.selectValue;
    visualType.value = data.visualType;
    podTemplate.value = data.podTemplate;
    const res = await fetchSysHosts({});
    sysHostsAlias.value = res;
  }

  async function handleSelectedTemplateHostAlias() {
    const res = await fetchPreviewHostAlias({ hosts: selectValue.value.join(',') });
    hostAliasPreview.value = res;
  }

  async function handleSubmitHostAliasToPodTemplate() {
    const param = {
      hosts: unref(selectValue).join(','),
      podTemplate: unref(podTemplate),
    };

    const content = await fetchCompleteHostAliasToPodTemplate(param);
    if (content != null && content !== '') {
      emit('complete', {
        visualType: unref(visualType),
        content,
      });
    }
    handleCloseDrawer();
  }
  const titleMap = {
    ptVisual: 'Pod Template HostAlias',
    jmPtVisual: 'JM Pod Template HostAlias',
    tmPtVisual: 'TM Pod Template HostAlias',
  };
  const getTitle = computed(() => {
    if (!unref(visualType)) return '';
    return titleMap[unref(visualType)];
  });
  function handleCloseDrawer() {
    closeDrawer();
    hostAliasPreview.value = '';
    selectValue.value = [];
  }
</script>
<template>
  <BasicDrawer
    @register="registerDrawerInner"
    item-layout="vertical"
    :mask-closable="false"
    :width="500"
    :title="getTitle"
  >
    <div>
      <p class="conf-desc">
        <span class="note-info" style="margin-bottom: 12px">
          <Tag color="#2db7f5" class="tag-note">Note</Tag>
          Enter the host-ip mapping value in the format <b>[hostname:ip]</b>, e.g:
          chd01.streampark.com:192.168.112.233
        </span>
      </p>
    </div>
    <div>
      <Select
        mode="multiple"
        placeholder="Search System Hosts"
        v-model:value="selectValue"
        style="width: 100%"
        :showArrow="true"
        @change="handleSelectedTemplateHostAlias"
      >
        <SelectOption v-for="item in sysHostsAlias" :key="item" :value="item">
          <!-- <template #suffixIcon>
          </template> -->
          <!-- <Icon icon="ant-design:plus-circle-outlined" /> -->
          {{ item }}
        </SelectOption>
      </Select>
    </div>
    <div style="margin-top: 30px">
      <Card title="preview" size="small" hoverable>
        <pre style="font-size: 12px">{{ hostAliasPreview }}</pre>
      </Card>
    </div>
    <div class="pod-template-tool-drawer-submit-cancel">
      <a-button
        class="e2e_flink_app_pod_cancel"
        :style="{ marginRight: '8px' }"
        @click="handleCloseDrawer()"
      >
        {{ t('common.cancelText') }}
      </a-button>
      <a-button
        class="e2e_flink_app_pod_submit"
        type="primary"
        @click="handleSubmitHostAliasToPodTemplate()"
      >
        {{ t('common.submitText') }}
      </a-button>
    </div>
  </BasicDrawer>
</template>
