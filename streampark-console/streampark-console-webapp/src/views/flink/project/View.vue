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
          @search="handleSearch"
          :placeholder="t('flink.project.searchPlaceholder')"
          class="search-input"
        />
      </template>
    </a-card>
    <div class="operate pt-20px bg-white" v-auth="'project:create'">
      <a-button type="dashed" style="width: 100%" @click="onAdd">
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
            @success="queryData"
          />
        </a-list>
      </a-spin>
    </a-card>
    <LogModal @register="registerLogModal" />
  </PageWrapper>
</template>
<script lang="ts">
  import { defineComponent, onUnmounted, reactive, ref, unref, watch } from 'vue';

  import { PageWrapper } from '/@/components/Page';
  import { statusList } from './project.data';
  import { RadioGroup, Radio, Input, Card, List, Spin } from 'ant-design-vue';
  import { getList } from '/@/api/flink/project';
  import { ProjectRecord } from '/@/api/flink/project/model/projectModel';
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

      const queryParams = reactive({
        buildState: '',
      });

      let projectDataSource = ref<Array<ProjectRecord>>([]);

      function onAdd() {
        go(`/flink/project/add`);
      }

      function handleSearch(value: string) {
        Object.assign(queryParams, { name: value });
        queryData();
      }

      function queryData(showLoading = true) {
        if (showLoading) loading.value = true;
        getList({ ...queryParams, teamId: userStore.getTeamId }).then((res) => {
          loading.value = false;
          projectDataSource.value = res.records;
        });
      }

      const handleQuery = function (val) {
        queryParams.buildState = val;
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
      // teamid update
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

      return {
        t,
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
