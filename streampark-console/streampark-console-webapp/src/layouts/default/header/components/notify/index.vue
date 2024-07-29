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
  <div :class="prefixCls">
    <Popover
      title=""
      trigger="click"
      :overlayClassName="`${prefixCls}__overlay`"
      v-model:visible="notifyVisible"
    >
      <Badge :count="count" :numberStyle="numberStyle" :offset="[-5, 10]">
        <BellOutlined />
      </Badge>
      <template #content>
        <Tabs v-model:activeKey="notifyType">
          <template v-for="item in listData" :key="item.key">
            <TabPane>
              <template #tab>
                {{ item.name }}
                <span v-if="item.list.length !== 0">({{ item.list.length }})</span>
              </template>
              <Spin :spinning="notifyLoading">
                <NoticeList
                  :notifyType="item.key"
                  :list="item.list"
                  @notify-reload="getNotifyList"
                  @notify-click="handleNotifyInfo"
                />
              </Spin>
            </TabPane>
          </template>
        </Tabs>
      </template>
    </Popover>
  </div>
</template>
<script lang="ts">
  import { computed, defineComponent, ref, unref, watch, h } from 'vue';

  import { Popover, Tabs, Badge, Spin } from 'ant-design-vue';
  import { BellOutlined } from '@ant-design/icons-vue';
  import NoticeList from './NoticeList.vue';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { fetchNotify, fetchNotifyDelete } from '/@/api/system/notify';
  import { NotifyItem } from '/@/api/system/model/notifyModel';
  import { useWebSocket } from '@vueuse/core';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { isObject } from '/@/utils/is';
  import { useI18n } from '/@/hooks/web/useI18n';
  const { t } = useI18n();
  export interface TabItem {
    key: number;
    name: string;
    list: NotifyItem[];
  }

  export default defineComponent({
    name: 'Notify',
    components: { Popover, BellOutlined, Tabs, TabPane: Tabs.TabPane, Badge, NoticeList, Spin },
    setup() {
      const { prefixCls } = useDesign('header-notify');
      const userStore = useUserStoreWithOut();
      const { createMessage, createConfirm } = useMessage();
      const notifyType = ref(1);
      const currentPage = ref(1);
      const notifyVisible = ref(false);
      const listData = ref<TabItem[]>([
        { key: 1, name: t('routes.basic.notice.exception'), list: [] },
        { key: 2, name: t('routes.basic.notice.message'), list: [] },
      ]);
      const notifyLoading = ref(false);
      const count = computed(() => {
        let count = 0;
        for (let i = 0; i < unref(listData).length; i++) {
          count += unref(listData)[i].list.filter((i) => i.readed === 0).length;
        }
        return count;
      });
      /* View notification messages */
      async function handleNotifyInfo(record: NotifyItem) {
        notifyVisible.value = false;
        createConfirm({
          iconType: unref(notifyType) == 1 ? 'error' : 'info',
          title: record.title,
          content: h('div', {}, record.context),
          okText: t('common.delText'),
          okType: 'danger',
          onOk: () => handleDelete(record.id),
        });
      }

      /* delete */
      async function handleDelete(id: string) {
        try {
          notifyLoading.value = true;
          await fetchNotifyDelete(id);
          getNotifyList(unref(notifyType));
        } catch (error) {
          console.error(error);
        } finally {
          notifyLoading.value = false;
        }
      }
      /* Get a list of notifications */
      async function getNotifyList(type: number) {
        try {
          notifyLoading.value = true;
          const res = await fetchNotify({ type, pageNum: 1, pageSize: 40 });
          handleNotifyMessage(type, res.records);
        } catch (error) {
          console.error(error);
        } finally {
          notifyLoading.value = false;
        }
      }
      /* Process notification message data */
      function handleNotifyMessage(type: number, data: NotifyItem[]) {
        /* The abnormal alarm */
        if (type === 1) {
          listData.value[0].list = [...data];
        } else {
          listData.value[1].list = [...data];
        }
      }

      const wbSocketUrl = `${window.location.origin}${
        import.meta.env.VITE_GLOB_API_URL + (import.meta.env.VITE_GLOB_API_URL_PREFIX || '')
      }/websocket/${userStore.getUserInfo.userId}`;

      const { data } = useWebSocket(wbSocketUrl.replace(/http/, 'ws'), {
        autoReconnect: {
          retries: 3,
          delay: 1000,
          onFailed() {
            createMessage.warning('Message server connection failed!');
          },
        },
      });
      watch([data, currentPage], ([newData]: [NotifyItem], [newPage]) => {
        if (newData && isObject(newData)) {
          /* The abnormal alarm */
          if (unref(notifyType) === 1) {
            listData.value[0].list.push(newData);
          } else {
            listData.value[1].list.push(newData);
          }
          handleNotifyInfo(newData);
        }
        if (newPage) {
          getNotifyList(unref(notifyType));
        }
      });
      getNotifyList(1);
      getNotifyList(2);
      return {
        prefixCls,
        listData,
        notifyType,
        count,
        handleNotifyInfo,
        notifyVisible,
        currentPage,
        numberStyle: {},
        notifyLoading,
        getNotifyList,
        handleDelete,
      };
    },
  });
</script>
<style lang="less">
  @prefix-cls: ~'@{namespace}-header-notify';

  .@{prefix-cls} {
    padding-top: 2px;

    &__overlay {
      max-width: 360px;
    }

    .ant-tabs-content {
      width: 300px;
    }

    .ant-badge {
      font-size: 18px;

      .ant-badge-multiple-words {
        padding: 0 4px;
      }

      svg {
        width: 0.9em;
      }
    }
  }
</style>
