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
  import { Descriptions, Tag } from 'ant-design-vue';
  import { computed, toRefs } from 'vue';
  import { SvgIcon } from '/@/components/Icon';
  import { BasicTitle } from '/@/components/Basic';
  import { alertTypes } from './alert.data';
  import { useI18n } from '/@/hooks/web/useI18n';

  const { t } = useI18n();
  const props = defineProps({
    alertType: {
      type: String,
      validator: (v: string) => ['1', '2', '4', '8', '16'].includes(v),
      required: true,
    },
    alertSource: {
      type: Object as PropType<Recordable>,
      required: true,
    },
  });
  const { alertType, alertSource } = toRefs(props);
  const DescriptionsItem = Descriptions.Item;

  const emailInfo = computed(() => {
    return JSON.parse(alertSource.value.emailParams || '{}');
  });
  const dingTalk = computed(() => {
    return JSON.parse(alertSource.value.dingTalkParams || '{}');
  });
  const weChat = computed(() => {
    return JSON.parse(alertSource.value.weComParams || '{}');
  });
  const lark = computed(() => {
    return JSON.parse(alertSource.value.larkParams || '{}');
  });
  function desensitization(dataString: string) {
    return String(dataString).replace(/^(.{4})(?:.+)(.{4})$/, '\$1********\$2');
  }
</script>

<template>
  <BasicTitle class="mt-10px border-dot">
    <div class="flex items-center">
      <SvgIcon :name="alertTypes[alertType].icon" :size="20" class="!align-middle" />
      <span class="pl-10px">
        {{ alertTypes[alertType].name }}
      </span>
    </div>
  </BasicTitle>
  <Descriptions size="small" :column="1" class="pl-15px mt-10px">
    <template v-if="alertType === '1'">
      <DescriptionsItem :label="t('flink.setting.alert.alertEmail')">
        <span class="text-blue-500">{{ emailInfo.contacts || '' }}</span>
      </DescriptionsItem>
    </template>
    <template v-else-if="alertType === '2'">
      <DescriptionsItem :label="t('flink.setting.alert.dingTalkUser')">
        {{ dingTalk.contacts || '' }}
      </DescriptionsItem>
      <DescriptionsItem :label="t('flink.setting.alert.larkIsAtAll')">
        <Tag :color="dingTalk.isAtAll ? 'green' : 'red'" class="!leading-20px">
          {{ dingTalk.isAtAll }}
        </Tag>
      </DescriptionsItem>
    </template>
    <template v-else-if="alertType === '4'">
      <DescriptionsItem :label="t('flink.setting.alert.weChattoken')">
        {{ desensitization(weChat.token || '') }}
      </DescriptionsItem>
    </template>
    <template v-else-if="alertType === '16'">
      <DescriptionsItem :label="t('flink.setting.alert.larkIsAtAll')">
        <Tag :color="lark.isAtAll ? 'green' : 'red'" class="!leading-20px">
          {{ lark.isAtAll }}
        </Tag>
      </DescriptionsItem>
    </template>
  </Descriptions>
</template>
<style lang="less">
  .border-dot:before {
    content: '';
    width: 0;
    height: 20px;
    margin-top: 2px;
    border: 2px solid #24c6dc;
    border-radius: 2px;
    transform: translateX(-10px);
  }
</style>
