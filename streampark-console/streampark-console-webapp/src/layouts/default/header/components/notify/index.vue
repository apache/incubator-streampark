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
  import { computed, defineComponent, ref, unref, h } from 'vue';
  import { Popover, Tabs, Badge, Spin } from 'ant-design-vue';
  import { BellOutlined } from '@ant-design/icons-vue';
  import NoticeList from './NoticeList.vue';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { fetchNotify, fetchNotifyDelete } from '/@/api/system/notify';
  import { NotifyItem } from '/@/api/system/model/notifyModel';
  import { useMessage } from '/@/hooks/web/useMessage';
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
      const { createConfirm } = useMessage();
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
          await getNotifyList(unref(notifyType));
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

      setInterval(() => getNotifyList(1), 5000);

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
