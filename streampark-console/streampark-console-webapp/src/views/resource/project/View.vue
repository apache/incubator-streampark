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
  <PageWrapper contentFullHeight contentBackground contentClass="px-20px">
    <a-card class="header" :bordered="false">
      <template #extra>
        <a-radio-group v-model:value="queryParams.buildState">
          <a-radio-button
            v-for="item in buttonList"
            @click="handleQuery(item.key)"
            :value="item.key"
            :key="item.key"
            >{{ item.label }}</a-radio-button
          >
        </a-radio-group>
        <a-input-search
          v-model:value="searchValue"
          @search="handleSearch"
          :placeholder="t('flink.project.searchPlaceholder')"
          class="search-input"
        />
      </template>
    </a-card>
    <div class="operate pt-20px bg-white" v-auth="'project:create'">
      <a-button id="e2e-project-create-btn" type="dashed" style="width: 100%" @click="onAdd">
        <Icon icon="ant-design:plus-outlined" />
        {{ t('common.add') }}
      </a-button>
    </div>
    <a-card :bordered="false">
      <a-spin :spinning="loading">
        <a-list>
          <ListItem
            :key="item.id"
            v-for="item in projectDataSource"
            :item="item"
            @view-log="handleViewLog"
            @success="handleListItemSuccess"
          />
        </a-list>
        <div class="text-center mt-10px">
          <a-pagination
            class="w-full"
            showLessItems
            hideOnSinglePage
            :pageSize="pageInfo.pageSize"
            :total="pageInfo.total"
            @change="handlePageChange"
          />
        </div>
      </a-spin>
    </a-card>
    <LogModal @register="registerLogModal" />
  </PageWrapper>
</template>
<script lang="ts">
  import { defineComponent, onUnmounted, reactive, ref, unref, watch } from 'vue';

  import { PageWrapper } from '/@/components/Page';
  import { statusList } from './project.data';
  import { RadioGroup, Radio, Input, Card, List, Spin, Pagination } from 'ant-design-vue';
  import { getList } from '/@/api/resource/project';
  import { ProjectRecord } from '/@/api/resource/project/model/projectModel';
  import ListItem from './components/ListItem.vue';
  import Icon from '/@/components/Icon/src/Icon.vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { useTimeoutFn } from '@vueuse/core';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useModal } from '/@/components/Modal';
  import LogModal from './components/LogModal.vue';
  import { useUserStoreWithOut } from '/@/store/modules/user';

  export default defineComponent({
    name: 'ProjectView',
    components: {
      PageWrapper,
      ARadioGroup: RadioGroup,
      ARadioButton: Radio.Button,
      AInputSearch: Input.Search,
      APagination: Pagination,
      ACard: Card,
      AList: List,
      ListItem,
      ASpin: Spin,
      Icon,
      LogModal,
    },
    setup() {
      const go = useGo();
      const userStore = useUserStoreWithOut();
      const { t } = useI18n();
      const [registerLogModal, { openModal: openLogModal }] = useModal();
      const buttonList = reactive(statusList);
      const loading = ref(false);
      const buildState = ref('');
      const searchValue = ref('');
      const pageInfo = reactive({
        currentPage: 1,
        pageSize: 10,
        total: 0,
      });

      const queryParams = reactive<{ buildState: string; name?: string }>({
        buildState: '',
      });

      let projectDataSource = ref<Array<ProjectRecord>>([]);

      function onAdd() {
        go(`/project/add`);
      }

      function handleSearch(value: string) {
        queryParams.name = value;
        pageInfo.currentPage = 1;
        queryParams.name = searchValue.value;
        queryData();
      }

      function queryData(showLoading = true) {
        if (showLoading) loading.value = true;
        getList({
          ...queryParams,
          pageNum: pageInfo.currentPage,
          pageSize: pageInfo.pageSize,
        }).then((res) => {
          loading.value = false;
          pageInfo.total = Number(res.total);
          projectDataSource.value = res.records;
        });
      }

      const handleQuery = function (val: string | undefined) {
        pageInfo.currentPage = 1;
        queryParams.buildState = val!;
        queryParams.name = searchValue.value;
        queryData();
      };

      const { start, stop } = useTimeoutFn(
        () => {
          if (!unref(loading)) queryData(false);
          start();
        },
        2000,
        { immediate: false },
      );
      /* View log */
      function handleViewLog(value: Recordable) {
        openLogModal(true, { project: value });
      }
      // teamId update
      watch(
        () => userStore.getTeamId,
        (val) => {
          if (val) queryData();
        },
      );
      queryData();
      start();

      onUnmounted(() => {
        stop();
      });
      function handlePageChange(val: number) {
        pageInfo.currentPage = val;
        queryParams.name = searchValue.value;
        queryData();
      }
      function handleListItemSuccess() {
        pageInfo.currentPage = 1;
        queryData();
      }
      return {
        t,
        searchValue,
        pageInfo,
        buildState,
        buttonList,
        handleQuery,
        queryParams,
        projectDataSource,
        loading,
        onAdd,
        handleSearch,
        registerLogModal,
        handleViewLog,
        queryData,
        handlePageChange,
        handleListItemSuccess,
      };
    },
  });
</script>
<style lang="less" scoped>
  .search-input {
    width: 272px;
    margin-left: 16px;
  }

  .add-btn {
    margin-left: 30px;
  }
</style>
