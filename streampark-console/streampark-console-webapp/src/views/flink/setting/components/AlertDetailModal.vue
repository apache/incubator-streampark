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
  export default defineComponent({
    name: 'AlertDetailModal',
  });
</script>
<script setup lang="ts" name="AlertDetailModal">
  import { defineComponent, reactive, ref, h } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { SvgIcon } from '/@/components/Icon';
  import { DescItem, Description } from '/@/components/Description';
  import { Divider, Tag, Typography } from 'ant-design-vue';
  import { useI18n } from '/@/hooks/web/useI18n';

  const { t } = useI18n();
  const typographyParagraph = Typography.Paragraph;
  interface DingTalkType {
    token: string;
    contacts: string;
    isAtAll: boolean;
    alertDingURL: string;
    secretEnable: boolean;
    secretToken: string;
  }
  interface LarkType {
    token: string;
    isAtAll: boolean;
    secretEnable: boolean;
    secretToken: string;
  }

  const emailInfo = ref<{ contacts: string }>({ contacts: '' });
  const weChat = ref<{ contacts: string }>({ contacts: '' });
  const alertTypeTags = ref<string[]>([]);
  const dingTalk = reactive<Partial<DingTalkType>>({});
  const lark = reactive<Partial<LarkType>>({});

  const [registerModal] = useModalInner((data) => {
    emailInfo.value = { contacts: '' };
    weChat.value = { contacts: '' };
    alertTypeTags.value = [];
    if (data) {
      alertTypeTags.value = data.alertTypeTags;
      emailInfo.value = JSON.parse(data.emailParams || '{}');
      weChat.value = JSON.parse(data.weComParams || '{}');
      if (data.dingTalkParams) {
        Object.assign(dingTalk, JSON.parse(data.dingTalkParams || '{}'));
      }
      if (data.larkParams) {
        Object.assign(lark, JSON.parse(data.larkParams || '{}'));
        console.log('lark', lark);
      }
    }
  });
  const dingTalkColumn: DescItem[] = [
    {
      label: t('flink.setting.alert.dingTalkUrl'),
      field: 'alertDingURL',
      span: 2,
      labelMinWidth: 50,
    },
    {
      label: t('flink.setting.alert.dingtalkAccessToken'),
      field: 'token',
      span: 2,
      render: renderTypl,
    },
    {
      label: t('flink.setting.alert.secretToken'),
      field: 'secretToken',
      span: 2,
      render: renderTypl,
    },
    { label: t('flink.setting.alert.dingTalkUser'), field: 'contacts' },
    { label: t('flink.setting.alert.dingtalkIsAtAll'), field: 'isAtAll', render: renderTag },
  ];
  const larkColumn: DescItem[] = [
    { label: t('flink.setting.alert.larkToken'), field: 'token', span: 2, render: renderTypl },
    {
      label: t('flink.setting.alert.larkSecretToken'),
      field: 'secretToken',
      span: 2,
      render: renderTypl,
    },
    { label: t('flink.setting.alert.larkIsAtAll'), field: 'isAtAll', render: renderTag },
  ];
  function renderTag(value: boolean) {
    return h(Tag, { color: value ? 'green' : 'red', class: '!leading-20px' }, () => String(value));
  }

  function renderTypl(value: string) {
    return h(typographyParagraph, { copyable: true, class: '!mb-0' }, () => value);
  }
</script>

<template>
  <BasicModal :show-ok-btn="false" @register="registerModal" class="alert-detail">
    <template #title>
      <SvgIcon name="alarm" size="25" />
      {{ t('flink.setting.alert.alertDetail') }}
    </template>
    <template v-if="alertTypeTags.includes('1')">
      <Divider>
        <SvgIcon name="mail" size="20" />
        {{ t('flink.setting.alert.email') }}
      </Divider>
      <Description
        class="alert-detail"
        :column="1"
        :data="emailInfo"
        :schema="[{ label: t('flink.setting.alert.alertEmail'), field: 'contacts' }]"
      />
    </template>
    <template v-if="alertTypeTags.includes('2')">
      <Divider>
        <SvgIcon name="dingtalk" size="20" />
        {{ t('flink.setting.alert.dingTalk') }}
      </Divider>
      <Description class="alert-detail" :column="2" :data="dingTalk" :schema="dingTalkColumn" />
    </template>
    <template v-if="alertTypeTags.includes('4')">
      <Divider><SvgIcon name="wecom" size="20" /> {{ t('flink.setting.alert.weChat') }} </Divider>
      <Description
        class="alert-detail"
        :column="1"
        :data="weChat"
        :schema="[
          { label: t('flink.setting.alert.weChattoken'), field: 'token', render: renderTypl },
        ]"
      />
    </template>
    <template v-if="alertTypeTags.includes('16')">
      <Divider>
        <SvgIcon name="dingtalk" size="20" />
        {{ t('flink.setting.alert.lark') }}
      </Divider>
      <Description :column="2" :data="lark" :schema="larkColumn" class="alert-detail" />
    </template>
  </BasicModal>
</template>

<style>
  .alert-detail .ant-descriptions-item-label {
    width: 150px;
  }
</style>
