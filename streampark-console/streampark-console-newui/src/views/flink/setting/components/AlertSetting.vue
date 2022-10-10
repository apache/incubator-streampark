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
    name: 'AlertSetting',
  });
</script>
<script setup lang="ts" name="AlertSetting">
  import { h, onMounted, ref } from 'vue';
  import { List, Popconfirm, Tooltip } from 'ant-design-vue';
  import {
    ThunderboltOutlined,
    EditOutlined,
    DeleteOutlined,
    PlusOutlined,
  } from '@ant-design/icons-vue';
  import { useModal } from '/@/components/Modal';
  import { SvgIcon } from '/@/components/Icon';
  import AlertModal from './AlertModal.vue';
  import { fetchAlertSetting, fetchSendAlert, fetchAlertDelete } from '/@/api/flink/setting/alert';
  import { AlertSetting } from '/@/api/flink/setting/types/alert.type';
  import { useMessage } from '/@/hooks/web/useMessage';

  const ListItem = List.Item;
  const ListItemMeta = ListItem.Meta;

  const { t } = useI18n();
  const { createMessage, createConfirm } = useMessage();
  const [registerAlertModal, { openModal: openAlertModal }] = useModal();
  const alerts = ref<AlertSetting[]>([]);
  const alertType = ref<number[]>([]);
  /* Get alert configuration */
  async function getAlertSetting() {
    const res = await fetchAlertSetting();
    alerts.value = res;
  }
  /* compute type */
  function computeAlertType(level: number) {
    if (level === null) {
      level = 0;
    }
    const result: number[] = [];
    while (level != 0) {
      // Get the lowest 1
      const code = level & -level;
      result.push(code);
      // Set the lowest position to 0
      level ^= code;
    }
    return result;
  }
  /* test connection */
  async function handleTestAlarm(item) {
    const hide = createMessage.loading(' Testing', 0);
    try {
      await fetchSendAlert({ id: item.id });
      createMessage.success('Test Alert Config  successful!');
      getAlertSetting();
    } catch (error: any) {
      /* custom alert message */
      if (error?.response?.data?.message) {
        createConfirm({
          iconType: 'error',
          title: 'Operation Failed',
          content: h('div', { class: 'whitespace-pre-wrap' }, error?.response?.data?.message),
        });
      } else {
        console.error(error);
      }
    } finally {
      hide();
    }
  }
  /* Click the edit button */
  function handleEditAlertConf(item: AlertSetting) {
    alertType.value = computeAlertType(item.alertType);

    let emailParams: Recordable<any> = {};
    let dingTalkParams: Recordable<any> = {};
    let weComParams: Recordable<any> = {};
    let larkParams: Recordable<any> = {};
    if (alertType.value.indexOf(1) > -1) {
      emailParams = JSON.parse(item.emailParams);
    }
    if (alertType.value.indexOf(2) > -1) {
      dingTalkParams = JSON.parse(item.dingTalkParams);
      // dingtalkIsAtAll = dingTalkParams.isAtAll;
      // dingtalkSecretEnable = dingTalkParams.secretEnable;
    }
    if (alertType.value.indexOf(4) > -1) {
      weComParams = JSON.parse(item.weComParams) as Recordable;
    }
    if (alertType.value.indexOf(16) > -1) {
      larkParams = JSON.parse(item.larkParams) as Recordable;
      // larkIsAtAll = larkParams.isAtAll;
      // larkSecretEnable = larkParams.secretEnable;
    }

    console.log('Alarm parameters：' + JSON.stringify(item));
    openAlertModal(true, {
      alertId: item.id,
      alertName: item.alertName,
      alertType: alertType.value,
      alertEmail: emailParams.contacts,
      alertDingURL: dingTalkParams.alertDingURL,
      dingtalkToken: dingTalkParams.token,
      dingtalkSecretToken: dingTalkParams.secretToken,
      alertDingUser: dingTalkParams.contacts,
      dingtalkIsAtAll: dingTalkParams.isAtAll,
      dingtalkSecretEnable: dingTalkParams.secretEnable,
      weToken: weComParams.token,
      larkToken: larkParams.token,
      larkIsAtAll: larkParams.isAtAll,
      larkSecretEnable: larkParams.secretEnable,
      larkSecretToken: larkParams.secretToken,
    });
  }

  /* delete configuration */
  async function handleDeleteAlertConf(item: AlertSetting) {
    try {
      await fetchAlertDelete({ id: item.id });
      createMessage.success('Delete Alert Config  successful!');
      getAlertSetting();
    } catch (error) {
      console.error(error);
    }
  }
  onMounted(() => {
    getAlertSetting();
  });
</script>

<template>
  <div v-auth="'project:create'">
    <a-button type="dashed" style="width: 100%; margin-top: 20px" @click="openAlertModal(true)">
      <PlusOutlined />
      {{ t('common.add') }}
    </a-button>
  </div>
  <List>
    <ListItem v-for="(item, index) in alerts" :key="index">
      <ListItemMeta style="width: 40%">
        <template #title>{{ item.alertName }}</template>
        <template #avatar>
          <div class="avatar">
            <SvgIcon name="flink" />
          </div>
        </template>
      </ListItemMeta>
      <div class="list-content" style="width: 40%">
        <div text-align center>Alert Type</div>
        <SvgIcon name="mail" size="25" v-if="computeAlertType(item.alertType).indexOf(1) > -1" />
        <SvgIcon
          name="dingtalk"
          size="25"
          v-if="computeAlertType(item.alertType).indexOf(2) > -1"
        />
        <SvgIcon name="wecom" size="25" v-if="computeAlertType(item.alertType).indexOf(4) > -1" />
        <SvgIcon name="message" size="25" v-if="computeAlertType(item.alertType).indexOf(8) > -1" />
        <SvgIcon name="lark" size="25" v-if="computeAlertType(item.alertType).indexOf(16) > -1" />
      </div>
      <template #actions>
        <Tooltip title="Alert Test">
          <a-button
            @click="handleTestAlarm(item)"
            shape="circle"
            size="large"
            style="margin-left: 3px"
            class="control-button ctl-btn-color"
          >
            <ThunderboltOutlined />
          </a-button>
        </Tooltip>
        <Tooltip title="Edit Alert Config">
          <a-button
            @click="handleEditAlertConf(item)"
            shape="circle"
            size="large"
            style="margin-left: 3px"
            class="control-button ctl-btn-color"
          >
            <EditOutlined />
          </a-button>
        </Tooltip>
        <Popconfirm
          :title="t('flink.setting.alert.delete')"
          :cancel-text="t('common.no')"
          :ok-text="t('common.yes')"
          @confirm="handleDeleteAlertConf(item)"
        >
          <a-button
            type="danger"
            shape="circle"
            size="large"
            style="margin-left: 3px"
            class="control-button"
          >
            <DeleteOutlined />
          </a-button>
        </Popconfirm>
      </template>
    </ListItem>
  </List>
  <AlertModal @register="registerAlertModal" @reload="getAlertSetting" width="850px" />
</template>
