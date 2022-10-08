<template>
  <div :class="prefixCls">
    <Popover
      title=""
      trigger="click"
      :overlayClassName="`${prefixCls}__overlay`"
      v-model:visible="noticyVisible"
    >
      <Badge :count="count" :numberStyle="numberStyle" :offset="[-5, 10]">
        <BellOutlined />
      </Badge>
      <template #content>
        <Tabs v-model:activeKey="noticyType">
          <template v-for="item in listData" :key="item.key">
            <TabPane>
              <template #tab>
                {{ item.name }}
                <span v-if="item.list.length !== 0">({{ item.list.length }})</span>
              </template>
              <Spin :spinning="noticyLoading">
                <NoticeList
                  :noticyType="item.key"
                  :list="item.list"
                  @noticy-reload="getNoticyList"
                  @noticy-click="handleNoticyInfo"
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
  import { fetchNotify, fetchNotifyDelete } from '/@/api/sys/notify';
  import { NoticyItem } from '/@/api/sys/model/notifyModel';
  import { useWebSocket } from '@vueuse/core';
  import { useUserStoreWithOut } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { isObject } from '/@/utils/is';

  export interface TabItem {
    key: number;
    name: string;
    list: NoticyItem[];
  }

  export default defineComponent({
    name: 'Noticy',
    components: { Popover, BellOutlined, Tabs, TabPane: Tabs.TabPane, Badge, NoticeList, Spin },
    setup() {
      const { prefixCls } = useDesign('header-notify');
      const userStore = useUserStoreWithOut();
      const { createMessage, createConfirm } = useMessage();
      const noticyType = ref(1);
      const currentPage = ref(1);
      const noticyVisible = ref(false);
      const listData = ref<TabItem[]>([
        { key: 1, name: '异常告警', list: [] },
        { key: 2, name: '通知消息', list: [] },
      ]);
      const noticyLoading = ref(false);
      const count = computed(() => {
        let count = 0;
        for (let i = 0; i < unref(listData).length; i++) {
          count += unref(listData)[i].list.filter((i) => i.readed === 0).length;
        }
        return count;
      });
      /* 查看通知消息 */
      async function handleNoticyInfo(record: NoticyItem) {
        noticyVisible.value = false;
        createConfirm({
          iconType: unref(noticyType) == 1 ? 'error' : 'info',
          title: record.title,
          content: h('div', {}, record.context),
          okText: '删除',
          okType: 'danger',
          onOk: () => handleDelete(record.id),
        });
      }

      /* 删除 */
      async function handleDelete(id: string) {
        try {
          noticyLoading.value = true;
          await fetchNotifyDelete(id);
          getNoticyList(unref(noticyType));
        } catch (error) {
          console.error(error);
        } finally {
          noticyLoading.value = false;
        }
      }
      /* 获取通知列表 */
      async function getNoticyList(type: number) {
        try {
          noticyLoading.value = true;
          const res = await fetchNotify({ type, pageNum: 1, pageSize: 40 });
          handleNoticyMessage(type, res.records);
        } catch (error) {
          console.error(error);
        } finally {
          noticyLoading.value = false;
        }
      }
      /* 处理通知消息数据 */
      function handleNoticyMessage(type: number, data: NoticyItem[]) {
        /* 异常告警 */
        if (type === 1) {
          listData.value[0].list = [...data];
        } else {
          listData.value[1].list = [...data];
        }
      }
      const wbSocketUrl = `${window.location.origin}${
        import.meta.env.VITE_APP_BASE_API
      }/websocket/${userStore.getUserInfo.userId}`;

      const { data } = useWebSocket(wbSocketUrl.replace(/http/, 'ws'), {
        autoReconnect: {
          retries: 3,
          delay: 1000,
          onFailed() {
            createMessage.warning('消息服务器连接失败!');
          },
        },
      });
      watch([data, currentPage], ([newData]: [NoticyItem], [newPage]) => {
        if (newData && isObject(newData)) {
          /* 异常告警 */
          if (unref(noticyType) === 1) {
            listData.value[0].list.push(newData);
          } else {
            listData.value[1].list.push(newData);
          }
          handleNoticyInfo(newData);
        }
        if (newPage) {
          getNoticyList(unref(noticyType));
        }
      });
      getNoticyList(1);
      getNoticyList(2);
      return {
        prefixCls,
        listData,
        noticyType,
        count,
        handleNoticyInfo,
        noticyVisible,
        currentPage,
        numberStyle: {},
        noticyLoading,
        getNoticyList,
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
